/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Preflight checks for integration-test profiles.
 */
public final class IntegrationTestPreflight {

  /** Profile name for sample JPA tests. */
  private static final String SAMPLE_JPA = "sample-jpa";

  /** Profile name for NCI-META JPA tests. */
  private static final String NCI_META_JPA = "nci-meta-jpa";

  /** Profile name for insertion tests. */
  private static final String INSERTION = "insertion";

  /** Profile name for admin loader tests. */
  private static final String ADMIN_LOADER = "admin-loader";

  /** Profile name for Flyway smoke tests. */
  private static final String FLYWAY = "flyway";

  /** Profile name for REST tests. */
  private static final String REST = "rest";

  /** Flyway fresh-schema JDBC URL property. */
  private static final String FLYWAY_JDBC_URL = "flyway.it.jdbcUrl";

  /** Flyway legacy-baseline JDBC URL property. */
  private static final String FLYWAY_BASELINE_JDBC_URL =
      "flyway.it.baselineJdbcUrl";

  /** Integration preflight expected-schema property prefix. */
  private static final String EXPECTED_SCHEMA_PREFIX =
      "integration.it.expectedSchema.";

  /** Default sample database schema from the integration-test runbook. */
  private static final String SAMPLE_SCHEMA = "ncimdbmeta";

  /** Default NCI-META database schema from the integration-test runbook. */
  private static final String NCI_META_SCHEMA = "ncimdbncimeta";

  /** Default insertion database schema from the integration-test runbook. */
  private static final String INSERTION_SCHEMA = "ncimdbinsert";

  /** Default admin loader database schema from the integration-test runbook. */
  private static final String ADMIN_LOADER_SCHEMA = "ncimdbadminload";

  /** Schemas that admin/load/unload tests must never target. */
  private static final List<String> ADMIN_LOADER_PROTECTED_SCHEMAS =
      Arrays.asList("ncimdb", SAMPLE_SCHEMA, NCI_META_SCHEMA,
          INSERTION_SCHEMA);

  /** Flyway migration resources expected on the test runtime classpath. */
  private static final List<String> FLYWAY_MIGRATION_RESOURCES = Arrays.asList(
      "db.migration/V1.0__baseline_current_schema.sql",
      "db.migration/V1.1__example.sql");

  /** Profiles that require loaded project data. */
  private static final List<String> LOADED_DATA_PROFILES = Arrays.asList(
      SAMPLE_JPA, NCI_META_JPA, INSERTION, REST);

  /** Profiles that require local data directories. */
  private static final List<String> DIRECTORY_PROFILES = Arrays.asList(
      SAMPLE_JPA, NCI_META_JPA, INSERTION, ADMIN_LOADER, REST);

  /** Utility class. */
  private IntegrationTestPreflight() {
    // n/a
  }

  /**
   * Runs the requested preflight profile.
   *
   * @param args command-line arguments
   * @throws Exception if an unexpected error occurs
   */
  public static void main(final String[] args) throws Exception {
    final String profile = profile(args);
    final List<String> failures = new ArrayList<>();

    System.out.println("Running integration-test preflight: " + profile);

    if (FLYWAY.equals(profile)) {
      checkFlywaySmoke(failures);
    } else {
      final Properties properties = PropertyUtility.getProperties();
      checkResolvedProperties(properties, failures);

      if (DIRECTORY_PROFILES.contains(profile)) {
        checkDirectories(properties, failures);
        checkIndexIsolation(profile, properties, failures);
      }
      checkDatabase(properties, failures);
      checkProfileSchema(profile, properties, failures);
      if (LOADED_DATA_PROFILES.contains(profile)) {
        checkLoadedData(properties, failures);
      }
      if (SAMPLE_JPA.equals(profile) || REST.equals(profile)) {
        checkSampleFixtureData(properties, failures);
      }
      if (INSERTION.equals(profile)) {
        checkInsertionFixtureData(properties, failures);
      }
      if (ADMIN_LOADER.equals(profile)) {
        checkAdminLoaderSafety(properties, failures);
        checkAdminLoaderSourceData(properties, failures);
      }
      if (REST.equals(profile)) {
        checkRestEndpoint(properties, failures);
        checkRestUsers(properties, failures);
        checkRestSampleFixture(properties, failures);
      }
    }

    if (failures.isEmpty()) {
      System.out.println("Integration-test preflight passed: " + profile);
      return;
    }

    System.out.flush();
    System.err.println("Integration-test preflight failed: " + profile);
    for (final String failure : failures) {
      System.err.println("  - " + failure);
    }
    System.exit(1);
  }

