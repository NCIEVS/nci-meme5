package com.wci.umls.server.jpa.algo;

/** Applies the NCI CCS family grouping when inserting source metadata. */
public final class NciSourceFamilyPolicy {

  private NciSourceFamilyPolicy() {
    // Utility class.
  }

  /**
   * Resolves the family using the root source name and the owning project.
   *
   * @param projectTerminology the owning project's terminology
   * @param sourceTerminology the unversioned source abbreviation
   * @param incomingFamily the family supplied in sources.src
   * @return the NCI family override, or the incoming family for other sources
   */
  public static String resolve(String projectTerminology,
    String sourceTerminology, String incomingFamily) {
    if (!"NCIMTH".equals(projectTerminology)) {
      return incomingFamily;
    }

    // Preserve the historical AdHoc grouping under the pre-2021 names.
    if ("CCS_10".equals(sourceTerminology)
        || "CCSR_10".equals(sourceTerminology)) {
      return "CCS_10";
    }

    // In 2021 CCS_10 became CCSR_ICD10PCS; CCSR_10 became CCSR_ICD10CM.
    // Keep the shared family, using its current root source abbreviation.
    if ("CCSR_ICD10PCS".equals(sourceTerminology)
        || "CCSR_ICD10CM".equals(sourceTerminology)) {
      return "CCSR_ICD10PCS";
    }
    return incomingFamily;
  }
}
