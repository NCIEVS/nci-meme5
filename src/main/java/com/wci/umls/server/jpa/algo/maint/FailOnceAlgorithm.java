/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;

/**
 * Implementation of an algorithm to fail the first time it's run, then succeed
 * when restarted. This will be used for testing purposes only
 */
public class FailOnceAlgorithm extends AbstractAlgorithm {

  /**
   * Tracks process execution IDs that have already failed once.
   * A given execution fails on first run and succeeds on restart.
   * Keyed by ProcessExecution ID so state resets naturally per execution.
   */
  private static final Set<Long> alreadyFailed = new HashSet<>();

  /**
   * Instantiates an empty {@link FailOnceAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public FailOnceAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("FAILONCE");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    if (getProject() == null) {
      throw new Exception("FailOnce initializer requires a project to be set");
    }
    // n/a - NO preconditions
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // If this process execution has not failed yet, fail it now.
    final Long executionId = getProcess().getId();
    if (!alreadyFailed.contains(executionId)) {
      alreadyFailed.add(executionId);
      throw new Exception("FAILONCE first run failed.");
    }

    // On restart of the same execution, succeed.
    else {
      fireProgressEvent(100, "FAILONCE progress: " + 100 + "%");
    }

    logInfo("Finished " + getName());

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - No reset
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception  {
    final List<AlgorithmParameter> params = super.getParameters();
    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Algorithm for testing fail and restart.";
  }
}
