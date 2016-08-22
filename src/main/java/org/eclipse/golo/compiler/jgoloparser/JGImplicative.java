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

public class JGImplicative implements JGFormula {
    private JGFormula premise = null, conclusion = null;

    public JGImplicative(JGFormula premise, JGFormula conclusion) {
        this.premise = premise;
        this.conclusion = conclusion;
    }

    public JGFormula getPremise() {
        return premise;
    }

    public void setPremise(JGFormula premise) {
        this.premise = premise;
    }

    public JGFormula getConclusion() {
        return conclusion;
    }

    public void setConclusion(JGFormula conclusion) {
        this.conclusion = conclusion;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
        premise.substitute(term, forVar);
        conclusion.substitute(term, forVar);
    }

    @Override
    public Set<JGTerm> freeVars() {
        Set<JGTerm> freeVars = premise.freeVars();
        freeVars.addAll(conclusion.freeVars());

        return freeVars;
    }

    @Override
    public String toString() {
        return "(" + premise.toString() + " -> " + conclusion.toString() + ")";
    }
}
