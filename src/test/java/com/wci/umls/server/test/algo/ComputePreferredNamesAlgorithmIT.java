/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.test.algo;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.LogEntry;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.jpa.algo.release.ComputePreferredNamesAlgorithm;
import com.wci.umls.server.jpa.model.ProcessExecutionJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.model.content.Concept;

/**
 * Integration testing for {@link ComputePreferredNamesAlgorithm}.
 */
public class ComputePreferredNamesAlgorithmIT
    extends AlgoIntegrationTestSupport {

  /** The algo. */
  private ComputePreferredNamesAlgorithm algo;

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    algo = new ComputePreferredNamesAlgorithm();
    algo.setProject(getProject());
    algo.setTerminology(getProject().getTerminology());
    algo.setVersion(getProject().getVersion());
    algo.setLastModifiedBy("admin");
    // algo.compute() calls getProcess().getTerminology() — set a dummy execution
    final ProcessExecutionJpa processExecution = new ProcessExecutionJpa();
    processExecution.setProject(getProject());
    processExecution.setTerminology(getProject().getTerminology());
    processExecution.setVersion(getProject().getVersion());
    algo.setProcess(processExecution);
  }

  /**
   * Test check preconditions.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCheckPreconditions() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    algo.checkPreconditions();
    // expect no failure
  }

  /**
   * Test reset.
   *
   * @throws Exception the exception
   */
  @Test
  public void testReset() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    algo.reset();
    // expect no failure
  }

  /**
   * Test compute.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCompute() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    // Break one concept preferred name
    final PfsParameter pfs = new PfsParameterJpa();
    pfs.setStartIndex(0);
    pfs.setMaxResults(1);
    final SearchResultList list =
        getLogService().findConceptSearchResults(getProject().getTerminology(),
            getProject().getVersion(), Branch.ROOT, null, pfs);
    final Concept concept =
        getLogService().getConcept(list.getObjects().get(0).getId());
    concept.setName("xyz");
    getLogService().updateConcept(concept);

    final String workId = compute(algo);

    // Get the log from the algorithm and analyze for changes
    List<LogEntry> entries =
        getLogService().findLogEntries("workId:" + workId, null);
    boolean found = false;
    for (final LogEntry entry : entries) {
      // Accept any non-zero update count: on a freshly-loaded DB all preferred
      // names may already be stale, so more than 1 concept may be updated.
      final String msg = entry.getMessage();
      if (msg.contains("concepts updated =")
          && !msg.contains("concepts updated = 0")) {
        found = true;
      }
    }
    assertTrue("Integration test failed to update any concept preferred names",
        found);
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    algo.close();
  }
}
