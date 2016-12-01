package org.eclipse.golo.compiler.jgoloparser.unary;

import org.eclipse.golo.compiler.jgoloparser.JGFormula;
import org.eclipse.golo.compiler.jgoloparser.JGTerm;

import java.util.Set;

abstract class JGUnary implements JGFormula {

  private JGFormula formula;

  private Operator operator;

  JGUnary(JGFormula formula, Operator operator) {
    this.formula = formula;
    this.operator = operator;
  }

  @Override
  public void substitute(JGTerm term, JGTerm forVar) {
    formula.substitute(term, forVar);
  }

  @Override
  public Set<JGTerm> freeVars() {
    return formula.freeVars();
  }

  public JGFormula getFormula() {
    return formula;
  }

  @Override
  public String toString() {
    return operator + formula.toString();
  }

  enum Operator {
    MINUS {
      @Override
      public String toString() {
        return "-";
      }
    },
    NEGATIVE {
      @Override
      public String toString() {
        return "!";
      }
    }
  }
}
