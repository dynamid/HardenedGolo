package org.eclipse.golo.compiler.jgoloparser;

import java.util.Set;

/**
 * Created by nstouls on 22/08/2016.
 */
public interface JGVariableContainer {
  public void substitute(JGTerm term, JGTerm forVar);
  public Set<JGTerm> freeVars();
}
