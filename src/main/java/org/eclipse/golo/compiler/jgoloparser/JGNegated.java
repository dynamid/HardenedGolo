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

public class JGNegated implements JGFormula {
    private JGFormula subformula = null;

    public JGFormula getSubformula() {
        return subformula;
    }

    public void setSubformula(JGFormula subformula) {
        this.subformula = subformula;
    }

    public JGNegated(JGFormula subformula) {
        this.subformula = subformula;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
        subformula.substitute(term, forVar);
    }

    @Override
    public Set<JGTerm> freeVars() {
        return subformula.freeVars();
    }

    @Override
    public String toString() {
        return "!" + subformula.toString();
    }
}
