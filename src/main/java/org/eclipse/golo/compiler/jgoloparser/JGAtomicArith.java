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

public class JGAtomicArith implements JGFormula {
    private String name = null;
    private ArrayList<JGTerm> params = null;


    public JGAtomicArith(String name) {
        this.name = name;
        this.params = new ArrayList<JGTerm>();
    }

    public JGAtomicArith(JGLiteral lit) {
      this.name = lit.toString();
      this.params = new ArrayList<JGTerm>();
    }



  public JGAtomicArith(String name, ArrayList<JGTerm> terms) {
        this.name = name;
        this.params = terms;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<JGTerm> getParams() {
        return params;
    }
    public void setParams(ArrayList<JGTerm> terms) {
        this.params = terms;
    }

    public int arity() {
        if(params==null) return 0;
        return params.size();
    }

    public boolean isPropositional() {
        return arity() == 0;
    }

    @Override
    public void substitute(JGTerm term, JGTerm forVar) {
        for (int i = 0; i < arity(); i++) {
          params.get(i).substitute(term, forVar);
        }
    }

    @Override
    public Set<JGTerm> freeVars() {
        HashSet<JGTerm> freeVars = new HashSet<JGTerm>();
        for (JGTerm term : params) {
            if (term.isVariable()) {
                freeVars.add(term);
            }
        }
        return freeVars;
    }


    @Override
    public String toString() {
        if (!isPropositional()) {
          StringBuilder result = new StringBuilder();
          result.append("( ");
          result.append(name);
          result.append(" ");

          for (int i = 0; i < arity(); i++) {
              result.append(params.get(i).toString());
              result.append(" ");
          }

          result.append(")");
          return result.toString();


        } else {
          return name;
        }

    }
}
