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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class JGAtomic implements JGFormula {
    private JGPredicate predicate = null;
    private ArrayList<JGTerm> terms = null;

    public JGAtomic(JGPredicate predicate) {
        this.predicate = predicate;
        this.terms = new ArrayList<JGTerm>();
    }

    public JGAtomic(JGPredicate predicate, ArrayList<JGTerm> terms) {
        this.predicate = predicate;
        this.terms = terms;
    }

    public JGPredicate getPredicate() {
        return predicate;
    }

    public void setPredicate(JGPredicate predicate) {
        this.predicate = predicate;
    }

    public ArrayList<JGTerm> getTerms() {
        return terms;
    }

    public void setTerms(ArrayList<JGTerm> terms) {
        this.terms = terms;
    }

    public int arity() {
        return terms.size();
    }

    public boolean isPropositional() {
        return arity() == 0;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
        for (int i = 0; i < terms.size(); i++) {
            terms.get(i).substitute(term, forVar);
        }
    }

    @Override
    public Set<JGTerm> freeVars() {
        HashSet<JGTerm> freeVars = new HashSet<JGTerm>();
        for (JGTerm term : terms) {
            if (term.isVariable()) {
                freeVars.add(term);
            }
        }
        return freeVars;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(predicate.toString());

        if (!isPropositional()) {
            result.append("(");

            int i;
            for (i = 0; i < terms.size() - 1; i++) {
                result.append(terms.get(i).toString());
                result.append(",");
            }
            result.append(terms.get(i).toString());

            result.append(")");
        }

        return result.toString();
    }
}
