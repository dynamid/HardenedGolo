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
package org.eclipse.golo.compiler.jgoloparser.binary;

import org.eclipse.golo.compiler.jgoloparser.JGFormula;
import org.eclipse.golo.compiler.jgoloparser.visitor.SpecTreeVisitor;

public class JGMultiplicative extends JGBinary {

  public JGMultiplicative(JGFormula left, JGFormula right, String operator) {
    super(left, Operator.parse(operator), right);
  }

  @Override
  public void accept(SpecTreeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Type getType() {
    return Type.NUMERIC;
  }
}
