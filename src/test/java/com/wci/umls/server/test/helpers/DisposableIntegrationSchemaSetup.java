/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Creates or drops disposable schemas used by integration-test profiles.
 */
public final class DisposableIntegrationSchemaSetup {

  /** Admin loader profile name. */
  private static final String ADMIN_LOADER = "admin-loader";

  /** Expected-schema property prefix. */
  private static final String EXPECTED_SCHEMA_PREFIX =
      "integration.it.expectedSchema.";

  /** Default admin loader schema. */
  private static final String ADMIN_LOADER_SCHEMA = "ncimdbadminload";

  /** Drop mode. */
  private static final String DROP_MODE = "drop";

  /** Schemas that must not be managed by this helper. */
  private static final List<String> PROTECTED_SCHEMAS = Arrays.asList(
      "ncimdb", "ncimdbmeta", "ncimdbncimeta", "ncimdbinsert");

  /** Utility class. */
  private DisposableIntegrationSchemaSetup() {
    // n/a
  }

  /**
   * Creates or drops the selected disposable schema.
   *
   * @param args profile name and optional mode
   * @throws Exception if setup fails
   */
  public static void main(final String[] args) throws Exception {
    final String profile = args.length == 0 ? ADMIN_LOADER
        : args[0].trim().toLowerCase(Locale.ROOT);
    final String mode = args.length < 2 ? "prepare"
        : args[1].trim().toLowerCase(Locale.ROOT);

    if (!ADMIN_LOADER.equals(profile)) {
      fail("unsupported disposable schema profile: " + profile);
    }

    final Properties properties = PropertyUtility.getProperties();
    final String jdbcUrl =
        properties.getProperty("jakarta.persistence.jdbc.url", "");
    final String schemaName = jdbcSchemaName(jdbcUrl);
    final String expectedSchema = System.getProperty(
        EXPECTED_SCHEMA_PREFIX + profile, ADMIN_LOADER_SCHEMA);

    validateTarget(profile, schemaName, expectedSchema, mode);

    if (DROP_MODE.equals(mode)) {
      dropSchema(properties, jdbcUrl, schemaName);
      System.out.println("Dropped disposable schema for " + profile + ": "
          + schemaName);
    } else {
      createSchema(properties, jdbcUrl, schemaName);
      System.out.println("Prepared disposable schema for " + profile + ": "
          + schemaName);
    }
  }

  /**
   * Validates the schema target.
   *
   * @param profile the profile
   * @param schemaName the schema name
   * @param expectedSchema the expected schema
   * @param mode setup mode
   */
  private static void validateTarget(final String profile,
    final String schemaName, final String expectedSchema, final String mode) {

    if (isBlank(schemaName)) {
      fail("could not determine configured schema for " + profile);
    }
    if (!schemaName.matches("[A-Za-z0-9_]+")) {
      fail("schema may only contain letters, numbers, and underscores: "
          + schemaName);
    }
    for (final String protectedSchema : PROTECTED_SCHEMAS) {
      if (schemaName.equalsIgnoreCase(protectedSchema)) {
        fail(profile + " schema " + schemaName
            + " is protected; choose a disposable schema");
      }
    }
    if (!schemaName.equalsIgnoreCase(expectedSchema)) {
      fail(profile + " schema " + schemaName + " does not match expected "
          + expectedSchema + "; export DB_NAME=" + expectedSchema
          + " before sourcing config/local/setenv.sh or override -D"
          + EXPECTED_SCHEMA_PREFIX + profile + "=<schema>");
    }
    if (DROP_MODE.equals(mode)) {
      final String runId = System.getProperty("integration.it.runId", "");
      if (!Boolean.getBoolean("integration.it.ephemeral")) {
        fail("set -Dintegration.it.ephemeral=true before dropping "
            + "disposable schemas");
      }
      if (isBlank(runId) || !schemaName.endsWith("_" + runId)) {
        fail("refusing to drop schema " + schemaName
            + "; expected generated run id suffix _" + runId);
      }
    } else if (!"prepare".equals(mode)) {
      fail("unsupported disposable schema mode: " + mode);
    }
  }

  /**
   * Creates a schema if needed.
   *
   * @param properties application properties
   * @param jdbcUrl JDBC URL
   * @param schemaName schema name
   * @throws Exception if creation fails
   */
  private static void createSchema(final Properties properties,
    final String jdbcUrl, final String schemaName) throws Exception {

    Class.forName(properties.getProperty("jakarta.persistence.jdbc.driver"));
    try (Connection connection = DriverManager.getConnection(
        serverJdbcUrl(jdbcUrl),
        properties.getProperty("jakarta.persistence.jdbc.user"),
        properties.getProperty("jakarta.persistence.jdbc.password"));
        Statement statement = connection.createStatement()) {
      statement.execute("create database if not exists "
          + quoteIdentifier(schemaName));
    }
  }

  /**
   * Drops a generated schema.
   *
   * @param properties application properties
   * @param jdbcUrl JDBC URL
   * @param schemaName schema name
   * @throws Exception if drop fails
   */
  private static void dropSchema(final Properties properties,
    final String jdbcUrl, final String schemaName) throws Exception {

    Class.forName(properties.getProperty("jakarta.persistence.jdbc.driver"));
    try (Connection connection = DriverManager.getConnection(
        serverJdbcUrl(jdbcUrl),
        properties.getProperty("jakarta.persistence.jdbc.user"),
        properties.getProperty("jakarta.persistence.jdbc.password"));
        Statement statement = connection.createStatement()) {
      statement.execute("drop database if exists "
          + quoteIdentifier(schemaName));
    }
  }

  /**
   * Returns a server JDBC URL without a schema path.
   *
   * @param jdbcUrl the schema JDBC URL
   * @return server JDBC URL
   */
  private static String serverJdbcUrl(final String jdbcUrl) {
    final int queryStart = jdbcUrl.indexOf('?');
    final String prefix = queryStart >= 0 ? jdbcUrl.substring(0, queryStart)
        : jdbcUrl;
    final String query = queryStart >= 0 ? jdbcUrl.substring(queryStart) : "";
    final int slash = prefix.lastIndexOf('/');
    if (slash < "jdbc:mysql://".length()) {
      return normalizeJdbcUrl(jdbcUrl);
    }
    return normalizeJdbcUrl(prefix.substring(0, slash + 1) + query);
  }

  /**
   * Normalizes a local MySQL JDBC URL.
   *
   * @param jdbcUrl JDBC URL
   * @return normalized URL
   */
  private static String normalizeJdbcUrl(final String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.toLowerCase(Locale.ROOT)
        .contains("servertimezone=")) {
      return jdbcUrl;
    }
    final String separator = jdbcUrl.contains("?") ? "&" : "?";
    return jdbcUrl + separator + "serverTimezone=UTC";
  }

  /**
   * Returns the schema name from a JDBC URL.
   *
   * @param jdbcUrl JDBC URL
   * @return schema name, or empty string
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
   * Quotes a schema identifier.
   *
   * @param schemaName schema name
   * @return quoted schema name
   */
  private static String quoteIdentifier(final String schemaName) {
    return "`" + schemaName + "`";
  }

  /**
   * Indicates whether text is blank.
   *
   * @param value the value
   * @return true if blank
   */
  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Fails setup with an unchecked exception.
   *
   * @param message failure message
   */
  private static void fail(final String message) {
    throw new IllegalArgumentException(message);
  }
}
