
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
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;


/**
 * <p>This Visitor aims at symbolic testing of the golo program.
 * @author Qifan ZHOU
 */
public class IrSymbolicTestVisitor implements GoloIrVisitor {

  private HashMap<String,VariableStatement> listOfVaribles;
  private LinkedList<ConstraintStatement> PathConstratint;
  private ArrayList<Integer> inputs;
  private int N_inputs;
  private int N_inputs_counter;


  public IrSymbolicTestVisitor() {

    System.out.println("--------- Symbolic Testing Visitor Created --------- " );

    listOfVaribles = new HashMap<String,VariableStatement>();
    PathConstratint= new LinkedList<ConstraintStatement>();
  }



  /** ================= <Visitor methods> ================= */
  @Override
  public void visitModule(GoloModule module) {
    System.out.println(">>>GoloModule: " + module.toString());
    module.walk(this);

    //solverExample();

  }

  @Override
  public void visitFunction(GoloFunction function) {
    System.out.println(">>>Function: " + function.toString());
    N_inputs_counter = 0;
    this.initInputs(function);
    function.walk(this);

  }

  @Override
  public void visitBlock(Block block) {
    block.walk(this);
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
    if(N_inputs_counter < N_inputs ){
      VariableStatement var = new VariableStatement(ref.getName(),ref.getName(),inputs.get(N_inputs_counter));
      listOfVaribles.put(var.getName(),var);
      N_inputs_counter++;
    }
  }



  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    functionInvocation.walk(this);
   // System.out.println(">>>FunctionInvocation : " + functionInvocation.toString());

  }


  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    //System.out.println(">>>ConditionalBranching: " + conditionalBranching.toString());
    System.out.println(">>>ConditionalBranching: " + conditionalBranching.getCondition().toString());
    ConstraintStatement constraint = parse_ConditionExprToConstraint(conditionalBranching.getCondition());
    PathConstratint.add(constraint);
    constraintSolve(constraint);
    //System.out.println(">>>to Constraint: " + constraint);

   // conditionalBranching.getCondition().accept(this);
    //conditionalBranching.getTrueBlock().accept(this);

  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
   /* System.out.println(">>>AssignmentStatement: " + assignmentStatement.toString());
    System.out.println("***** value of this AssignmentStatement is : " + ValueOfReference(assignmentStatement));
    System.out.println("00000000000000-"+SymbolicStatement);
    SymbolicStatement.put(assignmentStatement.getLocalReference().getName(),ValueOfReference(assignmentStatement));
    System.out.println("11111111111111-"+SymbolicStatement);*/

    //assignmentStatement.walk(this);
  }


  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {

  }


  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    System.out.println(">>>ConstantStatement: " + constantStatement.toString());
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    // System.out.println(">>>ReturnStatement: " + returnStatement.toString());
    returnStatement.walk(this);
    //System.out.println(listOfVaribles);
   // System.out.println(PathConstratint);
  }

  /** <Visitor methods> End */








