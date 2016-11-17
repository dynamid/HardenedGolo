package org.eclipse.golo.compiler.jgoloparser;

import java.util.Set;

interface JGVariableContainer {

  void substitute(JGTerm term, JGTerm forVar);

  Set<JGTerm> freeVars();
}
