/* Copyright 2016 INSA Lyon
 * Inspired from "FirstOrderParser" by Dominic Scheurer.
 *
 * HardenedGolo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FirstOrderParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FirstOrderParser.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclipse.golo.compiler.jgoloparser.quantify;

import org.eclipse.golo.compiler.jgoloparser.JGFormula;
import org.eclipse.golo.compiler.jgoloparser.JGTerm;

import java.util.HashSet;
import java.util.Set;

abstract class JGQuantifier implements JGFormula {

  private Quantifier quantifier;

  private JGTerm typeQuantifier;

  protected JGTerm quantifiedBy;

  protected JGFormula formula;

  JGQuantifier(Quantifier quantifier, JGTerm typeQuantifier, JGTerm quantifiedBy, JGFormula formula) {
    this.quantifiedBy = quantifiedBy;
    this.quantifier = quantifier;
    this.formula = formula;
    this.typeQuantifier = typeQuantifier;
  }

  @Override
  public void substitute(JGTerm term, JGTerm forVar) {
    if (!forVar.equals(quantifiedBy)) {
      formula.substitute(term, forVar);
    }
  }

  @Override
  public Set<JGTerm> freeVars() {
    Set<JGTerm> freeVars = new HashSet<>(formula.freeVars());
    freeVars.remove(quantifiedBy);
    return freeVars;
  }

  @Override
  public Type getType() {
    return Type.BOOLEAN;
  }

  public JGFormula getFormula() {
    return formula;
  }

  public JGTerm getTypeQuantifier() {
    return typeQuantifier;
  }

  @Override
  public String toString() {
    return quantifier + " " + quantifiedBy + ":" + typeQuantifier + ". ( " + formula + " )";
  }

  enum Quantifier {
    EXISTS,
    FORALL;

    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }
}
