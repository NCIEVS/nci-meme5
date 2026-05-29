/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Verifies the ClaML admin loader and unload path.
 */
@Ignore("ClaML fixture currently exceeds smoke-test runtime/memory budget")
public class ClaMLLoadAndUnloadIT extends AdminLoaderIntegrationSupport {

  /**
   * Loads and removes the bundled ICD10CM ClaML fixture.
   *
   * @throws Exception if the load or unload fails
   */
  @Test
  public void testClamlLoadAndUnload() throws Exception {
    loadClaml("ICD10CM", "2016", dataPath("icd10cm-2016.xml"));

    assertConceptsLoaded("ICD10CM", "2016");
    assertReleaseInfoExists("ICD10CM", "2016");

    removeTerminology("ICD10CM", "2016");
    assertNoConcepts("ICD10CM", "2016");
  }
}