/** ================= <Utilil Methods> ================= */

  private void initInputs(GoloFunction function) {
    this.N_inputs = function.getArity();
    this.inputs = new ArrayList<Integer>();
    for(int i =0; i< N_inputs; i++){
      inputs.add(generatRandomPositiveNegitiveValue(IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND));
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

  public static boolean isInteger(String s) {
    try {
      Integer.parseInt(s);
    } catch(NumberFormatException e) {
      return false;
    } catch(NullPointerException e) {
      return false;
    }
    // only got here if we didn't return false
    return true;
  }


  //this method transforms a "[var1] op1 [var2] op2 [var3]" form condition expression to a ConstraintStatement
  public ConstraintStatement parse_ConditionExprToConstraint(ExpressionStatement condition){
    String[] tokens = condition.toString().split("[ ]+");
    if(tokens[3].equals("==")){tokens[3]="=";}
    ConstraintStatement constraint = new ConstraintStatement(generateVariableFromExpr(tokens[0]),generateVariableFromExpr(tokens[2]),generateVariableFromExpr(tokens[4]),tokens[1],tokens[3]);
    return constraint;
  }

  public VariableStatement generateVariableFromExpr(String expr){
    if (isInteger(expr)){
      return new VariableStatement(Integer.parseInt(expr));
    }
    String name = expr.substring(expr.indexOf("=")+1, expr.indexOf("}"));
    return listOfVaribles.get(name);
  }

  public void constraintSolve(ConstraintStatement constraint){
    Model model = new Model("Choco Solver Hello World");

    IntVar var1 = model.intVar(constraint.getVar1().getName(),IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
    IntVar var2 = model.intVar(constraint.getVar2().getName(),IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
    IntVar var3;
    if(constraint.getVar3().isConstant) {
      var3 = model.intVar(constraint.getVar3().getName(),constraint.getVar3().getValue(),constraint.getVar3().getValue());
    }else{
      var3 = model.intVar(constraint.getVar3().getName(),IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);
    }

    model.arithm(var1, constraint.getOp1(), var2, constraint.getOp2(), var3).post();
    model.getSolver().solve();
    System.out.println("Solution found : " + var1 + ", " + var2 +','+ var3);

  }


  /** <Utilil Methods> End */




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





/** ================= <Sub class VariableStatement> ================= */
  private class VariableStatement {
    private String name;
    private String symbolicExpr;
    private int concretExpr;

    private boolean isConstant;

    private VariableStatement(String aName, String aSymb, int aValue) {
      name = aName;
      symbolicExpr = aSymb;
      concretExpr = aValue;
      isConstant=false;
    }

    private VariableStatement(int aValue) {
      name = Integer.toString(aValue);
      symbolicExpr = Integer.toString(aValue);
      concretExpr = aValue;
      isConstant=true;
    }

    public void setName(String aName) {
      name = aName;
    }
    public void setSymb(String aSymb) {
      symbolicExpr = aSymb;
    }
    public void setValue(int aValue) {
      concretExpr = aValue;
    }

    public String getName() {
      return name;
    }
    public String getSymb() {
      return symbolicExpr;
    }
    public int getValue() {
      return concretExpr;
    }

    public boolean isConstant() {
      return isConstant;
    }

    @Override
    public String toString (){
      return "<"+name+"="+symbolicExpr+"'="+concretExpr+">";
    }
  }
/** <Sub class VariableStatement> End */


/** ================= <Sub class ConstraintStatement> =================
 * "form: [var1] op1 [var2] op2 [var3]
 * example: x + y = z (var1 =x var2 =y var3 =z, op1="+", op2="=" )*/
  private class ConstraintStatement {
    private VariableStatement var1;
    private String op1;
    private VariableStatement var2;
    private String op2;
    private VariableStatement var3;

    private ConstraintStatement(VariableStatement aVar1,VariableStatement aVar2, VariableStatement aVar3, String anOp1, String anOp2) {
      var1=aVar1;
      var2=aVar2;
      var3=aVar3;
      op1=anOp1;
      op2=anOp2;
    }
    public void setVar1(VariableStatement aVar1) {
      var1 = aVar1;
    }
    public void setVar2(VariableStatement aVar2) {
      var1 = aVar2;
    }
    public void setVar3(VariableStatement aVar3) {
      var1 = aVar3;
    }
    public void setOp1(String anOp1) {
      op1 = anOp1;
    }
    public void setOp2(String anOp2) {
      op2 = anOp2;
    }
    public VariableStatement getVar1() {
      return var1;
    }
    public VariableStatement getVar2() {
      return var2;
    }
    public VariableStatement getVar3() {
      return var3;
    }
    public String getOp1() {
      return op1;
    }
    public String getOp2() {
      return op2;
    }

    @Override
    public String toString (){
      return "<"+var1.getName()+op1+var2.getName()+op2+var3.getName()+">";
    }
}
/** <Sub class ConstraintStatement> End */











//================= <Missing visitor methods -- Unsupported parts of the language> =================

@Override
public void visitBinaryOperation(BinaryOperation binaryOperation) {

  // System.out.println(binaryOperation.toString());
}

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
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement foreachStatement) {
    System.out.println("Foreach unsupported");
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



}
