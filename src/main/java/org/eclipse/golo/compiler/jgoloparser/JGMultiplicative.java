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

package org.eclipse.golo.compiler.jgoloparser;

import java.util.Set;

public class JGMultiplicative implements JGFormula {
    private JGFormula innerA = null;
    private JGFormula innerB = null;
    private String symbol;  // TODO : Should better be an enumeration ?

    public JGMultiplicative(JGFormula innerA, JGFormula innerB, String symbol) {
      this.innerA = innerA;
      this.innerB = innerB;
      this.symbol=symbol;
    }

    public JGFormula getSubInnerA() {
        return innerA;
    }
    public JGFormula getSubInnerB() {
    return innerB;
  }
    public String getOperator() {
    return symbol;
  }
    public void setSubInnerA(JGFormula innerA) {
        this.innerA = innerA;
    }
    public void setSubInnerB(JGFormula innerB) {
        this.innerB = innerB;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
      innerA.substitute(term, forVar);
      innerB.substitute(term, forVar);
    }

    @Override
    public Set<JGTerm> freeVars() {
      Set<JGTerm> freeVars = innerA.freeVars();
      freeVars.addAll(innerB.freeVars());

      return freeVars;
    }

    @Override
    public String toString() { return "(" + innerA.toString() + " "+symbol+" " + innerB.toString() + ")"; }
}
