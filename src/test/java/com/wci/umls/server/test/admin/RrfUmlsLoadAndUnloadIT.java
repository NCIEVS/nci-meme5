/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import org.junit.Test;

/**
 * Verifies the UMLS-style RRF admin loader and unload path.
 */
public class RrfUmlsLoadAndUnloadIT
    extends AdminLoaderIntegrationSupport {

  /**
   * Loads and removes bundled multi-source RRF fixture content.
   *
   * @throws Exception if the load or unload fails
   */
  @Test
  public void testRrfUmlsLoadAndUnload() throws Exception {
    loadRrfUmls("MTH", "latest", rrfFixturePath("SCTMSH_2014AB"));

    assertConceptsLoaded("MTH", "latest");
    assertConceptsLoaded("SNOMEDCT_US", "2014_09_01");
    assertReleaseInfoExists("MTH", "2014AB");

    removeTerminology("MTH", "latest");
    assertNoConcepts("MTH", "latest");
  }
}
