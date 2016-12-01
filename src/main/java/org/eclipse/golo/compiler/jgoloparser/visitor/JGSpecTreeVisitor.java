package org.eclipse.golo.compiler.jgoloparser.visitor;

import org.eclipse.golo.compiler.jgoloparser.*;
import org.eclipse.golo.compiler.jgoloparser.binary.*;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGExistential;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGUniversal;
import org.eclipse.golo.compiler.jgoloparser.unary.JGMinus;
import org.eclipse.golo.compiler.jgoloparser.unary.JGNegated;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.golo.compiler.jgoloparser.JGVariableContainer.Type;

public class JGSpecTreeVisitor implements SpecTreeVisitor {

  private final Map<JGTerm, Type> variables = new HashMap<>();

  @Override
  public void visit(JGSpecs node) {
    node.getSpecList().forEach(this::visit);
  }

  @Override
  public void visit(JGSpec node) {
    node.getFormula().accept(this);
  }

  @Override
  public void visit(JGAdditive node) {
    visitNumeric(node);
  }

  @Override
  public void visit(JGCompare node) {
    visitNumeric(node);
  }

  @Override
  public void visit(JGMultiplicative node) {
    visitNumeric(node);
  }

  private void visitNumeric(JGBinary node) {
    try {
      visitNumeric(node.getLeft());
    } catch (ParseException e) {
      System.err.println("Left term of " + node + " " + e.getMessage());
    }
    try {
      visitNumeric(node.getRight());
    } catch (ParseException e) {
      System.err.println("Right term of " + node + " " + e.getMessage());
    }
  }

  private void visitNumeric(JGFormula formula) throws ParseException {
    formula.accept(this);
    Type type = formula.getType();
    if (type != Type.NUMERIC) {
      if (formula instanceof JGTerm && type == Type.OTHER) {
        type = variables.get(formula);
        if (type == null) {
          variables.put((JGTerm)formula, Type.NUMERIC);
        } else if (type == Type.BOOLEAN) {
          throw new ParseException(" is already defined as boolean variable and can't use as boolean");
        }
      } else {
        throw new ParseException("isn't a numeric!");
      }
    }
  }

  @Override
  public void visit(JGConjunctive node) {
    visitBoolean(node);
  }

  @Override
  public void visit(JGDisjunctive node) {
    visitBoolean(node);
  }

  @Override
  public void visit(JGImplicative node) {
    visitBoolean(node);
  }

  private void visitBoolean(JGBinary node) {
    try {
      visitBoolean(node.getLeft());
    } catch (ParseException e) {
      System.err.println("Left term of " + node + " " + e.getMessage());
    }
    try {
      visitBoolean(node.getRight());
    } catch (ParseException e) {
      System.err.println("Right term of " + node + " " + e.getMessage());
    }
  }

  private void visitBoolean(JGFormula formula) throws ParseException {
    formula.accept(this);
    Type type = formula.getType();
    if (type != JGVariableContainer.Type.BOOLEAN) {
      if (formula instanceof JGTerm && type == Type.OTHER) {
        type = variables.get(formula);
        if (type == null) {
          variables.put((JGTerm)formula, Type.BOOLEAN);
        } else if (type == Type.NUMERIC) {
          throw new ParseException(" is already defined as numeric and can't use as boolean");
        }
      } else {
        throw new ParseException("isn't a boolean!");
      }
    }
  }

  @Override
  public void visit(JGExistential node) {
    try {
      visitBoolean(node.getQuantifiedBy());
    } catch (ParseException e) {
      System.err.println("Quantified term of " + node + " " + e.getMessage());
    }
  }

  @Override
  public void visit(JGUniversal node) {
    try {
      visitBoolean(node.getQuantifiedBy());
    } catch (ParseException e) {
      System.err.println("Quantified term of " + node + " " + e.getMessage());
    }
  }

  @Override
  public void visit(JGMinus node) {
    try {
      visitNumeric(node.getFormula());
    } catch (ParseException e) {
      System.err.println(node + " " + e.getMessage());
    }
  }

  @Override
  public void visit(JGNegated node) {
    try {
      visitBoolean(node.getFormula());
    } catch (ParseException e) {
      System.err.println(node.getFormula() + " " + e.getMessage());
    }
  }

  @Override
  public void visit(JGTerm node) {
    if (node.getType() == Type.OTHER && !variables.containsKey(node)) {
      variables.put(node, null);
    }
  }
}
