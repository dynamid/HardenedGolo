/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.golo.compiler.ir;

import org.eclipse.golo.compiler.jgoloparser.JGTerm;
import org.eclipse.golo.compiler.jgoloparser.JGVariableContainer;
import org.eclipse.golo.runtime.OperatorType;

import java.util.*;

/**
 * This Visitor aims at verifying the correctness of the types using in golo program.
 * @author Plokhoi Nikolai.
 */
public class IrTreeVisitAndCheckTypes implements GoloIrVisitor {

  private Context context;

  private ReferenceTable referenceTable;

  private static class functionNameArity {

    private String name;

    private int arity;

    private PositionInSourceCode positionInSourceCode;

    private functionNameArity(String name, int arity){
      this.name = name;
      this.arity = arity;
    }

    private String getName() {
      return name;
    }

    private int getArity() {
      return arity;
    }

    /**
     * @param another Compared function.
     * @return Has same name, and greater or same arity.
     */
    private boolean isSimilar(functionNameArity another){
      return another != null &&
             another.getName().equals(this.name) &&
             another.getArity() <= this.arity;
    }

    public String toString (){
      return name+arity;
    }
  }

  private static final class Context {

    private final Deque<ReferenceTable> referenceTableStack = new LinkedList<>();
  }

  // Following methods used to deals with the final ";".
  // The complexity is linked to the consideration of FunctionInvocation that can be as well an expression as a Statement.
  private boolean inBlock = false;

  private boolean maskIfInBlock() {
    return maskIfInBlock(false);
  }

  private boolean maskIfInBlock(boolean newIb) {
    boolean ib = inBlock;
    inBlock = newIb;
    return ib;
  }

  private void restoreIfInBlock(boolean ib) {
    restoreIfInBlock(ib,ib);
  }

  private void restoreIfInBlock(boolean ib, boolean force) {
    inBlock = ib;
  }

  @Override
  public void visitModule(GoloModule module) {
    this.context = new Context();
    module.walk(this);
  }

  @Override
  public void visitFunction(GoloFunction function) {
    referenceTable = function.getBlock().getReferenceTable();
    function.walk(this);
  }

  @Override
  public void visitBlock(Block block) {
    boolean upperLevelExist = maskIfInBlock(true);
    context.referenceTableStack.push(block.getReferenceTable());
    if (block.isEmpty()) {
      return;
    }
    block.walk(this);
    restoreIfInBlock(upperLevelExist);
    context.referenceTableStack.pop();
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    returnStatement.walk(this);
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    boolean ib = maskIfInBlock();
    functionInvocation.walk(this);
    restoreIfInBlock(ib);
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    boolean ib = maskIfInBlock();
    conditionalBranching.getCondition().accept(this);
    conditionalBranching.getTrueBlock().accept(this);

    if (conditionalBranching.hasFalseBlock()) {
      conditionalBranching.getFalseBlock().accept(this);
    } else if (conditionalBranching.hasElseConditionalBranching()) {
      conditionalBranching.getElseConditionalBranching().accept(this);
    }
    restoreIfInBlock(ib);
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    boolean ib = maskIfInBlock();
    assignmentStatement.walk(this);
    if (assignmentStatement.getLocalReference().isConstant()) {
      restoreIfInBlock(ib, false);
    } else {
      restoreIfInBlock(ib);
    }
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    referenceLookup.resolveIn(context.referenceTableStack.peek());
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
  }

  @Override
  public void visitBinaryOperation(BinaryOperation operation) {
    ExpressionStatement left = operation.getLeftExpression();
    ExpressionStatement right = operation.getRightExpression();
    switch (operation.getType()) {
      case MINUS:
      case MORE:
      case MOREOREQUALS:
      case LESS:
      case LESSOREQUALS:
      case PLUS:
      case DIVIDE:
        if (ReferenceLookup.class.isInstance(left)) {
          checkTypeReference(ReferenceLookup.class.cast(left), JGVariableContainer.Type.NUMERIC);
        }
        if (ReferenceLookup.class.isInstance(right)) {
          checkTypeReference(ReferenceLookup.class.cast(right), JGVariableContainer.Type.NUMERIC);
        }
        break;

      case AND:
      case OR:
        if (ReferenceLookup.class.isInstance(left)) {
          checkTypeReference(ReferenceLookup.class.cast(left), JGVariableContainer.Type.BOOLEAN);
        }
        if (ReferenceLookup.class.isInstance(right)) {
          checkTypeReference(ReferenceLookup.class.cast(right), JGVariableContainer.Type.BOOLEAN);
        }
    }
    operation.walk(this);
  }

