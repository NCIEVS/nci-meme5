package com.wci.umls.server.jpa.algo.maint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;

/** Tests AdHoc input requirements without starting a database service. */
public class AdHocInputDirectoryUnitTest {

  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  private File sourceDirectory;

  private static final List<String> FILE_ACTIONS = List.of(
      "Fix Atom Last Release CUI",
      "Initialize Source Atom Id Range App",
      "Update Root Terminology Contact Info From UMLS",
      "Reload Workflow Bin Definition Queries",
      "Write Meddra Ncit Overlap Report");

  private static final List<String> DATABASE_ACTIONS = List.of(
      "Fix bequeathal rels to unpublishable",
      "Fix atom errors to unpublishable",
      "Fix Orphan Definitions",
      "Fix Duplicate Concepts",
      "Fix Workflow Bin Ranks",
      "Approve worklist cluster",
      "Cleanup corrupted process config",
      "Inactivate Old SRC atoms and AtomRels",
      RetiredSourceRootRepair.ACTION_NAME);

  @Before
  public void setUp() throws Exception {
    sourceDirectory = temporary.newFolder("data");
    Properties properties = new Properties();
    properties.setProperty(ConfigUtility.SOURCE_DATA_DIR_PROPERTY,
        sourceDirectory.getPath());
    PropertyUtility.setProperties(properties);
  }

  @After
  public void tearDown() {
    PropertyUtility.resetProperties();
  }

  @Test
  public void databaseActionsAcceptBlankInputWithoutAConfiguredSourceDirectory() throws Exception {
    PropertyUtility.setProperties(new Properties());
    for (String action : DATABASE_ACTIONS) {
      for (String input : new String[] {null, "", "   "}) {
        assertNull(action, AdHocInputDirectory.resolve(action, input));
      }
    }
  }

  @Test
  public void unusedInputPathsDoNotBlockDatabaseActions() throws Exception {
    for (String action : DATABASE_ACTIONS) {
      assertNull(action, AdHocInputDirectory.resolve(action, "old-missing-directory"));
      assertNull(action, AdHocInputDirectory.resolve(action, "../unused"));
    }
    assertFalse(new File(sourceDirectory, "old-missing-directory").exists());
  }

  @Test
  public void allFileActionsRejectBlankInput() throws Exception {
    for (String action : FILE_ACTIONS) {
      for (String input : new String[] {null, "", "   "}) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> AdHocInputDirectory.resolve(action, input));
        assertTrue(action, error.getMessage().contains("process input directory"));
      }
    }
  }

  @Test
  public void allFileActionsResolveTheirExistingBaseDirectory() throws Exception {
    File input = temporary.newFolder("data", "maintenance");
    for (String action : FILE_ACTIONS) {
      assertEquals(action, input.getCanonicalFile(),
          AdHocInputDirectory.resolve(action, "maintenance"));
    }
  }

  @Test
  public void fileActionsRejectMissingDirectoriesAndRegularFiles() throws Exception {
    temporary.newFile("data/input.txt");
    for (String action : FILE_ACTIONS) {
      assertThrows(IllegalArgumentException.class,
          () -> AdHocInputDirectory.resolve(action, "missing"));
      assertThrows(IllegalArgumentException.class,
          () -> AdHocInputDirectory.resolve(action, "input.txt"));
    }
    assertFalse(new File(sourceDirectory, "missing").exists());
  }

  @Test
  public void fileActionsStillEnforceSourceDirectoryBoundaries() throws Exception {
    File outside = temporary.newFolder("outside");
    for (String action : FILE_ACTIONS) {
      assertThrows(IllegalArgumentException.class,
          () -> AdHocInputDirectory.resolve(action, "../outside"));
      assertThrows(IllegalArgumentException.class,
          () -> AdHocInputDirectory.resolve(action, outside.getAbsolutePath()));
    }
  }

  @Test
  public void overlapReportRetainsItsConfiguredOutputBase() throws Exception {
    File input = temporary.newFolder("data", "reports");
    File resolved = AdHocInputDirectory.resolve("Write Meddra Ncit Overlap Report", "reports");
    assertEquals(new File(input, "maint/dsstmp.txt").getCanonicalFile(),
        new File(resolved, "maint/dsstmp.txt").getCanonicalFile());
  }
}
