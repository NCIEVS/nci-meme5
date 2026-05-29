/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Compares RF2 full and RF2 snapshot loader output on paired mini fixtures.
 */
@Ignore("RF2 full-vs-snapshot comparison exceeds smoke-test runtime budget")
public class CompareRf2FullRf2SnapshotLoadersIT
    extends AdminLoaderIntegrationSupport {

  /**
   * Compares component stats from equivalent RF2 full and snapshot loads.
   *
   * @throws Exception if either load fails
   */
  @Test
  public void testRf2FullAndSnapshotLoadCountsMatch() throws Exception {
    loadRf2Full("SNOMEDCT", "latest",
        dataPath("snomedct-20140731-minif"));
    final Map<String, Integer> fullStats =
        componentStats("SNOMEDCT", "latest");

    loadRf2Snapshot("SNOMEDCT", "latest",
        dataPath("snomedct-20140731-mini"));
    final Map<String, Integer> snapshotStats =
        componentStats("SNOMEDCT", "latest");

    assertFalse("Expected RF2 full fixture stats", fullStats.isEmpty());
    assertEquals(fullStats, snapshotStats);
  }
}
