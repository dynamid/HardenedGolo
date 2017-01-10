
/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Created by qifan on 09/12/16.
 */

package org.eclipse.golo.compiler.ir;

import java.util.*;
//import com.microsoft.z3.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;


/**
 * <p>This Visitor aims at symbolic testing of the golo program.
 * @author Qifan ZHOU
 */
public class IrSymbolicTestVisitor implements GoloIrVisitor {

  private String sourcePath;
  private String destFile;
  private int[] charsPerLine;
  private ArrayList<String> whyMLcode = new ArrayList<>();
  private ArrayList<functionNameArity> functionsDefined = new ArrayList<>();
  private ArrayList<functionNameArity> functionsCalled = new ArrayList<>();

  private HashMap<String,Integer>  SymbolicStatement = new HashMap<>();



  /// Indentation management
  private int spacing = 0;
  private final static int INDENTATION_WIDTH = 4;


  public IrSymbolicTestVisitor(String sourcePath, int[] charsPerLine, String destFile) {
    this.sourcePath = sourcePath;
    this.charsPerLine = Arrays.copyOf(charsPerLine, charsPerLine.length);
    this.destFile = destFile;
    System.out.println("--------- Symbolic Testing Visitor Created --------- " );
  }



  //================= <Sub class functionNameArity> =================
  private static class functionNameArity {
    private String name;
    private int arity;
    private PositionInSourceCode positionInSourceCode;

    private functionNameArity(String name, int arity) {
      this.name = name;
      this.arity = arity;
    }


    private String getName() {
      return name;
    }

    private int getArity() {
      return arity;
    }

    private int getLine() {
      return positionInSourceCode.getLine();
    }

    private int getColumn() {
      return positionInSourceCode.getColumn();
    }

    /**
     * Check if function has same name, and greater or same arity than param
     */
    private boolean isSimilar(functionNameArity fndefined) {
      return fndefined != null &&
        fndefined.getName().equals(this.name) &&
        fndefined.getArity() <= this.arity;
    }

    public String toString() {
      return name + arity;
    }
  }
  //================= </Sub class functionNameArity> =================




  //================= <Visitor methods> =================
  @Override
  public void visitModule(GoloModule module) {
    System.out.println(">>>GoloModule: " + module.toString());
    //module.walk(this);



    solverExample();
   // evalExample1();
   // Context ctx = new Context();
   /* System.out.println("EvalExample1");
    Log.append("EvalExample1");

    IntExpr x = ctx.mkIntConst("x");
    IntExpr y = ctx.mkIntConst("y");
    IntExpr two = ctx.mkInt(2);*/
  }

  @Override
  public void visitFunction(GoloFunction function) {

    System.out.println(">>>Function: " + function.toString());
    generateInput(function);
    function.walk(this);

  }


  @Override
  public void visitBlock(Block block) {
    //System.out.println(block.toString());

    //ReferenceTable referenceTable = block.getReferenceTable();
    // context.referenceTableStack.push(referenceTable);
    // if (block.isEmpty()) { return; }

    block.walk(this);

    // context.referenceTableStack.pop();
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
   // System.out.println(ref.toString());
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    System.out.println(">>>ReturnStatement: " + returnStatement.toString());
    returnStatement.walk(this);
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {

   // System.out.println(">>>FunctionInvocation : " + functionInvocation.toString());

  }


  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    System.out.println(">>>ConditionalBranching: " + conditionalBranching.toString());

   // conditionalBranching.getCondition().accept(this);

    //conditionalBranching.getTrueBlock().accept(this);

  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    System.out.println(">>>AssignmentStatement: " + assignmentStatement.toString());
    System.out.println("***** value of this AssignmentStatement is : " + ValueOfReference(assignmentStatement));
    System.out.println("00000000000000-"+SymbolicStatement);
    SymbolicStatement.put(assignmentStatement.getLocalReference().getName(),ValueOfReference(assignmentStatement));
    System.out.println("11111111111111-"+SymbolicStatement);

    //assignmentStatement.walk(this);
  }


  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {

  }


  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    //System.out.println(">>>ConstantStatement: " + constantStatement.toString());
  }


  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {

   // System.out.println(binaryOperation.toString());
  }


  //================= <Missing visitor methods -- Unsupported parts of the language> =================

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    System.out.println("Unary operator unsupported");
    unaryOperation.getExpressionStatement().accept(this);
  }


  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    System.out.println("Case unsupported");
    for (WhenClause<Block> c : caseStatement.getClauses()) {
      c.condition().accept(this);
      c.action().accept(this);
    }
    caseStatement.getOtherwise().accept(this);
  }

  @Override
  public void visitMatchExpression(MatchExpression matchExpression) {
    System.out.println("Match unsupported");
    for (WhenClause<?> c : matchExpression.getClauses()) {
      c.accept(this);
    }
    matchExpression.getOtherwise().accept(this);
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    System.out.println("When unsupported");
    whenClause.walk(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    System.out.println("Loop unsupported");

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
    System.out.println("Foreach unsupported");

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
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
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
  }

  @Override
  public void visitUnion(Union union) {
  }

  @Override
  public void visitUnionValue(UnionValue value) {
  }

  @Override
  public void visitDecorator(Decorator decorator) {
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment assignment) {
  }




/* ================= <Utilil Methods> ================= */


  public void generateInput(GoloFunction function) {
    int N_input = function.getArity();
    ArrayList<Integer> inputs = new ArrayList<Integer>();
    for(int i =0; i< N_input; i++){
      inputs.add(generatRandomPositiveNegitiveValue(-65536,65536));
    }
    System.out.println("Generated inputs for >>>"+ function.getName()+"<<< :"+inputs.toString());
  }

  public static int generatRandomPositiveNegitiveValue(int min, int max) {
    int res = min + (int) (Math.random() * ((max - (min)) + 1));
    return res;
  }

  public int ValueOfReference(AssignmentStatement assignmentStatement) {
    int val = Integer.parseInt(assignmentStatement.getExpressionStatement().toString());
    return val;
  }


// ================= <Utilil Methods> =================




/* ================= <Constraint Solver Methods> =================*/
  public void solverExample(){

      Model model = new Model("Choco Solver Hello World");

      IntVar a = model.intVar("a",IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
      IntVar b = model.intVar("b",IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
      IntVar zero = model.intVar("zero",0);

      model.arithm(a, "+", b, "<", 8).post();
      model.arithm(a, "-", b, "=", 5).post();
      model.arithm(a, "-", zero, ">", 0).post();

      model.getSolver().solve();
      System.out.println("Solution found : " + a + ", " + b +','+ (a.getValue()+b.getValue()));
      model.getSolver().solve();
      System.out.println("Solution found : " + a + ", " + b);

      /*int i = 1;
      while (model.getSolver().solve()) {
        System.out.println("Solution " + i++ + " found : " + a + ", " + b);
      }*/
  }



// ================= <Constraint Solver Methods> =================
}
