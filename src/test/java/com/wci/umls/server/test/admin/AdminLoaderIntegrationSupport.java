/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.wci.umls.server.admin.AbstractLoader;
import com.wci.umls.server.admin.ClamlLoader;
import com.wci.umls.server.admin.OwlLoader;
import com.wci.umls.server.admin.Rf2FullLoader;
import com.wci.umls.server.admin.Rf2SnapshotLoader;
import com.wci.umls.server.admin.RrfSingleLoader;
import com.wci.umls.server.admin.RrfUmlsLoader;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.HistoryServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.model.algo.ReleaseInfo;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.HistoryService;
import com.wci.umls.server.services.SecurityService;

/**
 * Shared helpers for admin loader integration tests.
 */
public abstract class AdminLoaderIntegrationSupport {

  /** Bundled test-data directory. */
  private static final File DATA_ROOT =
      new File("config/src/main/resources/data");

  /** Test operation that can throw checked exceptions. */
  @FunctionalInterface
  protected interface TestOperation {

    /**
     * Runs the operation.
     *
     * @throws Exception if operation fails
     */
    void run() throws Exception;
  }

  /**
   * Returns an absolute test-data path.
   *
   * @param path relative path under bundled test data
   * @return absolute path
   */
  protected String dataPath(final String path) {
    return new File(DATA_ROOT, path).getAbsolutePath();
  }

