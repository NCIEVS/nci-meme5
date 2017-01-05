/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.util.Properties;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

/**
 * Algorithm to prepare MetamorphoSys.
 */
public class PrepareMetamorphoSysAlgorithm extends AbstractAlgorithm {

  /**
   * Instantiates an empty {@link PrepareMetamorphoSysAlgorithm}.
   *
   * @throws Exception the exception
   */
  public PrepareMetamorphoSysAlgorithm() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    // Send an email
    final Properties p = ConfigUtility.getConfigProperties();
    ConfigUtility.sendEmail(
        "Prepare MetamorphoSys for " + getProcess().getTerminology()
            + " Release " + getProcess().getVersion(),
        p.getProperty("mail,smtp.from"), p.getProperty("mail.smtp.to"),
        "Prepare MetamorphoSys for release. \n\nPut mmsys.zip file into "
            + p.getProperty("source.data.dir") + "/"
            + getProcess().getInputPath() + "/mr/mmsys.zip",
        p);
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        ""
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}
