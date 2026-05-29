/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import org.junit.Test;

/**
 * Verifies the RRF single-terminology admin loader and unload path.
 */
public class RrfSingleLoadAndUnloadIT
    extends AdminLoaderIntegrationSupport {

  /**
   * Loads and removes the bundled SNOMEDCT_US RRF fixture.
   *
   * @throws Exception if the load or unload fails
   */
  @Test
  public void testRrfSingleLoadAndUnload() throws Exception {
    loadRrfSingle("SNOMEDCT_US", "latest",
        rrfFixturePath("SCTMSH_2014AB"));

    assertConceptsLoaded("SNOMEDCT_US", "latest");

    removeTerminology("SNOMEDCT_US", "latest");
    assertNoConcepts("SNOMEDCT_US", "latest");
  }
}
