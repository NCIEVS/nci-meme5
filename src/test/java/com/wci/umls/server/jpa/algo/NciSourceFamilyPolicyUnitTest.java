package com.wci.umls.server.jpa.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/** Database-free regression checks for NCI's CCS source family grouping. */
public class NciSourceFamilyPolicyUnitTest {

  @Test
  public void currentNamesUseTheCurrentSharedFamilyOnEveryLoad() {
    for (String source : new String[] {"CCSR_ICD10PCS", "CCSR_ICD10CM"}) {
      for (String incoming : new String[] {"CCS", "CCS_10", "CCSR_10",
          "CCSR_ICD10CM", "CCSR_ICD10PCS", "", null}) {
        final String family =
            NciSourceFamilyPolicy.resolve("NCIMTH", source, incoming);

        assertEquals("CCSR_ICD10PCS", family);
        assertEquals(family,
            NciSourceFamilyPolicy.resolve("NCIMTH", source, family));
      }
    }
  }

  @Test
  public void legacyNamesRetainTheHistoricalAdHocGrouping() {
    for (String source : new String[] {"CCS_10", "CCSR_10"}) {
      assertEquals("CCS_10",
          NciSourceFamilyPolicy.resolve("NCIMTH", source, "CCS"));
    }
  }

  @Test
  public void otherProjectsKeepIncomingMetadataForBothSourceGenerations() {
    for (String project : new String[] {"MTH", "NCI", "CCSR_ICD10CM", "", null}) {
      for (String source : new String[] {"CCS_10", "CCSR_10", "CCSR_ICD10PCS",
          "CCSR_ICD10CM"}) {
        assertEquals("CCS",
            NciSourceFamilyPolicy.resolve(project, source, "CCS"));
      }
    }
  }

  @Test
  public void unrelatedSourcesAndVersionedNamesAreNotMatchedByPrefix() {
    for (String source : new String[] {"CCS", "CCS_OTHER", "CCSR_ICD10CM_EXT",
        "CCSR_ICD10CM_2026", "SNOMEDCT_US", "NCIMTH", "", null}) {
      for (String family : new String[] {"CCS", "SNOMED", "NCI", "", null}) {
        assertEquals(family,
            NciSourceFamilyPolicy.resolve("NCIMTH", source, family));
      }
    }
  }

  @Test
  public void augustReleaseSourcesReferenceAnEmittedRootAfterNormalization() {
    // VSAB|RSAB|SF from the two failing 202608 MRSAB rows.
    final String[] rows = {
        "CCSR_ICD10PCS_2026|CCSR_ICD10PCS|CCS",
        "CCSR_ICD10CM_2026|CCSR_ICD10CM|CCS"};
    final Set<String> roots = new HashSet<>();
    for (String row : rows) {
      roots.add(row.split("\\|")[1]);
    }

    // This rejects both the incoming CCS family and the retired CCS_10 target.
    final Set<String> families = new HashSet<>();
    for (String row : rows) {
      final String[] fields = row.split("\\|");
      final String family =
          NciSourceFamilyPolicy.resolve("NCIMTH", fields[1], fields[2]);
      assertTrue("Family must be an RSAB in this release: " + family,
          roots.contains(family));
      families.add(family);
    }
    assertEquals("Both sources must remain in one family", 1, families.size());
  }
}
