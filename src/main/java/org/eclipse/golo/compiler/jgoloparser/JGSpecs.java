package org.eclipse.golo.compiler.jgoloparser;

import java.util.List;
import java.util.LinkedList;
/**
 * Created by nstouls on 22/08/2016.
 */
public class JGSpecs {

  private static final String NEW_LINE = System.getProperty("line.separator").toString();
  private List<JGSpec> specList;
  public  JGSpecs() {
    specList=new LinkedList<JGSpec>();
  }

  public void add(JGSpec spec) {
    specList.add(spec);
  }

  @Override
  public String toString() {
    StringBuilder res = new StringBuilder();
    for(JGSpec sp : specList) {
      res.append(NEW_LINE).append(sp.toString());
    }
    return res.toString();
  }
}
