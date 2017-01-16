
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
  private Model model;
  private HashMap<String,VariableStatement> listOfVaribles;
  private HashMap<String,VariableStatement> listOfInputs;
  private LinkedList<ConstraintStatement> PathConstratint;

  private int N_inputs;
  private static int N_inputs_counter;


  public IrSymbolicTestVisitor() {
    model = new Model("Choco Solver for Symbolic Path Constraint");
    listOfVaribles = new HashMap<String,VariableStatement>();
    listOfInputs = new HashMap<String,VariableStatement>();
    PathConstratint= new LinkedList<ConstraintStatement>();

    System.out.println("--------- Symbolic Testing Visitor Created --------- " );
  }



  /** ================= <Visitor methods> ================= */
  @Override
  public void visitModule(GoloModule module) {
    System.out.println(">>>GoloModule: " + module.toString());
    module.walk(this);
  }

  @Override
  public void visitFunction(GoloFunction function) {
    System.out.println(">>>Function: " + function.toString());
    N_inputs=function.getArity();
    N_inputs_counter = 0;
    function.walk(this);

  }

  @Override
  public void visitBlock(Block block) {
    block.walk(this);
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
    if(N_inputs_counter < N_inputs ){
      VariableStatement var = new VariableStatement(ref.getName(),ref.getName(),generatRandomPositiveNegitiveValue(IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND));
      System.out.println("input "+(N_inputs_counter+1)+" : "+var);
      listOfVaribles.put(var.getName(),var);
      listOfInputs.put(var.getName(),var);
      N_inputs_counter++;
    }
  }



  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    //System.out.println(">>>AssignmentStatement: " + assignmentStatement.toString());
    VariableStatement var = parse_AssignmentExprToVariable(assignmentStatement);
    listOfVaribles.put(var.getName(),var);
    System.out.println(">>>AssignmentStatement: "+var);

    //assignmentStatement.walk(this);
  }


  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    //System.out.println(">>>ConditionalBranching: " + conditionalBranching.toString());
   // System.out.println(">>>ConditionalBranching: " + conditionalBranching.getCondition().toString());
    ConstraintStatement constraint = parse_ConditionExprToConstraint(conditionalBranching.getCondition());
    PathConstratint.add(constraint);
    //constraintSolve(constraint);
    //System.out.println(">>>to Constraint: " + constraint);

   // conditionalBranching.getCondition().accept(this);
    //conditionalBranching.getTrueBlock().accept(this);

  }




  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    //System.out.println(">>>ConstantStatement: " + constantStatement.getValue());
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    // System.out.println(">>>ReturnStatement: " + returnStatement.toString());
    returnStatement.walk(this);
   // System.out.println(">>>>>>>>before solve :"+listOfInputs);
    PathConstraintSolve(PathConstratint,listOfInputs);
    System.out.println(">>>>>>>>after solve :"+listOfInputs);
  }

  /** <Visitor methods> End */








