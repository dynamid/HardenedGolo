
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;


/**
 * <p>This Visitor aims at symbolic testing of the golo program.
 * Read a golo code file with a Golo Function and generate inputs values for exploring all the possibles path
 * @author Qifan ZHOU
 */
public class IrSymbolicTestVisitor implements GoloIrVisitor {

  private Model model;
  private GoloModule moduleStatement;
  private GoloFunction functionStatement;

  private HashMap<String,VariableStatement> listOfVaribles;
  private HashMap<String,VariableStatement> listOfInputs;
  private LinkedList<ConstraintStatement> PathConstratint;
  private LinkedList<LinkedList<ConstraintStatement>> unsatisfiable_Path_List;
  private ExecutionTree executionTree;
  private ExecutionNode currentNode;

  private int flag;
  private int pathCounter;
  private String fileName;
  private ArrayList<String> outputCode = new ArrayList<>();
  private final int EXPLORE_MAX = 200; //this value can be changed or removed ! !


  public IrSymbolicTestVisitor() {
    model = new Model("Choco Solver for Symbolic Path Constraint");
    listOfVaribles = new HashMap<>();
    listOfInputs = new HashMap<>();
    PathConstratint= new LinkedList<>();
    unsatisfiable_Path_List= new LinkedList<>();
    executionTree = new ExecutionTree();
    currentNode = executionTree.getRoot();
    pathCounter=1;
    System.out.println(">>> Symbolic Testing Visitor Created. " );
  }


  /** ================= <Visitor methods> ================= */
  @Override
  public void visitModule(GoloModule module) {
    System.out.println(">>> GoloModule: " + module.getPackageAndClass());
    if(module.getFunctions().size()>1){
      System.out.println(">>> ERROR: Symbolic Testing Tool only support 1 GoloFunction per file.<<<" );
      System.exit(0);
    }
    moduleStatement=module;
    module.walk(this);

  }

  @Override
  public void visitFunction(GoloFunction function) {
    System.out.println(">>> Function: " + function.toString());

    functionStatement=function;
    fileName = "test_"+function.getName()+".golo";

    /* initialization first inputs */
    for (int i=0;i<function.getArity();i++) {
      String expr= function.getParameterNames().get(i);
      VariableStatement var = new VariableStatement( expr,expr,generatRandomPositiveNegitiveValue(IntVar.MIN_INT_BOUND, IntVar.MAX_INT_BOUND));
      listOfVaribles.put(var.getName(),var);
      listOfInputs.put(var.getName(),var);
    }
    initTestFile();

    System.out.println("----------------------------  Exploring Path 1  ----------------------------");
    displayInputs();
    function.walk(this);
  }

  @Override
  public void visitBlock(Block block) {
    block.walk(this);
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    VariableStatement var = parse_AssignmentExprToVariable(assignmentStatement);
    listOfVaribles.put(var.getName(),var);
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    //System.out.println(">>>ConditionalBranching: " + conditionalBranching.getCondition().toString());

     /* generate Execution Node from current condition */
    ConstraintStatement constraint = parse_ConditionExprToConstraint(conditionalBranching.getCondition());
    currentNode = generateNode(constraint);
    /* Explore the different branch */
    if(constraint.isTrue()) {
    /* Then Branch */
      flag = 1;
      conditionalBranching.getTrueBlock().accept(this);
    }else {
    /* Else branche */
      flag = 0;
      if (conditionalBranching.hasElseConditionalBranching()) {
        conditionalBranching.getElseConditionalBranching().accept(this);
      }
      if (conditionalBranching.hasFalseBlock()) {
        conditionalBranching.getFalseBlock().accept(this);
      }
    }

  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    //System.out.println(">>>ConstantStatement: " + constantStatement.getValue());
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    // System.out.println(">>>ReturnStatement: " + returnStatement.toString());
    returnStatement.walk(this);
    displayLocalVariable();
    updateTestFile();
    currentNode.setPathDone();
    PathConstratint = evalPath(currentNode);
    System.out.println(">>> Global Execution Tree:");
    executionTree.display();
    System.out.println(">>> last node in this execution :"+ currentNode.toString());

    if (PathConstratint!=null) {
        pathCounter++;
        HashMap<String, VariableStatement> newInputs = PathConstraintSolve(PathConstratint, listOfInputs);

      /* if current constraint can't be solved, setDone this path and jump to UnDo Node */
        while (newInputs==null){
          pathCounter++;
          unsatisfiable_Path_List.add(PathConstratint);
          currentNode.setDone();
          currentNode.setPathDone();
          PathConstratint = evalPath(currentNode);
          if (PathConstratint==null) {
            executionTree.display();
            finishExplore();
          }
          newInputs = PathConstraintSolve(PathConstratint, listOfInputs);
        }

        /* Restart the new Explore */
        reset(newInputs);
        if(pathCounter >= EXPLORE_MAX){
          System.out.println("MAX EXPLORE NUMBER REACHED ;"+pathCounter);
          System.exit(0);
        }
        functionStatement.walk(this);
    }else{
      finishExplore();
    }
  }




/** ================= < Main Algorithm Methods> ================= */