  /**
   * Returns a writable RRF fixture path with optional legacy files supplied.
   *
   * @param path source fixture path
   * @return writable fixture path
   * @throws IOException if fixture preparation fails
   */
  protected String rrfFixturePath(final String path) throws IOException {
    final Path source = new File(DATA_ROOT, path).toPath();
    final Path target =
        new File("build/tmp/admin-loader-fixtures", path).toPath();

    try (Stream<Path> paths = Files.walk(source)) {
      for (final Path sourcePath : (Iterable<Path>) paths::iterator) {
        final Path targetPath = target.resolve(source.relativize(sourcePath));
        if (Files.isDirectory(sourcePath)) {
          Files.createDirectories(targetPath);
        } else {
          final Path targetParent = targetPath.getParent();
          if (targetParent != null) {
            Files.createDirectories(targetParent);
          }
          Files.copy(sourcePath, targetPath,
              StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }

    final Path mrhier = target.resolve("MRHIER.RRF");
    if (!Files.exists(mrhier)) {
      Files.createFile(mrhier);
    }
    return target.toAbsolutePath().toString();
  }

  /**
   * Loads one RRF terminology.
   *
   * @param terminology terminology
   * @param version version
   * @param inputDir input directory
   * @throws Exception if load fails
   */
  protected void loadRrfSingle(final String terminology, final String version,
    final String inputDir) throws Exception {

    runLoader(new RrfSingleLoader(), properties(
        "terminology", terminology,
        "version", version,
        "prefix", "MR",
        "input.dir", inputDir,
        "server", "false",
        "mode", "create"));
  }

  /**
   * Loads UMLS-style RRF data.
   *
   * @param terminology terminology
   * @param version version
   * @param inputDir input directory
   * @throws Exception if load fails
   */
  protected void loadRrfUmls(final String terminology, final String version,
    final String inputDir) throws Exception {

    runLoader(new RrfUmlsLoader(), properties(
        "terminology", terminology,
        "version", version,
        "prefix", "MR",
        "input.dir", inputDir,
        "edit.mode", "false",
        "server", "false",
        "mode", "create"));
  }

  /**
   * Loads RF2 full data.
   *
   * @param terminology terminology
   * @param version version
   * @param inputDir input directory
   * @throws Exception if load fails
   */
  protected void loadRf2Full(final String terminology, final String version,
    final String inputDir) throws Exception {

    runLoader(new Rf2FullLoader(), properties(
        "terminology", terminology,
        "version", version,
        "input.dir", inputDir,
        "server", "false",
        "mode", "create"));
  }

  /**
   * Loads RF2 snapshot data.
   *
   * @param terminology terminology
   * @param version version
   * @param inputDir input directory
   * @throws Exception if load fails
   */
  protected void loadRf2Snapshot(final String terminology, final String version,
    final String inputDir) throws Exception {

    runLoader(new Rf2SnapshotLoader(), properties(
        "terminology", terminology,
        "version", version,
        "input.dir", inputDir,
        "server", "false",
        "mode", "create"));
  }

  /**
   * Loads ClaML data.
   *
   * @param terminology terminology
   * @param version version
   * @param inputFile input file
   * @throws Exception if load fails
   */
  protected void loadClaml(final String terminology, final String version,
    final String inputFile) throws Exception {

    runLoader(new ClamlLoader(), properties(
        "terminology", terminology,
        "version", version,
        "input.file", inputFile,
        "server", "false",
        "mode", "create"));
  }

  /**
   * Loads OWL data.
   *
   * @param terminology terminology
   * @param version version
   * @param inputFile input file
   * @throws Exception if load fails
   */
  protected void loadOwl(final String terminology, final String version,
    final String inputFile) throws Exception {

    runLoader(new OwlLoader(), properties(
        "terminology", terminology,
        "version", version,
        "input.file", inputFile,
        "server", "false",
        "mode", "create"));
  }

  /**
   * Runs a loader with temporary system properties.
   *
   * @param loader loader
   * @param properties loader properties
   * @throws Exception if load fails
   */
  protected void runLoader(final AbstractLoader loader,
    final Map<String, String> properties) throws Exception {

    withSystemProperties(properties, loader::run);
  }

  /**
   * Removes a terminology and its release info.
   *
   * @param terminology terminology
   * @param version version
   * @throws Exception if removal fails
   */
  protected void removeTerminology(final String terminology,
    final String version) throws Exception {

    new ContentServiceRestImpl().removeTerminology(terminology, version,
        adminAuthToken());
    removeReleaseInfo(terminology, version);
  }

  /**
   * Checks that a terminology has concepts.
   *
   * @param terminology terminology
   * @param version version
   * @throws Exception if count fails
   */
  protected void assertConceptsLoaded(final String terminology,
    final String version) throws Exception {

    assertTrue("Expected concepts for " + terminology + "/" + version,
        conceptCount(terminology, version) > 0);
  }

  /**
   * Checks that a terminology has no concepts.
   *
   * @param terminology terminology
   * @param version version
   * @throws Exception if count fails
   */
  protected void assertNoConcepts(final String terminology,
    final String version) throws Exception {

    assertEquals("Expected no concepts for " + terminology + "/" + version,
        0, conceptCount(terminology, version));
  }

  /**
   * Checks release info exists.
   *
   * @param terminology terminology
   * @param version release version
   * @throws Exception if lookup fails
   */
  protected void assertReleaseInfoExists(final String terminology,
    final String version) throws Exception {

    assertNotNull("Expected release info for " + terminology + "/" + version,
        releaseInfo(terminology, version));
  }

  /**
   * Returns component stats.
   *
   * @param terminology terminology
   * @param version version
   * @return component stats
   * @throws Exception if query fails
   */
  protected Map<String, Integer> componentStats(final String terminology,
    final String version) throws Exception {

    final ContentService service = new ContentServiceJpa();
    try {
      return service.getComponentStats(terminology, version, Branch.ROOT);
    } finally {
      service.close();
      service.closeFactory();
    }
  }

  /**
   * Returns a map from alternating key/value arguments.
   *
   * @param values alternating key/value strings
   * @return map
   */
  protected Map<String, String> properties(final String... values) {
    if (values.length % 2 != 0) {
      throw new IllegalArgumentException("properties require key/value pairs");
    }
    final Map<String, String> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }

  /**
   * Runs an operation with temporary system properties.
   *
   * @param values properties to set
   * @param operation operation
   * @throws Exception if operation fails
   */
  private void withSystemProperties(final Map<String, String> values,
    final TestOperation operation) throws Exception {

    final Map<String, String> originals = new LinkedHashMap<>();
    for (final Map.Entry<String, String> entry : values.entrySet()) {
      originals.put(entry.getKey(), System.getProperty(entry.getKey()));
      System.setProperty(entry.getKey(), entry.getValue());
    }
    try {
      operation.run();
    } finally {
      for (final Map.Entry<String, String> entry : originals.entrySet()) {
        if (entry.getValue() == null) {
          System.clearProperty(entry.getKey());
        } else {
          System.setProperty(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * Counts concepts.
   *
   * @param terminology terminology
   * @param version version
   * @return count
   * @throws Exception if query fails
   */
  private int conceptCount(final String terminology, final String version)
    throws Exception {

    final ContentService service = new ContentServiceJpa();
    try {
      return service.getAllConcepts(terminology, version, Branch.ROOT).size();
    } finally {
      service.close();
      service.closeFactory();
    }
  }

  /**
   * Returns release info.
   *
   * @param terminology terminology
   * @param version version
   * @return release info, or null
   * @throws Exception if lookup fails
   */
  private ReleaseInfo releaseInfo(final String terminology,
    final String version) throws Exception {

    final HistoryService service = new HistoryServiceJpa();
    try {
      return service.getReleaseInfo(terminology, version);
    } finally {
      service.close();
      service.closeFactory();
    }
  }

  /**
   * Removes release info if present.
   *
   * @param terminology terminology
   * @param version version
   * @throws Exception if removal fails
   */
  private void removeReleaseInfo(final String terminology,
    final String version) throws Exception {

    final HistoryService service = new HistoryServiceJpa();
    try {
      final ReleaseInfo info = service.getReleaseInfo(terminology, version);
      if (info != null) {
        service.removeReleaseInfo(info.getId());
      }
    } finally {
      service.close();
      service.closeFactory();
    }
  }

  /**
   * Authenticates the configured admin user.
   *
   * @return auth token
   * @throws Exception if authentication fails
   */
  private String adminAuthToken() throws Exception {
    final Properties properties = PropertyUtility.getProperties();
    final SecurityService service = new SecurityServiceJpa();
    try {
      return service.authenticate(properties.getProperty("admin.user"),
          properties.getProperty("admin.password")).getAuthToken();
    } finally {
      service.close();
    }
  }
}
