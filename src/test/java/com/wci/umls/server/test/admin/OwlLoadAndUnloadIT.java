/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Verifies the OWL admin loader and unload path.
 */
@Ignore("OWL fixture is kept out of the smoke baseline until runtime is bounded")
public class OwlLoadAndUnloadIT extends AdminLoaderIntegrationSupport {

  /**
   * Loads and removes the bundled SNOMEDCT OWL fixture.
   *
   * @throws Exception if the load or unload fails
   */
  @Test
  public void testOwlLoadAndUnload() throws Exception {
    loadOwl("SNOMEDCT", "20150131", dataPath("snomed.owl"));

    assertConceptsLoaded("SNOMEDCT", "20150131");
    assertReleaseInfoExists("SNOMEDCT", "20150131");

    removeTerminology("SNOMEDCT", "20150131");
    assertNoConcepts("SNOMEDCT", "20150131");
  }
}