  private void checkTypeReference(ReferenceLookup referenceLookup, JGVariableContainer.Type type) {
    String name = referenceLookup.getName();
    LocalReference reference = referenceTable.get(name);
    if (reference != null) {
      if (reference.getType() != type) {
        System.err.println("Variable '" + name + "' defined in specification as non-" + type.toString().toLowerCase() + " value but used as it!");
      }
    } else {
      System.err.println("Undeclared reference: " + name);
    }
  }

  @Override
  public void visitUnaryOperation(UnaryOperation operation) {
    ExpressionStatement statement = operation.getExpressionStatement();
    if (operation.getType() == OperatorType.NOT && ReferenceLookup.class.isInstance(statement)) {
      checkTypeReference(ReferenceLookup.class.cast(statement), JGVariableContainer.Type.BOOLEAN);
    } else if (operation.getType() == OperatorType.MINUS && ReferenceLookup.class.isInstance(statement)) {
      checkTypeReference(ReferenceLookup.class.cast(statement), JGVariableContainer.Type.NUMERIC);
    }
    operation.getExpressionStatement().accept(this);
  }

  //================= <Missing visitor methods -- Unsupported parts of the language> =================

  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    for (WhenClause<Block> c : caseStatement.getClauses()) {
      c.condition().accept(this);
      c.action().accept(this);
    }
    caseStatement.getOtherwise().accept(this);
  }

  @Override
  public void visitMatchExpression(MatchExpression matchExpression) {
    for (WhenClause<?> c : matchExpression.getClauses()) {
      c.accept(this);
    }
    matchExpression.getOtherwise().accept(this);
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    whenClause.walk(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    if (loopStatement.hasInitStatement()) {
      loopStatement.getInitStatement().accept(this);
    }
    loopStatement.getConditionStatement().accept(this);
    loopStatement.getBlock().accept(this);
    if (loopStatement.hasPostStatement()) {
      loopStatement.getPostStatement().accept(this);
    }
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement foreachStatement) {
    for (LocalReference ref : foreachStatement.getReferences()) {
      ref.accept(this);
    }
    foreachStatement.getIterable().accept(this);
    if (foreachStatement.hasWhenClause()) {
      foreachStatement.getWhenClause().accept(this);
    }
    foreachStatement.getBlock().accept(this);
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    methodInvocation.walk(this);
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    throwStatement.getExpressionStatement().accept(this);
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    tryCatchFinally.getTryBlock().accept(this);
    if (tryCatchFinally.hasCatchBlock()) {
      tryCatchFinally.getCatchBlock().accept(this);
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      tryCatchFinally.getFinallyBlock().accept(this);
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    GoloFunction target = closureReference.getTarget();
    if (target.isAnonymous()) {
      target.walk(this);
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    for (ExpressionStatement statement : collectionLiteral.getExpressions()) {
      statement.accept(this);
    }
  }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collectionComprehension) {
    collectionComprehension.getExpression().accept(this);
    for (Block b : collectionComprehension.getLoopBlocks()) {
      b.accept(this);
    }
  }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) {
    namedArgument.getExpression().accept(this);
  }

  @Override
  public void visitModuleImport(ModuleImport moduleImport) {
    moduleImport.walk(this);
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation namedAugmentation) {
    namedAugmentation.walk(this);
  }

  @Override
  public void visitAugmentation(Augmentation augmentation) {
    augmentation.walk(this);
  }

  @Override
  public void visitStruct(Struct struct) {
    struct.walk(this);
  }

  @Override
  public void visitUnion(Union union) {
    union.walk(this);
  }

  @Override
  public void visitUnionValue(UnionValue value) {
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    decorator.getExpressionStatement().accept(this);
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment assignment) {
    assignment.getExpression().accept(this);
  }
}
