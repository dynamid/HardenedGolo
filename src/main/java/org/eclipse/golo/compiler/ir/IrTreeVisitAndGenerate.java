/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.ir;

import org.eclipse.golo.runtime.OperatorType;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.Charset;


/* TODO List :
    - Remove the ";" at the and of a function, before the "with return".
       ==> needs to check consequences, but this ";" has to be added at the end of each instruction.
       ==> Be careful with blocks that do not add ";" at their end
    - Correct the miss of ";" when calling a function without result. A return is generated

    - Propose a turnover or a translation for some internal function like println

    - Add a command line parameter to siwtch between Int and Int32 environments.

  Solved :
   - Correct parenthesis problem in nested expressions.
     Ex :
                        r/4 - 100*n
     provides no more :
                        ( - )
                            ( / )
                                ( r )
                                (  of_int 4 )
                            ( * )
                                (  of_int 100 )
                                ( n )
     but mor correctly :

                        ( - )
                            ( ( / )
                                ( r )
                                (  of_int 4 ) )
                            ( ( * )
                                (  of_int 100 )
                                ( n ) )


   - Correct indentation : incr/decr are now managed by the owner. Each visitor method no more starts with incr()
   - Add the final ";", at the end of an "if" structure
 */


/**
 * <p>This Visitor aims at verifying the correctness of the golo program.
 * It generates a .mlw file containing some verification conditions.
 * Such kind of file is next usable through WHY3 tool.</p>
 *
 * <p>This Visitor is a 1-pass. It generates WhyML file while parsing the IR.</p>
 * @author Raphael Laurent
 */
public class IrTreeVisitAndGenerate implements GoloIrVisitor {

  private Context context;
  private String sourcePath;
  private String destFile;
  private int[] charsPerLine;
  private ArrayList<String> whyMLcode = new ArrayList<>();
  private ArrayList<functionNameArity> functionsDefined = new ArrayList<>();
  private ArrayList<functionNameArity> functionsCalled = new ArrayList<>();

  /// If true, the verification has to consider integers with 32 bits
  private boolean int32;
  private String usedInt;

  /// Indentation management
  private int spacing = 0;
  private final static int INDENTATION_WIDTH = 4;



  public IrTreeVisitAndGenerate (String sourcePath, int[] charsPerLine, String destFile){
    this(sourcePath, charsPerLine, destFile, false);
  }

  public IrTreeVisitAndGenerate (String sourcePath, int[] charsPerLine, String destFile, boolean int32){
    super();
    this.sourcePath=sourcePath;
    this.charsPerLine=Arrays.copyOf(charsPerLine, charsPerLine.length);
    this.destFile=destFile;
    this.int32=int32;
    if(int32) {
      usedInt="int32";
    } else {
      usedInt="int";
    }
  }



  //================= <Sub class functionNameArity> =================
  private static class functionNameArity {
    private String name;
    private int arity;
    private PositionInSourceCode positionInSourceCode;

    private functionNameArity(String name, int arity){
      this.name = name;
      this.arity = arity;
    }

    private functionNameArity(String name, int arity, PositionInSourceCode pos){
      this(name,arity);
      this.positionInSourceCode=pos;
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

    /** Check if function has same name, and greater or same arity than param */
    private boolean isSimilar(functionNameArity fndefined){
      return fndefined != null &&
             fndefined.getName().equals(this.name) &&
             fndefined.getArity() <= this.arity;
    }

    public String toString (){
      return name+arity;
    }
  }
  //================= </Sub class functionNameArity> =================









  //================= </Sub class Context> =================
  private static final class Context {
    private final Deque<ReferenceTable> referenceTableStack = new LinkedList<>();
  }
  //================= </Sub class Context> =================










  //================= <Code generation methods> =================
  /** Return an indentation prefix String (containing spaces according to the depth level of the current code) */
  private String space() {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < spacing; i++) {
      buf.append(" ");
    }
    return buf.toString();
  }

  /** Augment the indentation level into the produced code. */
  private void incr() {
    spacing = spacing + INDENTATION_WIDTH;
  }

  /** Reduce the indentation level into the produced code. */
  private void decr() {
    spacing = spacing - INDENTATION_WIDTH;
  }

  private void appendWhyMLLastString(String toAppend){
    whyMLcode.set(whyMLcode.size()-1,whyMLcode.get(whyMLcode.size()-1) + toAppend);
  }
  //================= </Code generation methods> =================









