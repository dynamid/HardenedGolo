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

public class JGTerm implements JGVariableContainer {
  private String name = "";
  private boolean function;
  private ArrayList<JGTerm> innerTerms = null;

  public JGTerm(String name) {
    this.name = name;
    this.function=false;
    this.innerTerms = new ArrayList<JGTerm>();
  }

  public JGTerm(String name, ArrayList<JGTerm> innerTerms) {
    this.name = name;
    if (innerTerms != null) {
      this.function = true;
      this.innerTerms = innerTerms;

    } else {
      this.function=false;
    }
  }

  public String getName() {
      return name;
  }

  public void setName(String name) {
      this.name = name;
  }

  public ArrayList<JGTerm> getInnerTerms() {
      return innerTerms;
  }

  public void setInnerTerms(ArrayList<JGTerm> innerTerms) {
      this.innerTerms = innerTerms;
  }

  public int arity() {
    if(innerTerms==null) return 0;
    return innerTerms.size();
  }

  public boolean isConstant() {
      return arity() == 0 &&
              Character.isLowerCase(name.charAt(0));
  }

  public boolean isVariable() {
      return arity() == 0 &&
              Character.isUpperCase(name.charAt(0));
  }


  public boolean isFunction() {
    return function;
  }

  @Override
  public String toString() {
    if(isFunction()) {
      StringBuilder result = new StringBuilder();
      result.append("( ");
      result.append(name.toString());
      result.append(" ");

      int i;
      for (i = 0; i < innerTerms.size(); i++) {
        result.append(innerTerms.get(i).toString());
        result.append(" ");
      }

      result.append(")");
      return result.toString();

    } else {
      return name.toString();
    }
  }

  @Override
  public void substitute(JGTerm term, JGTerm forVar) {
      if (isVariable() &&
              this.equals(forVar)) {
          setName(term.getName());
          setInnerTerms(term.getInnerTerms());
      }

      for (int i = 0; i < innerTerms.size(); i++) {
          JGTerm innerTerm = innerTerms.get(i);

          if (!innerTerm.isVariable()) {
              innerTerm.substitute(term, forVar);
          } else if (innerTerm.equals(forVar)) {
              innerTerms.set(i, term);
          }
      }
  }

  @Override
  public Set<JGTerm> freeVars() {
      HashSet<JGTerm> freeVars = new HashSet<JGTerm>();

      for (JGTerm innerTerm : innerTerms) {
          if (innerTerm.isVariable()) {
              freeVars.add(innerTerm);
          } else {
              freeVars.addAll(innerTerm.freeVars());
          }
      }

      return freeVars;
  }

  @Override
  public int hashCode() {
      return toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
      if (!(obj instanceof JGTerm)) {
          return false;
      } else {
          return toString().equals(((JGTerm) obj).toString());
      }
  }
}
