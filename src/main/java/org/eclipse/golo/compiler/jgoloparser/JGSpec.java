package org.eclipse.golo.compiler.jgoloparser;

/**
 * Created by nstouls on 22/08/2016.
 */
public class JGSpec {
  private JGFormula formula;
  private String specType;
  public  JGSpec(String specType, JGFormula form) {
    this.formula=form;
    this.specType=specType;
  }
  public JGFormula getFormula(){return formula;}
  public String getSpecType(){return specType;}

  @Override
  public String toString() {
    return specType+" { "+formula.toString()+" } ";
  }
}
