package org.eclipse.golo.compiler.jgoloparser.visitor;

import org.eclipse.golo.compiler.ir.GoloFunction;
import org.eclipse.golo.compiler.jgoloparser.JGSpec;
import org.eclipse.golo.compiler.jgoloparser.JGSpecs;
import org.eclipse.golo.compiler.jgoloparser.JGTerm;
import org.eclipse.golo.compiler.jgoloparser.JGVariableContainer;
import org.eclipse.golo.compiler.jgoloparser.binary.*;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGExistential;
import org.eclipse.golo.compiler.jgoloparser.quantify.JGUniversal;
import org.eclipse.golo.compiler.jgoloparser.unary.JGMinus;
import org.eclipse.golo.compiler.jgoloparser.unary.JGNegated;

import java.util.Map;

public interface SpecTreeVisitor {

  void visit(JGAdditive node);

  void visit(JGCompare node);

  void visit(JGConjunctive node);

  void visit(JGDisjunctive node);

  void visit(JGImplicative node);

  void visit(JGMultiplicative node);

  void visit(JGExistential node);

  void visit(JGUniversal node);

  void visit(JGMinus node);

  void visit(JGNegated node);

  void visit(JGTerm node);

  void visit(JGSpecs specification);

  void visit(JGSpec specification);

  void verify(GoloFunction function);
}
