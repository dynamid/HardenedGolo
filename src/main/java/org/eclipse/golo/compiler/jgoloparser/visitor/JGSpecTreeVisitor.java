package org.eclipse.golo.compiler.jgoloparser.visitor;

import org.eclipse.golo.compiler.ir.GoloFunction;
import org.eclipse.golo.compiler.ir.LocalReference;
import org.eclipse.golo.compiler.ir.ReferenceTable;
import org.eclipse.golo.compiler.jgoloparser.*;
import org.eclipse.golo.compiler.jgoloparser.binary.*;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGExistential;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGUniversal;
import org.eclipse.golo.compiler.jgoloparser.unary.JGMinus;
import org.eclipse.golo.compiler.jgoloparser.unary.JGNegated;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.golo.compiler.jgoloparser.JGVariableContainer.Type;

public class JGSpecTreeVisitor implements SpecTreeVisitor {

  private static final Set<String> WHYML_TYPES = new HashSet<>();

  static {
    WHYML_TYPES.add("char");
    WHYML_TYPES.add("byte");
    WHYML_TYPES.add("short");
    WHYML_TYPES.add("int");
    WHYML_TYPES.add("long");
    WHYML_TYPES.add("float");
    WHYML_TYPES.add("double");
  }

  private ReferenceTable referenceTable;

  @Override
  public void visit(JGSpecs node) {
    node.getSpecList().forEach(this::visit);
  }

  @Override
  public void visit(JGSpec node) {
    node.getFormula().accept(this);
  }

  @Override
  public void verify(GoloFunction function) {
    JGSpecs specs = function.getSpecification();
    if (specs != null) {
      referenceTable = function.getBlock().getReferenceTable();
      visit(specs);
    }
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
        LocalReference reference = referenceTable.get(((JGTerm) formula).getName());
        if (reference != null) {
          type = reference.getType();
          if (type == null) {
            reference.setType(Type.NUMERIC);
          } else if (type == Type.BOOLEAN) {
            throw new ParseException(" is already defined as boolean variable and can't use as boolean");
          }
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
        LocalReference reference = referenceTable.get(((JGTerm) formula).getName());
        if (reference != null) {
          type = reference.getType();
          if (type == null) {
            reference.setType(Type.BOOLEAN);
          } else if (type == Type.NUMERIC) {
            throw new ParseException(" is already defined as boolean and can't use as numeric");
          }
        }
      } else {
        throw new ParseException("isn't a boolean!");
      }
    }
  }

  @Override
  public void visit(JGExistential node) {
    try {
      if (!isAllowableWhyMLType(node.getTypeQuantifier())) {
        System.err.println("Unsupported type of quantifier!");
      }
      visitBoolean(node.getFormula());
    } catch (ParseException e) {
      System.err.println("Quantified term of " + node + " " + e.getMessage());
    }
  }

  @Override
  public void visit(JGUniversal node) {
    try {
      if (isAllowableWhyMLType(node.getTypeQuantifier())) {
        System.err.println("Unsupported type of quantifier!");
      }
      visitBoolean(node.getFormula());
    } catch (ParseException e) {
      System.err.println("Quantified term of " + node + " " + e.getMessage());
    }
  }

  private boolean isAllowableWhyMLType(JGTerm type) {
    return WHYML_TYPES.contains(type.toString());
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
  }
}
