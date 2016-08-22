/* Copyright 2014 Dominic Scheurer
 *
 * This file is part of FirstOrderParser.
 *
 * FirstOrderParser is free software: you can redistribute it and/or modify
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

public class JGDisjunctive implements JGFormula {
    private JGFormula subformulaA = null, subformulaB = null;

    public JGDisjunctive(JGFormula subformulaA, JGFormula subformulaB) {
        this.subformulaA = subformulaA;
        this.subformulaB = subformulaB;
    }

    public JGFormula getSubformulaA() {
        return subformulaA;
    }

    public void setSubformulaA(JGFormula subformulaA) {
        this.subformulaA = subformulaA;
    }

    public JGFormula getSubformulaB() {
        return subformulaB;
    }

    public void setSubformulaB(JGFormula subformulaB) {
        this.subformulaB = subformulaB;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
        subformulaA.substitute(term, forVar);
        subformulaB.substitute(term, forVar);
    }

    @Override
    public Set<JGTerm> freeVars() {
        Set<JGTerm> freeVars = subformulaA.freeVars();
        freeVars.addAll(subformulaB.freeVars());

        return freeVars;
    }

    @Override
    public String toString() {
        return "(" + subformulaA.toString() + " | " + subformulaB.toString() + ")";
    }
}
