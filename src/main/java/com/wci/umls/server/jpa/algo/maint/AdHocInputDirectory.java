package com.wci.umls.server.jpa.algo.maint;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.wci.umls.server.helpers.ConfigUtility;

/** Resolves Input Path only for AdHoc actions that use it for file I/O. */
final class AdHocInputDirectory {

  // Include both readers and writers. Release readers use Input Path as the
  // base for <version>/META; the overlap report writes beneath Input Path.
  // Add new file-dependent actions here when extending AdHocAlgorithm.
  private static final Set<String> FILE_ACTIONS = Set.of(
      "Fix Atom Last Release CUI",
      "Initialize Source Atom Id Range App",
      "Update Root Terminology Contact Info From UMLS",
      "Reload Workflow Bin Definition Queries",
      "Write Meddra Ncit Overlap Report");

  private AdHocInputDirectory() {
    // Utility class.
  }

  /** Returns the validated base directory, or null when the action needs none. */
  static File resolve(String actionName, String inputPath) throws IOException {
    if (actionName == null || !FILE_ACTIONS.contains(actionName)) {
      return null;
    }
    return ConfigUtility.resolveExistingSourceDataDirectory(
        "process input directory", inputPath);
  }
}