  private String getSourceCodeBlocksLines(GoloElement element){
    String str="";
    int numberChars = 0;
    try {
      if (element.getASTNode().jjtGetLastToken().endLine > element.getASTNode().jjtGetFirstToken().beginLine) {
        numberChars += charsPerLine[element.getASTNode().jjtGetFirstToken().beginLine - 1];
      }
      for (int i = element.getASTNode().jjtGetFirstToken().beginLine + 1; i < element.getASTNode().jjtGetLastToken().endLine; i++) {
        numberChars += charsPerLine[i - 1];
      }
      numberChars += element.getASTNode().jjtGetLastToken().endColumn;
      str = "#\"" + sourcePath + "\" "
        + element.getASTNode().jjtGetFirstToken().beginLine + " "
        + (element.getASTNode().jjtGetFirstToken().beginColumn - 1) + " "
        + numberChars + "#";
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    return str;
  }

  private String getSourceCodeModuleLines (GoloElement element){
    String str;
    int numberChars = 0;
    for(int i = element.getASTNode().jjtGetFirstToken().beginLine; i <= charsPerLine.length; i++) {
      numberChars += charsPerLine[i-1];
    }
    str = "#\"" + sourcePath + "\" "
      + element.getASTNode().jjtGetFirstToken().beginLine + " "
      + (element.getASTNode().jjtGetFirstToken().beginColumn - 1) + " "
      + numberChars + "#";
    return str;
  }

  private void testFunctionsCalledExist(){
    boolean exists;
    for(functionNameArity fnc: functionsCalled){
      exists=false;
      for(functionNameArity fnd: functionsDefined){
        if(fnc.isSimilar(fnd))
          exists=true;
      }
      if(!exists){
        System.out.println("Function doesn't exist: "+fnc.getName()+" at line "+fnc.getLine()+", column "+fnc.getColumn());
      }
    }
  }




  //================= <Visitor methods> =================
  @Override
  public void visitModule(GoloModule module) {
    this.context = new Context();
    String moduleName = module.getPackageAndClass().toString().replaceAll("\\.", "");
    moduleName = moduleName.substring(0, 1).toUpperCase() + moduleName.substring(1);
    whyMLcode.add("module " + moduleName);
    incr();
    whyMLcode.add(space() + getSourceCodeModuleLines(module));
    if(int32) {
      whyMLcode.add(space() + "use import mach.int.Int32");//temporary import
    } else {
      whyMLcode.add(space() + "use import mach.int.Int");//temporary import
    }
    whyMLcode.add(space() + "use import ref.Ref"); //temporary import
    // whyMLcode.add(space() + "use import mach.int.Int"); //temporary import Seems useless
    whyMLcode.add(space() + "function "+usedInt+" : "+usedInt);
    whyMLcode.add(space() + "constant null : "+usedInt); //temporary null type creation
    whyMLcode.add(space() + "exception Return ()"); // temporary exception declaration
    module.walk(this);
    decr();
    whyMLcode.add(space() +"end");
    try {
      Files.write(Paths.get(destFile), whyMLcode, Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    testFunctionsCalledExist();
  }

  @Override
  public void visitFunction(GoloFunction function) {
    //System.out.println("Function: "+function.getName());
    functionsDefined.add(new functionNameArity(function.getName(),function.getArity()));
    whyMLcode.add(space() + "let " + function.getName() + " ");
    appendWhyMLLastString(getSourceCodeBlocksLines(function) + " = ");
    incr();
    whyMLcode.add(space() + "fun ");
    visitFunctionDefinition(function);
    decr();
  }


  private void visitFunctionDefinition(GoloFunction function) {
    LinkedList<LocalReference> refList = function.returnOnlyReference(this);
    for (String param : function.getParameterNames()) {
      appendWhyMLLastString("( " + param + " )");
    }
    if (function.getParameterNames().isEmpty()){
      appendWhyMLLastString("()");
    }
    if (function.isVarargs()) {
      System.out.println("varargs unsupported");
      appendWhyMLLastString(" (varargs)");
    }
    if (function.isSynthetic()) {
      System.out.println("synthetic unsupported");
      appendWhyMLLastString(" (synthetic, " + function.getSyntheticParameterCount() + " synthetic parameters)");
      if (function.getSyntheticSelfName() != null) {
        appendWhyMLLastString(" (selfname: " + function.getSyntheticSelfName() + ")");
      }
    }
    appendWhyMLLastString(" -> ");

    incr();
    //Add specification
    whyMLcode.add(space()+function.getSpecification());

    //Declaration of local variables
    for (LocalReference ref: refList) {
      boolean isParam = false;
      for (String param: function.getParameterNames()){
        if (ref.getName().equals(param)) {
          isParam = true;
        }
      }
      if(!isParam && !ref.isConstant()){
        whyMLcode.add(space() + "let " + ref.getName() + " = ref "+usedInt+" in ");
      }
    }
    whyMLcode.add(space() + "let return = ref "+usedInt+" in try");
    incr();
    function.walk(this);
    decr();
    whyMLcode.add(space() + ";");
    whyMLcode.add(space() + "with Return -> !return end");
    decr();
  }

  @Override
  public void visitBlock(Block block) {
    ReferenceTable referenceTable = block.getReferenceTable();
    context.referenceTableStack.push(referenceTable);
    if (block.isEmpty()) { return; }
    // space(); System.out.println("(* Block *)");
    whyMLcode.add(space() + "begin (");
    incr();
    whyMLcode.add(space() + getSourceCodeBlocksLines(block));
    block.walk(this);
    decr();
    whyMLcode.add(space() + ") end");
    context.referenceTableStack.pop();
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
    incr();
    // space(); System.out.println("(* - " + ref + " *)");
    decr();
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    whyMLcode.add(space() + "(return := ");
    returnStatement.walk(this);
    appendWhyMLLastString(");");
    whyMLcode.add(space() + "(raise Return)");
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    /* space();
    System.out.println("Function call: " + functionInvocation.getName()
        + ", on reference? -> " + functionInvocation.isOnReference()
        + ", on module state? -> " + functionInvocation.isOnModuleState()
        + ", anonymous? -> " + functionInvocation.isAnonymous()
        + ", named arguments? -> " + functionInvocation.usesNamedArguments());*/
    if (!functionInvocation.isOnReference() && !functionInvocation.isOnModuleState()) {
      functionsCalled.add(new functionNameArity(functionInvocation.getName(),functionInvocation.getArity(), functionInvocation.getPositionInSourceCode()));
      whyMLcode.add(space() + functionInvocation.getName() + " ");
    }
    else {
      /* ReferenceTable table = context.referenceTableStack.peek();
      System.out.println(table.get(functionInvocation.getName()));
      System.out.println(table.hasReferenceFor(functionInvocation.getName()));*/

      // Need to support closures before being able to implement this
      System.out.println("Function call on reference or module state unsupported");
      whyMLcode.add("Function call on reference or module state not implemented");
    }
    if (functionInvocation.getArity()==0) {
      appendWhyMLLastString("(");
    }
    functionInvocation.walk(this);
    if (functionInvocation.getArity()==0) {
      appendWhyMLLastString(")");
    }
  }


  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    whyMLcode.add(space() + "if ");
    incr();
    conditionalBranching.getCondition().accept(this);
    decr();
    whyMLcode.add(space() + "then");
    incr();
    conditionalBranching.getTrueBlock().accept(this);
    decr();

    if (conditionalBranching.hasFalseBlock()) {
      whyMLcode.add(space() + "else");
      incr();
      conditionalBranching.getFalseBlock().accept(this);
      appendWhyMLLastString(";");
      decr();
    } else if (conditionalBranching.hasElseConditionalBranching()) {
      whyMLcode.add(space() + "else");
      incr();
      conditionalBranching.getElseConditionalBranching().accept(this);
      decr();
    } else {
      appendWhyMLLastString(";");
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    String refName = assignmentStatement.getLocalReference().getName();
    // space(); System.out.println("(* Assignment: " + assignmentStatement.getLocalReference() + " *)");
    if (assignmentStatement.getLocalReference().isConstant()) {
      whyMLcode.add(space() + "let " + refName + " = ");
      incr();
      assignmentStatement.walk(this);
      decr();
      appendWhyMLLastString(" in");
    } else {
      whyMLcode.add(space() + "( "  + refName + " := ");
      incr();
      assignmentStatement.walk(this);
      decr();
      appendWhyMLLastString(" );");
    }
  }



  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    LocalReference reference = referenceLookup.resolveIn(context.referenceTableStack.peek());
    // space(); System.out.println("(* Reference lookup, Constant? " + reference.isConstant() + " *)");
    if (reference.isConstant()) {
      appendWhyMLLastString(referenceLookup.getName());
    } else {
      appendWhyMLLastString("(!" + referenceLookup.getName() + ") ");
    }
  }


  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    // System.out.print(" (* Constant = *) ");
    if(int32 && constantStatement.getValue() instanceof Integer) {
      // In case of 32bits literal integer, we have to call the of_int function on the absolute value of the constant.
      int v = ((Integer) constantStatement.getValue()).intValue();
      if(v<0) {
        appendWhyMLLastString("(  - ( of_int " + (-v) + " )) ");
      } else {
        appendWhyMLLastString("( of_int " + v + " ) ");
      }
    } else {
      appendWhyMLLastString(""+constantStatement.getValue());
    }
  }



  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    // space(); System.out.println("(* Binary operator: *)");
    //TODO : support all operators OperatorType
    //TODO : Moving constants out of the method
    List<OperatorType> supportedOperators = new ArrayList<>();
    supportedOperators.addAll(Arrays.asList(OperatorType.AND,
      OperatorType.PLUS,
      OperatorType.MINUS,
      OperatorType.TIMES,
      OperatorType.DIVIDE,
      OperatorType.MODULO,
      OperatorType.LESS,
      OperatorType.LESSOREQUALS,
      OperatorType.MORE,
      OperatorType.MOREOREQUALS));
    if (supportedOperators.contains(binaryOperation.getType())) {
      appendWhyMLLastString("( ( " + binaryOperation.getType() + " ) ");
    } else if (binaryOperation.getType() == OperatorType.EQUALS) {
      appendWhyMLLastString("( ( = ) ");
    } else if (binaryOperation.getType() == OperatorType.NOTEQUALS) {
      appendWhyMLLastString("( ( <> ) ");
    } else {
      System.out.println("Operator not supported: "+binaryOperation.getType());
      appendWhyMLLastString("Operator not supported: "+binaryOperation.getType());
    }
    binaryOperation.walk(this);
    appendWhyMLLastString(" )");
  }