  /**
   * Returns the profile argument.
   *
   * @param args command-line arguments
   * @return the profile
   */
  private static String profile(final String[] args) {
    if (args.length == 0 || isBlank(args[0])) {
      return SAMPLE_JPA;
    }
    return args[0].trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Checks property resolution.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkResolvedProperties(final Properties properties,
    final List<String> failures) {

    if (properties == null) {
      failures.add("application properties could not be loaded");
      return;
    }

    for (final String key : Arrays.asList("jakarta.persistence.jdbc.url",
        "jakarta.persistence.jdbc.user", "data.dir", "index.dir",
        "source.data.dir", "base.url")) {
      final String value = properties.getProperty(key);
      if (isBlank(value)) {
        failures.add("required property is blank: " + key);
      } else if (value.contains("${")) {
        failures.add("property still contains an unresolved placeholder: "
            + key + "=" + value);
      }
    }
  }

  /**
   * Checks local filesystem directories.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkDirectories(final Properties properties,
    final List<String> failures) {

    for (final String key : Arrays.asList("data.dir", "source.data.dir",
        "index.dir", "lvg.dir")) {
      final String value = property(properties, key);
      if (isBlank(value)) {
        failures.add("required directory property is blank: " + key);
        continue;
      }

      final File dir = new File(value);
      if (!dir.isDirectory()) {
        failures.add("required directory does not exist: " + key + "="
            + dir.getAbsolutePath());
      }
    }
  }

  /**
   * Checks that Hibernate Search indexes are isolated with the fixture data.
   *
   * @param profile the profile
   * @param properties the properties
   * @param failures failures
   */
  private static void checkIndexIsolation(final String profile,
    final Properties properties, final List<String> failures) {

    final File dataDir = new File(property(properties, "data.dir"));
    final File indexDir = new File(property(properties, "index.dir"));
    try {
      final File canonicalData = dataDir.getCanonicalFile();
      final File canonicalIndex = indexDir.getCanonicalFile();
      if (canonicalData.equals(canonicalIndex)) {
        failures.add("profile " + profile
            + " must not use data.dir itself as index.dir: "
            + canonicalIndex.getAbsolutePath());
      } else if (!Boolean.getBoolean(
          "integration.it.allowExternalIndexDir")
          && !isUnderDirectory(canonicalIndex, canonicalData)) {
        failures.add("profile " + profile
            + " should isolate index.dir under data.dir. Found index.dir="
            + canonicalIndex.getAbsolutePath() + ", data.dir="
            + canonicalData.getAbsolutePath()
            + ". Override only deliberately with "
            + "-Dintegration.it.allowExternalIndexDir=true.");
      }
    } catch (Exception e) {
      failures.add("index isolation check failed: " + e.getMessage());
    }
  }

  /**
   * Checks database connectivity.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkDatabase(final Properties properties,
    final List<String> failures) {

    try {
      Class.forName(property(properties, "jakarta.persistence.jdbc.driver"));
      try (Connection connection = connect(properties);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("select 1")) {
        if (!resultSet.next()) {
          failures.add("database connectivity query returned no rows");
        }
      }
    } catch (Exception e) {
      failures.add("database connectivity failed: " + e.getMessage());
    }
  }

  /**
   * Checks that a loaded database has project data.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkLoadedData(final Properties properties,
    final List<String> failures) {

    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement();
        ResultSet resultSet =
            statement.executeQuery("select count(*) from projects")) {
      if (resultSet.next() && resultSet.getInt(1) < 1) {
        failures.add("loaded test database has no projects");
      }
    } catch (Exception e) {
      failures.add("loaded-data check failed; expected projects table: "
          + e.getMessage());
    }
  }

  /**
   * Checks that a named JPA profile is pointed at the expected fixture schema.
   *
   * @param profile the profile name
   * @param properties the properties
   * @param failures failures
   */
  private static void checkProfileSchema(final String profile,
    final Properties properties, final List<String> failures) {

    final String expectedSchema = expectedSchema(profile);
    if (isBlank(expectedSchema)) {
      return;
    }

    final String actualSchema =
        jdbcSchemaName(property(properties, "jakarta.persistence.jdbc.url"));
    if (isBlank(actualSchema)) {
      failures.add("could not determine configured database schema for "
          + profile + " from jakarta.persistence.jdbc.url");
    } else if (!actualSchema.equalsIgnoreCase(expectedSchema)) {
      failures.add("profile " + profile + " is configured for database schema "
          + actualSchema + "; expected " + expectedSchema
          + ". Export DB_NAME=" + expectedSchema
          + " before sourcing config/local/setenv.sh, or intentionally "
          + "override with -D" + EXPECTED_SCHEMA_PREFIX + profile
          + "=<schema>.");
    }
  }

  /**
   * Checks sample fixture rows used by the sample JPA smoke tests.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkSampleFixtureData(final Properties properties,
    final List<String> failures) {

    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement()) {
      checkMinimumRowCount(statement,
          "select count(*) from projects where terminology = 'MTH' "
              + "and version = 'latest'",
          1, "sample database is missing the MTH/latest project", failures);
      checkMinimumRowCount(statement,
          "select count(*) from concepts where terminology = 'MTH' "
              + "and version = 'latest' and terminologyId = 'C0000294'",
          1, "sample database is missing concept C0000294 for MTH/latest",
          failures);
      checkMinimumRowCount(statement,
          "select count(*) from concepts where terminology = 'MTH' "
              + "and version = 'latest' and terminologyId = 'C0000097'",
          1, "sample database is missing concept C0000097 for MTH/latest",
          failures);
    } catch (Exception e) {
      failures.add("sample fixture check failed: " + e.getMessage());
    }
  }

  /**
   * Checks fixture rows and source directories used by insertion smoke tests.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkInsertionFixtureData(final Properties properties,
    final List<String> failures) {

    final File sourceDir =
        new File(property(properties, "source.data.dir"),
            "terminologies/NCI_INSERT/src");
    if (!sourceDir.isDirectory()) {
      failures.add("insertion source-data directory is missing: "
          + sourceDir.getAbsolutePath());
    } else {
      for (final String name : Arrays.asList("sources.src", "termgroups.src",
          "classes_atoms.src", "relationships.src", "contexts.src",
          "attributes.src", "mergefacts.src", "MRDOC.RRF")) {
        checkFileExists(sourceDir, name, failures);
      }
    }

    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement()) {
      checkMinimumRowCount(statement,
          "select count(*) from projects where terminology = 'NCIMTH' "
              + "and version = 'latest'",
          1, "insertion database is missing the NCIMTH/latest project",
          failures);
      checkMinimumRowCount(statement,
          "select count(*) from concepts where terminology = 'NCIMTH' "
              + "and version = 'latest'",
          1, "insertion database is missing NCIMTH/latest concepts",
          failures);
      checkMinimumRowCount(statement,
          "select count(*) from concepts where terminology = 'NCI' "
              + "and version = '2016_04D'",
          1, "insertion database is missing baseline NCI/2016_04D concepts",
          failures);
    } catch (Exception e) {
      failures.add("insertion fixture check failed: " + e.getMessage());
    }
  }

  /**
   * Checks that a required file exists under a directory.
   *
   * @param dir the directory
   * @param name the file name
   * @param failures failures
   */
  private static void checkFileExists(final File dir, final String name,
    final List<String> failures) {

    final File file = new File(dir, name);
    if (!file.isFile()) {
      failures.add("required insertion source file is missing: "
          + file.getAbsolutePath());
    }
  }

  /**
   * Checks that a query returns at least a minimum count.
   *
   * @param statement the statement
   * @param query the SQL query
   * @param minimum the minimum expected value
   * @param message failure message
   * @param failures failures
   * @throws Exception if the query fails
   */
  private static void checkMinimumRowCount(final Statement statement,
    final String query, final int minimum, final String message,
    final List<String> failures) throws Exception {

    try (ResultSet resultSet = statement.executeQuery(query)) {
      if (!resultSet.next() || resultSet.getInt(1) < minimum) {
        failures.add(message);
      }
    }
  }

  /**
   * Returns the expected schema for a profile.
   *
   * @param profile the profile name
   * @return the expected schema, or empty string when no schema is enforced
   */
  private static String expectedSchema(final String profile) {
    String defaultSchema = "";
    if (SAMPLE_JPA.equals(profile)) {
      defaultSchema = SAMPLE_SCHEMA;
    } else if (NCI_META_JPA.equals(profile)) {
      defaultSchema = NCI_META_SCHEMA;
    } else if (INSERTION.equals(profile)) {
      defaultSchema = INSERTION_SCHEMA;
    } else if (ADMIN_LOADER.equals(profile)) {
      defaultSchema = ADMIN_LOADER_SCHEMA;
    } else if (REST.equals(profile)) {
      defaultSchema = SAMPLE_SCHEMA;
    }
    return System.getProperty(EXPECTED_SCHEMA_PREFIX + profile,
        defaultSchema);
  }

  /**
   * Checks that admin/load tests point at a disposable schema.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkAdminLoaderSafety(final Properties properties,
    final List<String> failures) {

    final String actualSchema =
        jdbcSchemaName(property(properties, "jakarta.persistence.jdbc.url"));
    if (isBlank(actualSchema)) {
      failures.add("could not determine admin-loader database schema from "
          + "jakarta.persistence.jdbc.url");
      return;
    }

    for (final String protectedSchema : ADMIN_LOADER_PROTECTED_SCHEMAS) {
      if (actualSchema.equalsIgnoreCase(protectedSchema)) {
        failures.add("admin-loader profile must not target shared schema "
            + actualSchema + "; use " + ADMIN_LOADER_SCHEMA
            + " or another disposable schema");
        return;
      }
    }
  }

  /**
   * Checks bundled source files used by the admin/load smoke tests.
   *
   * @param failures failures
   */
  private static void checkAdminLoaderSourceData(final Properties properties,
    final List<String> failures) {

    final File dataRoot = new File("config/src/main/resources/data");
    for (final String path : Arrays.asList(
        "SAMPLE_UMLS/MRCONSO.RRF",
        "SAMPLE_UMLS/MRHIER.RRF",
        "snomedct-20140731-mini/Terminology/sct2_Concept_INT_20140731.txt",
        "snomedct-20140731-minif/Terminology/"
            + "sct2_Concept_Full_INT_20140731.txt",
        "icd10cm-2016.xml",
        "snomed.owl")) {
      final File file = new File(dataRoot, path);
      if (!file.isFile()) {
        failures.add("required admin-loader source file is missing: "
            + file.getAbsolutePath());
      }
    }

    final File dataDir = new File(property(properties, "data.dir"));
    for (final String name : Arrays.asList("acronyms.txt", "spelling.txt")) {
      final File file = new File(dataDir, name);
      if (!file.isFile()) {
        failures.add("required admin-loader data file is missing: "
            + file.getAbsolutePath()
            + "; run make prepare-admin before make integration-admin");
      }
    }
  }

  /**
   * Checks REST server availability.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkRestEndpoint(final Properties properties,
    final List<String> failures) {

    final String path = "/configure/configured";
    final String url;
    try {
      url = ConfigUtility.getRestUrl(properties, path);
    } catch (IllegalArgumentException e) {
      failures.add(e.getMessage());
      return;
    }

    try {
      final HttpURLConnection connection =
          ConfigUtility.openRestConnection(properties, path);
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      final int status = connection.getResponseCode();
      if (status < 200 || status >= 300) {
        failures.add("REST preflight endpoint returned HTTP " + status + ": "
            + url);
      }
    } catch (Exception e) {
      failures.add("REST server is not reachable at " + url + ": "
          + e.getMessage());
    }
  }

  /**
   * Checks REST test users.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkRestUsers(final Properties properties,
    final List<String> failures) {

    for (final String key : Arrays.asList("admin.user", "admin.password",
        "viewer.user", "viewer.password")) {
      if (isBlank(property(properties, key))) {
        failures.add("REST test credential property is blank: " + key);
      }
    }
  }

  /**
   * Checks that the running REST server exposes the expected sample fixture.
   *
   * @param properties the properties
   * @param failures failures
   */
  private static void checkRestSampleFixture(final Properties properties,
    final List<String> failures) {

    final String path = "/content/concept/MTH/latest/C0000097";
    final String url;
    try {
      url = ConfigUtility.getRestUrl(properties, path);
    } catch (IllegalArgumentException e) {
      failures.add(e.getMessage());
      return;
    }

    final String authToken = authenticateRestAdmin(properties, failures);
    if (isBlank(authToken)) {
      return;
    }

    try {
      final HttpURLConnection connection =
          ConfigUtility.openRestConnection(properties, path);
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Authorization", authToken);
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);

      final int status = connection.getResponseCode();
      final String response = readResponse(connection);
      if (status < 200 || status >= 300) {
        failures.add("REST sample fixture probe returned HTTP " + status
            + " for " + url + ". The app may be running against the wrong "
            + "database; expected " + SAMPLE_SCHEMA + ".");
      } else if (!response.contains("\"terminology\":\"MTH\"")
          || !response.contains("\"version\":\"latest\"")
          || !response.contains("\"terminologyId\":\"C0000097\"")) {
        failures.add("REST sample fixture probe did not return MTH/latest "
            + "concept C0000097 from " + url + ". The app may be running "
            + "against the wrong database; expected " + SAMPLE_SCHEMA + ".");
      }
    } catch (Exception e) {
      failures.add("REST sample fixture probe failed at " + url + ": "
          + e.getMessage());
    }
  }

