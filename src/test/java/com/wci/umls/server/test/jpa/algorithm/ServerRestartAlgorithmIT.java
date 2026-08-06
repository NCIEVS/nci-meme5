/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.algorithm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.ServerRestartException;
import com.wci.umls.server.jpa.algo.maint.ServerRestartAlgorithm;
import com.wci.umls.server.jpa.model.ProcessExecutionJpa;
import com.wci.umls.server.jpa.services.ProcessServiceJpa;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Tests the Server Restart algorithm resume marker behavior.
 */
public class ServerRestartAlgorithmIT extends IntegrationUnitSupport {

  /** The algorithm. */
  ServerRestartAlgorithm algo = null;

  /** The process service. */
  ProcessServiceJpa processService = null;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    processService = new ProcessServiceJpa();
    algo =
        (ServerRestartAlgorithm) processService.getAlgorithmInstance(
            ServerRestartAlgorithm.ALGORITHM_KEY);

    final ProcessExecutionJpa processExecution = new ProcessExecutionJpa();
    processExecution.setId(1L);
    processExecution.setExecutionInfo(new HashMap<>());

    algo.setLastModifiedBy("admin");
    algo.setLastModifiedFlag(true);
    algo.setProject(algo.getProjects().getObjects().get(0));
    algo.setProcess(processExecution);
    algo.setTerminology("MTH");
    algo.setVersion("latest");
    algo.setTransactionPerOperation(false);
    algo.beginTransaction();
  }

  /**
   * Test server restart request and resume behavior.
   *
   * @throws Exception the exception
   */
  @Test
  public void testServerRestartRequestAndResume() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    try {
      algo.compute();
      assertTrue(false);
    } catch (ServerRestartException e) {
      assertTrue(e.getMessage().contains("Server Restart requested"));
    }

    ServerRestartAlgorithm.markRestartRequest(algo.getProcess().getExecutionInfo(),
        algo.getActivityId(), 1L);

    algo.compute();
    assertFalse(algo.getProcess().getExecutionInfo()
        .containsKey(ServerRestartAlgorithm.RESTART_PENDING_KEY));
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    if (algo != null) {
      algo.close();
    }
    if (processService != null) {
      processService.close();
    }
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }
}