  /* this method transforms a "[var] = [number]" or "[var1] = [var2] + [number]" form assignment expression to a VariableStatement */
  private VariableStatement parse_AssignmentExprToVariable(AssignmentStatement assignment){
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
  private ConstraintStatement parse_ConditionExprToConstraint(ExpressionStatement condition){

    String[] tokens = condition.toString().split("[ ]+");

    if (tokens.length == 5) {
      if (tokens[1].equals("==")) {
        tokens[1] = "=";
      }
      if (tokens[3].equals("==")) {
        tokens[3] = "=";
      }
      return new ConstraintStatement(generateVariableFromReferenceExpr(tokens[0]), generateVariableFromReferenceExpr(tokens[2]), generateVariableFromReferenceExpr(tokens[4]), tokens[1], tokens[3]);
    }else if (tokens.length == 3){
      if (tokens[1].equals("==")) {
        tokens[1] = "=";
      }
      return  new ConstraintStatement(generateVariableFromReferenceExpr(tokens[0]), generateVariableFromReferenceExpr("0"), generateVariableFromReferenceExpr(tokens[2]), "+", tokens[1]);

    }else{
      System.out.println(">>>>>> Error: condition expression only support '[VAR1] op1 [VAR2] op2 [VAR3]' or [VAR1] op [VAR2] form <<<<<<<");
      System.exit(0);
    }
    return null;
  }

  private VariableStatement generateVariableFromReferenceExpr(String expr){
    if (isInteger(expr)){
      return new VariableStatement(Integer.parseInt(expr)); //create constant value
    }
    String name = expr.substring(expr.indexOf("=")+1, expr.indexOf("}"));
    return getVarFromList(name);
  }

 /* Main Function for solving the Path Constraint and generate the new inputs */
  private HashMap<String,VariableStatement> PathConstraintSolve(LinkedList<ConstraintStatement> pc, HashMap<String,VariableStatement> map){

  HashMap<String,IntVar> SolverVarList = new HashMap<>();
  for (ConstraintStatement aPc : pc) {
    constraintSolve(aPc, SolverVarList);
  }

  //System.out.println(model) ;
  try{
    model.getSolver().solve();
  }catch(OutOfMemoryError e){
    System.out.println(">>> Undefined by this Solver: "+ pc);
    return null;
  }
  ESat satisfaction = model.getSolver().isSatisfied();
  switch(satisfaction){
    case TRUE:    System.out.println(">>> Satisfiable: "+ pc); break;
    case FALSE:   System.out.println(">>> Unsatisfiable: "+ pc); return null;
    case UNDEFINED: System.out.println(">>> Undefined by this Solver: "+ pc); return null;
  }

    /* replace the former inputs by the new inputs generated by Constraint Solver */
  for (String key : map.keySet()) {
    if(SolverVarList.get(key)!=null&& map.get(key)!=null){
      map.get(key).setValue(SolverVarList.get(key).getValue());
    }
  }
  return map;
}

  private void constraintSolve(ConstraintStatement constraint,HashMap<String,IntVar> SolverVarList){
    IntVar var1 = evalVar(constraint.getVar1(), SolverVarList);
    IntVar var2 = evalVar(constraint.getVar2(), SolverVarList);
    IntVar var3 = evalVar(constraint.getVar3(), SolverVarList);
    String op1 = constraint.getOp1();
    String op2 = constraint.getOp2();

    addConstraintsModel(model, var1, var2, var3, op1, op2);
  }

  private void addConstraintsModel(Model model,IntVar var1, IntVar var2 , IntVar var3, String op1, String op2){
    switch(op1){
      case "+" : model.arithm(var1, op1, var2, op2, var3).post(); break;
      case "-" : model.arithm(var1, op1, var2, op2, var3).post(); break;
      case "*" : model.times(var1, var2,  var3).post(); break;
      case "/" : model.div(var1, var2,  var3).post(); break;
      case "%" : model.mod(var1,var2,var3).post(); break;
      default:     System.out.println(">>>>>> Error: operator '"+op1+"'is not supported in this version <<<<<<");
        System.exit(0); break;
    }

  }

  private ExecutionNode generateNode (ConstraintStatement constraint){
    ExecutionNode res = this.currentNode;
    ExecutionNode newNode = new ExecutionNode(constraint);
    if(executionTree.getRoot()==null){
      executionTree.setRoot(newNode);
      res = executionTree.getRoot();
    }else if(constraint.toString().equals(res.getCondition().toString())){
      res.setCondition(constraint);
    }
    else if(res.add(newNode,flag)) {
      res = newNode;
    }else{
      if(flag==1){
        res=res.getTrueBranch();
      }else{
        res=res.getFalseBranch();
      }
    }

    return res;
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


        /* eval the local variable */
        if((!isInput(v))&&(!isInteger(v.getSymb()))){

          String[] tokens = v.getSymb().split("[ ]");
          if (tokens.length == 1) {
            IntVar var1 = evalVar(getVarFromList(tokens[0]), SolverVarList);
            addConstraintsModel(model,var1, evalVar(new VariableStatement(0),SolverVarList), var,"+" , "=");  // VAR1 + 0 = VAR2

          }else if (tokens.length == 3){
            IntVar var1 = evalVar(getVarFromList(tokens[0]), SolverVarList);
            IntVar var2 = evalVar(getVarFromList(tokens[2]), SolverVarList);
            addConstraintsModel(model, var1, var2, var, tokens[1], "=");

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

  private LinkedList<ConstraintStatement> evalPath(ExecutionNode node){
    LinkedList<ConstraintStatement> pathConstratints= new LinkedList<>();
    ExecutionNode lastNodeUndo = node.tillUndo(node);
    if (lastNodeUndo == null){
      return null;
    }

    if(!lastNodeUndo.TrueDone()) {
      lastNodeUndo.addToConstraintList(pathConstratints,true);
    }else if (!lastNodeUndo.FalseDone()) {
      lastNodeUndo.addToConstraintList(pathConstratints,false);
    }

    return pathConstratints;
  }



/** ================= <Utilil Methods> ================= */

  private static int generatRandomPositiveNegitiveValue(int min, int max) {
  return  min + (int) (Math.random() * ((max - (min)) + 1));
}

  private void reset(HashMap<String,VariableStatement> newInputs) {
    System.out.println("---------------------------- Exploring Path "+ pathCounter+"  ----------------------------");
    System.out.println(">>> Path_Constraint: " + PathConstratint);
    displayInputs();

    /* initialization */
    model = new Model("Choco Solver for Symbolic Path Constraint");
    listOfVaribles = new HashMap<>();
    listOfInputs = newInputs;
    listOfVaribles = new HashMap<>();
    for (String key : listOfInputs.keySet()) {
       listOfVaribles.put(listOfInputs.get(key).getName(),listOfInputs.get(key));
    }
    PathConstratint= new LinkedList<>();
    currentNode = executionTree.getRoot();
  }

  private void finishExplore() {
    System.out.println("------------------------ Symbolic Execution Completed ----------------------");
    System.out.println(">>> Total Path : "+ pathCounter);
    System.out.println(">>> Unsatisfiable : "+ unsatisfiable_Path_List.size());
    for (LinkedList<ConstraintStatement> anUnsatisfiable_Path_List : unsatisfiable_Path_List) {
      System.out.print(anUnsatisfiable_Path_List + "  ");
    }
    if(unsatisfiable_Path_List.size()>0){
      System.out.println();
    }
    System.out.println(">>> Test the : "+ (pathCounter-unsatisfiable_Path_List.size())+" satisfiable paths: Execute the output files " +fileName  );

    outputTestFile();
    System.exit(0);
  }

  private static boolean isInteger(String s) {
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

  private boolean isInput(VariableStatement v) {
    return listOfInputs.get(v.getName())!=null;
  }

  private  VariableStatement getVarFromList(String name){
    if (isInteger(name)){
      return new VariableStatement(Integer.parseInt(name)); //create constant value
    }
    if(listOfVaribles.get(name)==null){
      System.out.println(">>>>>> Error: variable '"+name+"' does not exist in listOfVariables <<<<<<<");
      System.exit(0);
    }
    return listOfVaribles.get(name);
  }

  private void displayInputs(){
    System.out.print(">>> Inputs: ");
    for (String key : listOfInputs.keySet()) {
      System.out.print("["+key+"="+listOfInputs.get(key).getValue()+"] ");
    }
    System.out.println("");
  }

  private void displayLocalVariable(){
    System.out.print(">>> local_variables: ");
    for (String key : listOfVaribles.keySet()) {
      if(listOfInputs.get(key)==null) {
        System.out.print(listOfVaribles.get(key).toString()+"  ");
      }
    }
    System.out.println("");
  }

  private void initTestFile(){
    outputCode.add ( "module "+ functionStatement.getName()+"\n");
    outputCode.add ( "import "+ moduleStatement.getPackageAndClass()+"\n");
    outputCode.add ( "function main = |args| {\n");
  }

  private void updateTestFile(){
    String s1= "  println(\">>> Testing <" + functionStatement.getName()+"> with Inputs [";

    String s2 = "  "+ functionStatement.getName()+"(";

    for (int i=0;i<functionStatement.getArity();i++) {
      if(i>0){
        s1= s1+", ";
        s2= s2+", ";
      }
      String name= functionStatement.getParameterNames().get(i);
      s1= s1+name+"="+listOfInputs.get(name).getValue();
      s2 = s2+ listOfInputs.get(name).getValue();
    }
    s1 = s1+"]\")";
    s2 = s2+")\n";

    outputCode.add(s1);
    outputCode.add(s2);
  }

  private void outputTestFile(){
    String s= "  println(\">>> "+pathCounter+"  Path Explored Completed.\")\n";
    outputCode.add (s);
    outputCode.add ( "}");
    try {
      Files.write(Paths.get(fileName), outputCode, Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /* public void solverExample(HashMap<String,VariableStatement> listOfInputs){

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

      int i = 1;
      while (model.getSolver().solve()) {
        System.out.println("Solution " + i++ + " found : " + a + ", " + b);
      }
  }*/


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

    private void setName(String aName) {
      name = aName;
    }
    private void setSymb(String aSymb) {
      symbolicExpr = aSymb;
    }
    public void setValue(int aValue) {
      concretExpr = aValue;
    }

    private String getName() {
      return name;
    }
    private String getSymb() {
      return symbolicExpr;
    }
    private int getValue() {
      return concretExpr;
    }

    private boolean isConstant() {
      return isConstant;
    }
    private void setConstant(boolean b) {
      isConstant = b;
    }

    @Override
    public String toString (){
      return "<"+name+"='"+symbolicExpr+"'="+concretExpr+">";
    }
  }

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

    private VariableStatement getVar1() {
      return var1;
    }
    private VariableStatement getVar2() {
      return var2;
    }
    private VariableStatement getVar3() {
      return var3;
    }
    private String getOp1() {
      return op1;
    }
    private String getOp2() {
      return op2;
    }

    @Override
    public String toString (){
      return "<"+var1.getName()+op1+var2.getName()+op2+var3.getName()+">";
    }

    private boolean isTrue(){
      String str = " Judge the condition "+ this.toString();
      Model m  = new Model(str);
      VariableStatement variable1 = getVarFromList(var1.getName());
      VariableStatement variable2 = getVarFromList(var2.getName());
      VariableStatement variable3 = getVarFromList(var3.getName());
      IntVar v1 = m.intVar(variable1.getName(),variable1.getValue(),variable1.getValue());
      IntVar v2 = m.intVar(variable2.getName(),variable2.getValue(),variable2.getValue());
      IntVar v3 = m.intVar(variable3.getName(),variable3.getValue(),variable3.getValue());
      String o1 = op1;
      String o2 = op2;
      addConstraintsModel(m,v1,v2,v3,o1,o2);
      Solver solver = m.getSolver();

      solver.solve();
      ESat satisfaction = solver.isSatisfied();

      switch(satisfaction){
        case TRUE:    //System.out.println( this.toString()+" is True ");
                        return true;
        case FALSE:   //System.out.println( this.toString()+" is False ");
                        return false;
        case UNDEFINED: System.out.println(">>>>> ERROR:" + this.toString()+" is UNDEFINED condition <<<<<< "); System.exit(0); break;
      }

      System.out.println(m);
      return false;
    }

    private ConstraintStatement inverse(){
      ConstraintStatement res = new ConstraintStatement(var1,var2,var3,op1,op2);
      switch(op2){
        case "=" : res.op2 = "!="; break;
        case "!=" :res.op2 = "=" ; break;
        case ">" : res.op2 = "<="; break;
        case "<" : res.op2 = ">="; break;
        case "<=" : res.op2 =">"; break;
        case ">=" : res.op2="<"; break;
        default : System.out.println("ERROR : inverse condition unsupported: "+ op2 ); System.exit(0); break;
      }
      return res;
    }

  }

  /** ================= <Sub class ExecutionTree> ================= */

  private class ExecutionNode {
    private ConstraintStatement myCondition;
    private ExecutionNode TrueBranch;
    private ExecutionNode FalseBranch;
    private ExecutionNode FatherNode;
    private boolean BoolFromFather;
    private boolean TrueIsDone;
    private boolean FalseIsDone;

    private ExecutionNode(ConstraintStatement aCondition){
      myCondition = aCondition;
      TrueIsDone = false;
      FalseIsDone = false;
      TrueBranch = null;
      FalseBranch = null;
      FatherNode = null;
      BoolFromFather=false;
    }

    public void setCondition(ConstraintStatement aCondition) {
      myCondition = aCondition;
    }
    public ConstraintStatement getCondition() {
      return myCondition;
    }

    private void setTrueBranch(ExecutionNode left) {
      this.TrueBranch = left;
    }
    private void setFalseBranch(ExecutionNode right ) {
      this.FalseBranch = right;
    }
    private ExecutionNode getTrueBranch() {
      return TrueBranch;
    }
    private ExecutionNode getFalseBranch() {
      return FalseBranch;
    }
    private void setTrueDone(boolean done){
      TrueIsDone = done;
    }
    private void setFalseDone(boolean done){
      FalseIsDone = done;
    }
    private boolean  TrueDone() {
      return TrueIsDone;
    }
    private boolean FalseDone() {
      return FalseIsDone;
    }
    private void setDone(){
      if(!TrueDone()){
        setTrueDone(true);
      }else if (!FalseDone()){
        setFalseDone(true);
      }
    }

    private ExecutionNode getFather() {
      if (this.FatherNode==null){
        return null;
      }
      return FatherNode;
    }


    public boolean add(ExecutionNode node, int flag ) {

        if (flag==1){
          if(this.getTrueBranch()!=null){
            return false;
          }else {
            node.FatherNode = this;
            setTrueBranch(node);
            node.BoolFromFather = true;
          }
        }else if(flag==0) {
          if (this.getFalseBranch() != null) {
            return false;
          } else {
            node.FatherNode = this;
            setFalseBranch(node);
            node.BoolFromFather = false;
          }
        }
        return true;
    }

    private ExecutionNode tillUndo(ExecutionNode node) {
      setPathDone();
      if((node.TrueDone())&&(node.FalseDone())){
        if (node.FatherNode==null){
          return null;
        }else {
          if (node.BoolFromFather) {
            node.FatherNode.setTrueDone(true);
          } else {
            node.FatherNode.setFalseDone(true);
          }
          return tillUndo(node.FatherNode);
        }

      }
      return node;
    }

    private void setPathDone(){
      if(this.getCondition().isTrue()){
        this.setTrueDone(true);
      }else{
        this.setFalseDone(true);
      }
      if((this.TrueDone())&&(this.FalseDone())){
        setFatherDone();
      }
    }

    private void setFatherDone(){
        if(this.getFather()!=null){
          if(this.BoolFromFather){
              this.getFather().setTrueDone(true);
          }else{
              this.getFather().setTrueDone(false);
          }
        }
    }

    private void addToConstraintList(LinkedList<ConstraintStatement> list, boolean flag){
        if (flag) {
          list.add(this.getCondition());
        } else {
          list.add(this.getCondition().inverse());
        }

        if(this.getFather()!=null) {
          this.getFather().addToConstraintList(list, BoolFromFather);
        }
    }

    @Override
    public String toString(){
      if(this.FatherNode==null){
        return (myCondition.toString());
      }
      return this.myCondition.toString() + " : " +BoolFromFather + " Branch of :" + FatherNode.getCondition().toString();
    }
  }

  private class ExecutionTree {
    private ExecutionNode root;

    private ExecutionTree() {
      root = null;
    }
    private ExecutionNode getRoot() {
      return root;
    }

    private void setRoot(ExecutionNode node) {
      root = node;
    }


    private  void display() {
        int maxLevel = maxLevel(root);

        printNodeInternal(Collections.singletonList(root), 1, maxLevel);
      }

      private  void printNodeInternal(List<ExecutionNode> nodes, int level, int maxLevel) {
        if (nodes.isEmpty() || isAllElementsNull(nodes))
          return;

        int floor = maxLevel - level;
        int endgeLines = (int) Math.pow(2, (Math.max(floor - 1, 0)));
        int firstSpaces = (int) Math.pow(2, (floor)) - 1;
        int betweenSpaces = (int) Math.pow(2, (floor + 1)) - 1;

        printWhitespaces(firstSpaces);

        List<ExecutionNode> newNodes = new ArrayList<>();
        for (ExecutionNode node : nodes) {
          if (node != null) {
            if(node.TrueIsDone){System.out.print("[1]");}else{System.out.print("[0]");}
            System.out.print(node.getCondition());
            if(node.FalseDone()){System.out.print("[1]");}else{System.out.print("[0]");}
            newNodes.add(node.getTrueBranch());
            newNodes.add(node.getFalseBranch());
          } else {
            newNodes.add(null);
            newNodes.add(null);
            System.out.print(" ");
          }

          printWhitespaces(betweenSpaces);
        }
        System.out.println("");

        for (int i = 1; i <= endgeLines; i++) {
          for (ExecutionNode node : nodes) {
            printWhitespaces(firstSpaces - i);
            if (node == null) {
              printWhitespaces(endgeLines + endgeLines + i + 1);
              continue;
            }

            if (node.getTrueBranch() != null)
              System.out.print("/");
            else
              printWhitespaces(1);

            printWhitespaces(i + i - 1);

            if (node.getFalseBranch() != null)
              System.out.print("\\");
            else
              printWhitespaces(1);

            printWhitespaces(endgeLines + endgeLines - i);
          }

          System.out.println("");
        }

        printNodeInternal(newNodes, level + 1, maxLevel);
      }

      private  void printWhitespaces(int count) {
        for (int i = 0; i < count; i++)
          System.out.print(" ");
      }

      private  int maxLevel(ExecutionNode node) {
        if (node == null)
          return 0;

        return Math.max(maxLevel(node.TrueBranch), maxLevel(node.FalseBranch)) + 1;
      }

      private  <T> boolean isAllElementsNull(List<T> list) {
        for (Object object : list) {
          if (object != null)
            return false;
        }

        return true;
      }



  }


/** ================= <Unsupported visitor methods > ================= */

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