  //================= <Missing visitor methods -- Unsupported parts of the language> =================

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    System.out.println("Unary operator unsupported");
    whyMLcode.add(space() + "Unary operator: " + unaryOperation.getType());
    incr();
    unaryOperation.getExpressionStatement().accept(this);
    decr();
  }




  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    incr();
    System.out.println("Case unsupported");
    whyMLcode.add(space() + "Case");
    incr();
    for (WhenClause<Block> c : caseStatement.getClauses()) {
      whyMLcode.add(space() + "When");
      incr();
      c.condition().accept(this);
      c.action().accept(this);
      decr();
    }
    whyMLcode.add(space() + "Otherwise");
    caseStatement.getOtherwise().accept(this);
    decr();
  }

  @Override
  public void visitMatchExpression(MatchExpression matchExpression) {
    incr();
    System.out.println("Match unsupported");
    whyMLcode.add(space() + "Match");
    incr();
    for (WhenClause<?> c : matchExpression.getClauses()) {
      c.accept(this);
    }
    whyMLcode.add(space() + "Otherwise");
    matchExpression.getOtherwise().accept(this);
    decr();
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    System.out.println("When unsupported");
    whyMLcode.add(space() + "When");
    incr();
    whenClause.walk(this);
    decr();
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    System.out.println("Loop unsupported");
    whyMLcode.add(space() + "Loop");
    incr();
    if (loopStatement.hasInitStatement()) {
      loopStatement.getInitStatement().accept(this);
    }
    loopStatement.getConditionStatement().accept(this);
    loopStatement.getBlock().accept(this);
    if (loopStatement.hasPostStatement()) {
      loopStatement.getPostStatement().accept(this);
    }
    decr();
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement foreachStatement) {
    System.out.println("Foreach unsupported");
    whyMLcode.add(space() + "Foreach");
    incr();
    for (LocalReference ref : foreachStatement.getReferences()) {
      ref.accept(this);
    }
    foreachStatement.getIterable().accept(this);
    if (foreachStatement.hasWhenClause()) {
      whyMLcode.add(space() + "When:");
      foreachStatement.getWhenClause().accept(this);
    }
    foreachStatement.getBlock().accept(this);
    decr();
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    System.out.println("Method invocation unsupported");
    whyMLcode.add(space() + "Method invocation: "+methodInvocation.getName()+", null safe? -> "+methodInvocation.isNullSafeGuarded());
    incr();
    methodInvocation.walk(this);
    decr();
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    System.out.println("Throw unsupported");
    whyMLcode.add(space() + "Throw");
    incr();
    throwStatement.getExpressionStatement().accept(this);
    decr();
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    System.out.println("Try Catch Finally unsupported");
    whyMLcode.add(space() + "Try");
    incr();
    tryCatchFinally.getTryBlock().accept(this);
    decr();
    if (tryCatchFinally.hasCatchBlock()) {
      whyMLcode.add(space() + "Catch: " + tryCatchFinally.getExceptionId());
      incr();
      tryCatchFinally.getCatchBlock().accept(this);
      decr();
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      whyMLcode.add(space() + "Finally");
      incr();
      tryCatchFinally.getFinallyBlock().accept(this);
      decr();
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    GoloFunction target = closureReference.getTarget();
    incr();
    if (target.isAnonymous()) {
      System.out.println("Closure unsupported");
      whyMLcode.add(space() + "Closure: ");
      incr();
      visitFunctionDefinition(target);
      decr();
    } else {
      // ReferenceTable table = context.referenceTableStack.peek();
      System.out.println("Closure reference unsupported");
      appendWhyMLLastString("Closure reference: " + target.getName() + ", regular arguments at index " +target.getSyntheticParameterCount());
      whyMLcode.add("");
        incr();
      for (String refName : closureReference.getCapturedReferenceNames()) {
        appendWhyMLLastString(space() + "- capture: " + refName);
      }
      decr();
    }
    decr();
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    incr();
    System.out.println("Loop break unsupported");
    whyMLcode.add(space() + "Loop break flow: " + loopBreakFlowStatement.getType().name());
    decr();
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    incr();
    System.out.println("Collection literal unsupported");
    whyMLcode.add(space() + "Collection literal of type: " + collectionLiteral.getType());
    for (ExpressionStatement statement : collectionLiteral.getExpressions()) {
      statement.accept(this);
    }
    decr();
  }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collectionComprehension) {
    incr();
    System.out.println("Collection comprehension unsupported");
    whyMLcode.add(space() + "Collection comprehension of type: " + collectionComprehension.getType());
    incr();
    whyMLcode.add(space() + "Expression: ");
    collectionComprehension.getExpression().accept(this);
    whyMLcode.add(space() + "Comprehension: ");
    for (Block b : collectionComprehension.getLoopBlocks()) {
      b.accept(this);
    }
    decr();
    decr();
  }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) {
    incr();
    System.out.println("Named argument unsupported");
    whyMLcode.add(space() + "Named argument: " + namedArgument.getName());
    namedArgument.getExpression().accept(this);
    decr();
  }



  @Override
  public void visitModuleImport(ModuleImport moduleImport) {
    incr();
    System.out.println("Module Import unsupported");
    whyMLcode.add(space() + " - " + moduleImport);
    moduleImport.walk(this);
    decr();
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation namedAugmentation) {
    incr();
    System.out.println("Named Augmentation unsupported");
    whyMLcode.add(space() + "Named Augmentation " + namedAugmentation.getName());
    namedAugmentation.walk(this);
    decr();
  }

  @Override
  public void visitAugmentation(Augmentation augmentation) {
    incr();
    System.out.println("Augmentation unsupported");
    whyMLcode.add(space() + "Augmentation on " + augmentation.getTarget());
    if (augmentation.hasNames()) {
      incr();
      for (String name : augmentation.getNames()) {
        System.out.println("Named Augmentation unsupported");
        whyMLcode.add(space() + "Named Augmentation " + name);
      }
      decr();
    }
    augmentation.walk(this);
    decr();
  }

  @Override
  public void visitStruct(Struct struct) {
    incr();
    System.out.println("Struct unsupported");
    whyMLcode.add(space() + "Struct " + struct.getPackageAndClass().className());
    whyMLcode.add(space() + " - target class = " + struct.getPackageAndClass());
    whyMLcode.add(space() + " - members = " + struct.getMembers());
    struct.walk(this);
    decr();
  }

  @Override
  public void visitUnion(Union union) {
    incr();
    System.out.println("Union unsupported");
    whyMLcode.add(space() + "Union " + union.getPackageAndClass().className());
    whyMLcode.add(space() + " - target class = " + union.getPackageAndClass());
    union.walk(this);
    decr();
  }

  @Override
  public void visitUnionValue(UnionValue value) {
    incr();
    System.out.println("Union Value unsupported");
    whyMLcode.add(space() + "Value " + value.getPackageAndClass().className());
    whyMLcode.add(space() + " - target class = " + value.getPackageAndClass());
    if (value.hasMembers()) {
      whyMLcode.add(space() + " - members = " + value.getMembers());
    }
    decr();
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    incr();
    System.out.println("Decorator unsupported");
    whyMLcode.add(space() + "@Decorator");
    decorator.getExpressionStatement().accept(this);
    decr();
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment assignment) {
    incr();
    System.out.println("Destructuring assignement unsupported");
    whyMLcode.add(space() + "Destructuring assignement: {declaring="+assignment.isDeclaring()+", varargs="+assignment.isVarargs()+"}");
    incr();
    for (LocalReference ref : assignment.getReferences()) {
      whyMLcode.add(space() + "- " + ref);
    }
    decr();
    assignment.getExpression().accept(this);
    decr();
  }


}
