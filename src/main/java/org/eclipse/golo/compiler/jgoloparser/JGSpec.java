package org.eclipse.golo.compiler.jgoloparser;

public class JGSpec {

  private JGFormula formula;

  private String specType;

  public  JGSpec(String specType, JGFormula form) {
    this.formula = form;
    this.specType = specType;
  }

  @Override
  public String toString() {
    return specType + " { " + formula + " } ";
  }
}
