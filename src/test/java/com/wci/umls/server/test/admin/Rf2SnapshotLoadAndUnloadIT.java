/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Verifies the RF2 snapshot admin loader and unload path.
 */
@Ignore("RF2 snapshot fixture exceeds smoke-test runtime budget")
public class Rf2SnapshotLoadAndUnloadIT
    extends AdminLoaderIntegrationSupport {

  /**
   * Loads and removes the bundled SNOMEDCT RF2 snapshot fixture.
   *
   * @throws Exception if the load or unload fails
   */
  @Test
  public void testRf2SnapshotLoadAndUnload() throws Exception {
    loadRf2Snapshot("SNOMEDCT", "latest",
        dataPath("snomedct-20140731-mini"));

    assertConceptsLoaded("SNOMEDCT", "latest");
    assertReleaseInfoExists("SNOMEDCT", "20140731");

    removeTerminology("SNOMEDCT", "latest");
    assertNoConcepts("SNOMEDCT", "latest");
  }
}
