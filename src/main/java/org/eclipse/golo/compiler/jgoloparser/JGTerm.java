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

import org.eclipse.golo.compiler.jgoloparser.visitor.SpecTreeVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class JGTerm implements JGFormula {

  private String name;

  private boolean isFunction;

  private ArrayList<JGFormula> innerTerms;

  private Type type = Type.OTHER;

  public JGTerm(String name, ArrayList<JGFormula> innerTerms) {
    this.name = name;
    if (innerTerms != null && innerTerms.size() > 0) {
      this.innerTerms = innerTerms;
      isFunction = true;
    }
  }

  public JGTerm(JGLiteral lit) {
    this(lit.toString());
    Object value = lit.getValue();
    if (Number.class.isInstance(value)) {
      type = Type.NUMERIC;
    } else if (Boolean.class.isInstance(value)) {
      type = Type.BOOLEAN;
    }
  }

  public JGTerm(String name) {
    this.name = name;
    innerTerms = new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private ArrayList<JGFormula> getInnerTerms() {
    return innerTerms;
  }

  private void setInnerTerms(ArrayList<JGFormula> innerTerms) {
    this.innerTerms = innerTerms;
    isFunction = true;
  }

  public int arity() {
    return !isFunction ? 0 : innerTerms.size();
  }

  public boolean isConstant() {
    return !isFunction && Character.isUpperCase(name.charAt(0));
  }

  public boolean isVariable() {
    return !isFunction && Character.isLowerCase(name.charAt(0));
  }

  public boolean isFunction() {
    return isFunction;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    if (isFunction()) {
      StringBuilder result = new StringBuilder();
      result.append("( ");
      result.append(name);
      result.append(" ");

      for (JGFormula innerTerm : innerTerms) {
        result.append(innerTerm);
        result.append(" ");
      }
      result.append(")");
      return result.toString();
    } else {
      return name;
    }
  }

  @Override
  public void substitute(JGTerm term, JGTerm forVar) {
    if (isVariable() && equals(forVar)) {
      setName(term.getName());
      setInnerTerms(term.getInnerTerms());
    }

    for (int i = 0; i < innerTerms.size(); i++) {
      JGFormula innerForm = innerTerms.get(i);
      if (innerForm instanceof JGTerm) {
        JGTerm innerTerm = (JGTerm) innerForm;
        if (!innerTerm.isVariable()) {
          innerTerm.substitute(term, forVar);
        } else if (innerTerm.equals(forVar)) {
          innerTerms.set(i, term);
        }
      } else {
        innerForm.substitute(term, forVar);
      }
    }
  }

  @Override
  public Set<JGTerm> freeVars() {
    HashSet<JGTerm> freeVars = new HashSet<>();
    for (JGFormula innerForm : innerTerms) {
      if (innerForm instanceof JGTerm) {
        JGTerm innerTerm = (JGTerm)innerForm;
        if (innerTerm.isVariable()) {
          freeVars.add(innerTerm);
        } else {
          freeVars.addAll(innerTerm.freeVars());
        }
      } else {
        freeVars.addAll(innerForm.freeVars());
      }
    }
    return freeVars;
  }

  @Override
  public void accept(SpecTreeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    // FIXME check all possible variants
    if (isFunction && obj instanceof JGTerm) {
      JGTerm another = JGTerm.class.cast(obj);
      return another.isFunction && type == another.type && name.equals(another.name) && arity() == another.arity();
    }
    return obj instanceof JGTerm && toString().equals(obj.toString());
  }
}