/** ================= <Utilil Methods> ================= */

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

  public boolean isInput(VariableStatement v) {
    if(listOfInputs.get(v.getName())!=null){
      return true;
    }
    return false;
  }

  public  VariableStatement getVarFromList(String name){
    if (isInteger(name)){
      return new VariableStatement(Integer.parseInt(name)); //create constant value
    }
    if(listOfVaribles.get(name)==null){
      System.out.println(">>>>>> Error: variable '"+name+"' does not exist in listOfVariables <<<<<<<");
      System.exit(0);
    }
    return listOfVaribles.get(name);
  }

  public static int generatRandomPositiveNegitiveValue(int min, int max) {
    int res = min + (int) (Math.random() * ((max - (min)) + 1));
    return res;
  }

  private VariableStatement generateVariableFromReferenceExpr(String expr){
    if (isInteger(expr)){
      return new VariableStatement(Integer.parseInt(expr)); //create constant value
    }
    String name = expr.substring(expr.indexOf("=")+1, expr.indexOf("}"));
    return getVarFromList(name);
  }



  //this method transforms a "[var] = [number]" or "[var1] = [var2] + [number]" form assignment expression to a VariableStatement
  public VariableStatement parse_AssignmentExprToVariable(AssignmentStatement assignment){
    VariableStatement var = new VariableStatement();
    var.setName(assignment.getLocalReference().getName());

    String[] tokens = assignment.getExpressionStatement().toString().split("[ ]+");
    if(tokens.length==1){
      // 'var = number ' form

      var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue());

      if(isInteger(tokens[0])){
        var.setSymb(tokens[0]);
        var.setConstant(true);
      }else{
        var.setSymb(evalSymbExpr(tokens[0]));
        var.setConstant(false);
      }

    }else if(tokens.length == 3){
      // 'var1 =var2 + number ' form

      switch(tokens[1]) {
        case "+" :   var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue() + generateVariableFromReferenceExpr(tokens[2]).getValue()); break;
        case "-" :   var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue() - generateVariableFromReferenceExpr(tokens[2]).getValue()); break;
        case "*" :   var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue() * generateVariableFromReferenceExpr(tokens[2]).getValue()); break;
        case "/" :   var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue() / generateVariableFromReferenceExpr(tokens[2]).getValue()); break;
        case "%" :   var.setValue(generateVariableFromReferenceExpr(tokens[0]).getValue() % generateVariableFromReferenceExpr(tokens[2]).getValue()); break;
        default:     System.out.println(">>>>>> Error: operator '"+tokens[1]+"'is not supported in this version <<<<<<");
                     System.exit(0); break;
      }

      if(isInteger(tokens[0])&&isInteger(tokens[2])){
          var.setSymb( Integer.toString(var.getValue()));
          var.setConstant(true);
      }else{
          String s1 =evalSymbExpr(tokens[0]);
          String s2 =evalSymbExpr(tokens[2]);
          if(isInteger(s1)&&isInteger(s2)){
              var.setSymb( Integer.toString(var.getValue()));
              var.setConstant(true);
          }else{
              var.setSymb(s1 + " " + tokens[1] + " " + s2);
              var.setConstant(false);
          }
      }

    }else{
      System.out.println(">>>>>> Error: assignement only support 'VAR1 = NUMBER1' or 'VAR1 = VAR2 + NUMBER1' form <<<<<<<");
      System.exit(0);
    }

    return var;
  }

  //this method transforms a "[var1] op1 [var2] op2 [var3]" form condition expression to a ConstraintStatement
  public ConstraintStatement parse_ConditionExprToConstraint(ExpressionStatement condition){

    String[] tokens = condition.toString().split("[ ]+");

    if (tokens.length == 5) {
      if (tokens[1].equals("==")) {
        tokens[1] = "=";
      }
      if (tokens[3].equals("==")) {
        tokens[3] = "=";
      }
      ConstraintStatement constraint = new ConstraintStatement(generateVariableFromReferenceExpr(tokens[0]), generateVariableFromReferenceExpr(tokens[2]), generateVariableFromReferenceExpr(tokens[4]), tokens[1], tokens[3]);
      return constraint;
    }else if (tokens.length == 3){
      if (tokens[1].equals("==")) {
        tokens[1] = "=";
      }
      ConstraintStatement constraint = new ConstraintStatement(generateVariableFromReferenceExpr(tokens[0]), generateVariableFromReferenceExpr("0"), generateVariableFromReferenceExpr(tokens[2]), "+", tokens[1]);
      return constraint;

    }else{
      System.out.println(">>>>>> Error: condition expression only support '[VAR1] op1 [VAR2] op2 [VAR3]' or [VAR1] op [VAR2] form <<<<<<<");
      System.exit(0);
    }
    return null;
  }



  /* Main Function for solving the Path Constraint and generate the new inputs */
  public void PathConstraintSolve(LinkedList<ConstraintStatement> pc, HashMap<String,VariableStatement> map){

    HashMap<String,IntVar> SolverVarList = new HashMap<String,IntVar>();
    ListIterator<ConstraintStatement> listIterator = pc.listIterator();
    while (listIterator.hasNext()) {
      constraintSolve(listIterator.next(),SolverVarList);
    }

    System.out.println(model) ;
    model.getSolver().solve();

    /* replace the former inputs by the new inputs generated by Constraint Solver */
    for (String key : map.keySet()) {
      if(SolverVarList.get(key)!=null){
        map.get(key).setValue(SolverVarList.get(key).getValue());
      }
    }
  }

  private void constraintSolve(ConstraintStatement constraint,HashMap<String,IntVar> SolverVarList){
    IntVar var1 = evalVar(constraint.getVar1(), SolverVarList);
    IntVar var2 = evalVar(constraint.getVar2(), SolverVarList);
    IntVar var3 = evalVar(constraint.getVar3(), SolverVarList);
    String op1 = constraint.getOp1();
    String op2 = constraint.getOp2();

    addConstraintsModel(var1, var2, var3, op1, op2);
  }

  private void addConstraintsModel(IntVar var1, IntVar var2 , IntVar var3, String op1, String op2){
    switch(op1){
      case "+" : model.arithm(var1, op1, var2, op2, var3).post(); break;
      case "-" : model.arithm(var1, op1, var2, op2, var3).post(); break;
      case "*" : model.times(var1, var2,  var3).post(); break;
      case "/" : model.div(var1, var2,  var3).post(); break;
      default:     System.out.println(">>>>>> Error: operator '"+op1+"'is not supported in this version <<<<<<");
                   System.exit(0); break;
    }

  }

  private String evalSymbExpr(String expr){
    if(isInteger(expr)){
      return expr;
    }
    String str = expr.substring(expr.indexOf("=")+1, expr.indexOf("}"));
    VariableStatement var = listOfVaribles.get(str);
    if (var==null){
      System.out.println(">>>>>> Error: SybExpression '"+str+"'does not exist <<<<<<");
      System.exit(0);
    }else if(isInput(var)){
      return var.getName();
    }

    return var.getSymb();
  }

  private IntVar evalVar(VariableStatement v, HashMap<String,IntVar> SolverVarList){
    IntVar var = SolverVarList.get(v.getName());
    if(var==null){
      if(v.isConstant()){
        var = model.intVar(v.getName(),v.getValue(),v.getValue());
      }else {
        var = model.intVar(v.getName(), IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND);


        /** eval the local variable */
        if((!isInput(v))&&(!isInteger(v.getSymb()))){

          String[] tokens = v.getSymb().toString().split("[ ]");
          if (tokens.length == 1) {
            IntVar var1 = evalVar(getVarFromList(tokens[0]), SolverVarList);
            addConstraintsModel(var1, evalVar(new VariableStatement(0),SolverVarList), var,"+" , "=");  // VAR1 + 0 = VAR2

          }else if (tokens.length == 3){
            IntVar var1 = evalVar(getVarFromList(tokens[0]), SolverVarList);
            IntVar var2 = evalVar(getVarFromList(tokens[2]), SolverVarList);
            addConstraintsModel(var1, var2, var, tokens[1], "=");

          }else{
            System.out.println(">>>>>> Error: SymbExpr fomat unsupported <<<<<<<");
            System.exit(0);
          }

        }

      }
      SolverVarList.put(v.getName(),var);
    }
    return var;
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

    private VariableStatement(){
      setConstant(false);
    }

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
    public void setConstant(boolean b) {
      isConstant = b;
    }

    @Override
    public String toString (){
      return "<"+name+"='"+symbolicExpr+"'="+concretExpr+">";
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
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    functionInvocation.walk(this);
    // System.out.println(">>>FunctionInvocation : " + functionInvocation.toString());

  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {

  }

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