  /**
   * Authenticates the configured REST admin user.
   *
   * @param properties the properties
   * @param failures failures
   * @return auth token, or empty string
   */
  private static String authenticateRestAdmin(final Properties properties,
    final List<String> failures) {

    final String adminUser = property(properties, "admin.user");
    final String adminPassword = property(properties, "admin.password");
    if (isBlank(adminUser) || isBlank(adminPassword)) {
      return "";
    }

    final String path = "/security/authenticate/" + adminUser;
    final String url;
    try {
      url = ConfigUtility.getRestUrl(properties, path);
    } catch (IllegalArgumentException e) {
      failures.add(e.getMessage());
      return "";
    }

    try {
      final HttpURLConnection connection =
          ConfigUtility.openRestConnection(properties, path);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "text/plain");
      connection.setRequestProperty("Accept", "application/json");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.setDoOutput(true);

      try (OutputStream output = connection.getOutputStream()) {
        output.write(adminPassword.getBytes(StandardCharsets.UTF_8));
      }

      final int status = connection.getResponseCode();
      final String response = readResponse(connection);
      if (status < 200 || status >= 300) {
        failures.add("REST admin authentication returned HTTP " + status
            + " for " + url);
        return "";
      }

      final String token = extractJsonString(response, "authToken");
      if (isBlank(token)) {
        failures.add("REST admin authentication did not return an authToken");
      }
      return token;
    } catch (Exception e) {
      failures.add("REST admin authentication failed at " + url + ": "
          + e.getMessage());
      return "";
    }
  }

  /**
   * Checks the opt-in Flyway smoke-test configuration.
   *
   * @param failures failures
   */
  private static void checkFlywaySmoke(final List<String> failures) {
    if (!Boolean.getBoolean("flyway.it.enabled")) {
      failures.add("set -Dflyway.it.enabled=true for Flyway smoke preflight");
    }
    checkFlywayMigrationResources(failures);

    final Properties properties = PropertyUtility.getProperties();
    checkFlywayTargetSafety(properties, failures);
    checkFlywaySchema(FLYWAY_JDBC_URL, properties, failures);
    checkFlywaySchema(FLYWAY_BASELINE_JDBC_URL, properties, failures);
  }

  /**
   * Checks Flyway migration resources.
   *
   * @param failures failures
   */
  private static void checkFlywayMigrationResources(
    final List<String> failures) {

    final ClassLoader loader =
        Thread.currentThread().getContextClassLoader();
    for (final String resource : FLYWAY_MIGRATION_RESOURCES) {
      if (loader.getResource(resource) == null) {
        failures.add("Flyway migration resource is not on the test runtime "
            + "classpath: " + resource);
      }
    }
  }

  /**
   * Checks that Flyway smoke-test targets are disposable schemas.
   *
   * @param properties application properties
   * @param failures failures
   */
  private static void checkFlywayTargetSafety(final Properties properties,
    final List<String> failures) {

    final String appSchema =
        jdbcSchemaName(property(properties, "jakarta.persistence.jdbc.url"));
    final String migrateSchema =
        jdbcSchemaName(System.getProperty(FLYWAY_JDBC_URL));
    final String baselineSchema =
        jdbcSchemaName(System.getProperty(FLYWAY_BASELINE_JDBC_URL));

    checkFlywaySchemaName(FLYWAY_JDBC_URL, migrateSchema, appSchema, failures);
    checkFlywaySchemaName(FLYWAY_BASELINE_JDBC_URL, baselineSchema, appSchema,
        failures);

    if (!isBlank(migrateSchema) && migrateSchema.equalsIgnoreCase(
        baselineSchema)) {
      failures.add(FLYWAY_JDBC_URL + " and " + FLYWAY_BASELINE_JDBC_URL
          + " must point to different disposable schemas; both resolve to "
          + migrateSchema);
    }
  }

  /**
   * Checks one Flyway schema name.
   *
   * @param urlProperty the URL system property
   * @param schemaName the schema name
   * @param appSchema the configured application schema
   * @param failures failures
   */
  private static void checkFlywaySchemaName(final String urlProperty,
    final String schemaName, final String appSchema,
    final List<String> failures) {

    if (isBlank(System.getProperty(urlProperty))) {
      return;
    }
    if (isBlank(schemaName)) {
      failures.add("could not determine schema name from " + urlProperty
          + "; use a JDBC URL ending in a disposable schema name");
    } else if (!isBlank(appSchema) && schemaName.equalsIgnoreCase(appSchema)) {
      failures.add(urlProperty + " must not point to the configured "
          + "application schema " + appSchema
          + "; use an empty disposable schema instead");
    }
  }

  /**
   * Checks one Flyway smoke schema.
   *
   * @param urlProperty the URL system property
   * @param failures failures
   */
  private static void checkFlywaySchema(final String urlProperty,
    final Properties properties, final List<String> failures) {

    final String jdbcUrl = System.getProperty(urlProperty);
    if (isBlank(jdbcUrl)) {
      failures.add("set -D" + urlProperty + "=jdbc:mysql://...");
      return;
    }

    final String schemaName = jdbcSchemaName(jdbcUrl);
    System.out.println("  " + urlProperty + " schema: "
        + (isBlank(schemaName) ? "<unknown>" : schemaName));

    final String user = System.getProperty("flyway.it.user", "root");
    final String password = System.getProperty("flyway.it.password", "");
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      try (Connection connection =
          DriverManager.getConnection(ConfigUtility.validateJdbcUrl(
              normalizeJdbcUrl(jdbcUrl), urlProperty, properties), user,
              password);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("show full tables")) {
        int tableCount = 0;
        final List<String> sampleTables = new ArrayList<>();
        while (resultSet.next()) {
          tableCount++;
          if (sampleTables.size() < 5) {
            sampleTables.add(resultSet.getString(1));
          }
        }
        if (tableCount > 0) {
          failures.add(urlProperty + " must point to an empty schema; found "
              + tableCount + " table(s), including "
              + String.join(", ", sampleTables));
        }
      }
    } catch (Exception e) {
      failures.add("Flyway schema check failed for " + urlProperty + ": "
          + e.getMessage());
    }
  }

  /**
   * Opens a JDBC connection from application properties.
   *
   * @param properties the properties
   * @return the connection
   * @throws Exception if connection fails
   */
  private static Connection connect(final Properties properties)
    throws Exception {

    return DriverManager.getConnection(
        ConfigUtility.getJdbcUrl(properties, "jakarta.persistence.jdbc.url"),
        property(properties, "jakarta.persistence.jdbc.user"),
        property(properties, "jakarta.persistence.jdbc.password"));
  }

  /**
   * Returns a property value, defaulting to an empty string.
   *
   * @param properties the properties
   * @param key the key
   * @return the value
   */
  private static String property(final Properties properties, final String key) {
    return properties == null ? "" : properties.getProperty(key, "");
  }

  /**
   * Normalizes a local MySQL JDBC URL.
   *
   * @param jdbcUrl the JDBC URL
   * @return the normalized URL
   */
  private static String normalizeJdbcUrl(final String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.contains("serverTimezone=")) {
      return jdbcUrl;
    }
    final String separator = jdbcUrl.contains("?") ? "&" : "?";
    return jdbcUrl + separator + "serverTimezone=UTC";
  }

  /**
   * Returns the schema name from a JDBC URL.
   *
   * @param jdbcUrl the JDBC URL
   * @return the schema name, or an empty string
   */
  private static String jdbcSchemaName(final String jdbcUrl) {
    if (isBlank(jdbcUrl)) {
      return "";
    }

    final String uriText =
        jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;
    try {
      final URI uri = URI.create(uriText);
      final String path = uri.getPath();
      if (!isBlank(path) && path.length() > 1) {
        return path.substring(1);
      }
    } catch (IllegalArgumentException e) {
      // Fall through to the simple parser below.
    }

    final String withoutParameters = jdbcUrl.split("\\?", 2)[0];
    final int slash = withoutParameters.lastIndexOf('/');
    if (slash >= 0 && slash < withoutParameters.length() - 1) {
      return withoutParameters.substring(slash + 1);
    }
    return "";
  }

  /**
   * Reads an HTTP response body.
   *
   * @param connection the connection
   * @return the response body
   * @throws Exception if reading fails
   */
  private static String readResponse(final HttpURLConnection connection)
    throws Exception {

    InputStream input = connection.getErrorStream();
    if (input == null) {
      input = connection.getInputStream();
    }
    try (InputStream stream = input) {
      if (stream == null) {
        return "";
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /**
   * Extracts a simple JSON string field.
   *
   * @param json the JSON text
   * @param fieldName the field name
   * @return the field value, or an empty string
   */
  private static String extractJsonString(final String json,
    final String fieldName) {

    if (isBlank(json) || isBlank(fieldName)) {
      return "";
    }
    final String marker = "\"" + fieldName + "\":\"";
    final int start = json.indexOf(marker);
    if (start < 0) {
      return "";
    }
    final int valueStart = start + marker.length();
    final int valueEnd = json.indexOf('"', valueStart);
    if (valueEnd < 0) {
      return "";
    }
    return json.substring(valueStart, valueEnd);
  }

  /**
   * Indicates whether a path is contained by a directory.
   *
   * @param path the path
   * @param directory the directory
   * @return true if the path is under the directory
   */
  private static boolean isUnderDirectory(final File path,
    final File directory) {
    File current = path;
    while (current != null) {
      if (current.equals(directory)) {
        return true;
      }
      current = current.getParentFile();
    }
    return false;
  }

  /**
   * Indicates whether a value is blank.
   *
   * @param value the value
   * @return true if blank
   */
  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
