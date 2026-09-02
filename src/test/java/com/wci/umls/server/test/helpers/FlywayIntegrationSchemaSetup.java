/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Creates missing disposable schemas for Flyway integration tests.
 */
public final class FlywayIntegrationSchemaSetup {

  /** Flyway fresh-schema JDBC URL property. */
  private static final String FLYWAY_JDBC_URL = "flyway.it.jdbcUrl";

  /** Flyway legacy-baseline JDBC URL property. */
  private static final String FLYWAY_BASELINE_JDBC_URL =
      "flyway.it.baselineJdbcUrl";

  /** Drop mode. */
  private static final String DROP_MODE = "drop";

  /** Utility class. */
  private FlywayIntegrationSchemaSetup() {
    // n/a
  }

  /**
   * Creates missing Flyway integration schemas.
   *
   * @param args command-line arguments
   * @throws Exception if setup fails
   */
  public static void main(final String[] args) throws Exception {
    final String mode = args.length == 0 ? "prepare"
        : args[0].trim().toLowerCase(Locale.ROOT);
    final List<String> failures = new ArrayList<>();

    System.out.println((DROP_MODE.equals(mode) ? "Dropping" : "Preparing")
        + " Flyway integration schemas");

    if (!Boolean.getBoolean("flyway.it.enabled")) {
      failures.add("set -Dflyway.it.enabled=true for Flyway schema setup");
    }

    final Properties properties = PropertyUtility.getProperties();
    final String appSchema =
        jdbcSchemaName(properties.getProperty("jakarta.persistence.jdbc.url"));
    final String migrateUrl = System.getProperty(FLYWAY_JDBC_URL);
    final String baselineUrl = System.getProperty(FLYWAY_BASELINE_JDBC_URL);
    final String migrateSchema = jdbcSchemaName(migrateUrl);
    final String baselineSchema = jdbcSchemaName(baselineUrl);

    checkTarget(FLYWAY_JDBC_URL, migrateUrl, migrateSchema, appSchema,
        failures);
    checkTarget(FLYWAY_BASELINE_JDBC_URL, baselineUrl, baselineSchema,
        appSchema, failures);

    if (DROP_MODE.equals(mode)) {
      checkDropTarget(FLYWAY_JDBC_URL, migrateSchema, failures);
      checkDropTarget(FLYWAY_BASELINE_JDBC_URL, baselineSchema, failures);
    }

    if (!isBlank(migrateSchema) && migrateSchema.equalsIgnoreCase(
        baselineSchema)) {
      failures.add(FLYWAY_JDBC_URL + " and " + FLYWAY_BASELINE_JDBC_URL
          + " must point to different disposable schemas; both resolve to "
          + migrateSchema);
    }

    if (failures.isEmpty()) {
      if (DROP_MODE.equals(mode)) {
        dropSchema(FLYWAY_JDBC_URL, migrateUrl, migrateSchema, properties,
            failures);
        dropSchema(FLYWAY_BASELINE_JDBC_URL, baselineUrl, baselineSchema,
            properties, failures);
      } else {
        prepareSchema(FLYWAY_JDBC_URL, migrateUrl, migrateSchema, properties,
            failures);
        prepareSchema(FLYWAY_BASELINE_JDBC_URL, baselineUrl, baselineSchema,
            properties, failures);
      }
    }

    if (failures.isEmpty()) {
      System.out.println("Flyway integration schemas "
          + (DROP_MODE.equals(mode) ? "were dropped" : "are ready"));
      return;
    }

    System.out.flush();
    System.err.println("Flyway integration schema "
        + (DROP_MODE.equals(mode) ? "drop" : "setup") + " failed");
    for (final String failure : failures) {
      System.err.println("  - " + failure);
    }
    System.exit(1);
  }

  /**
   * Checks one Flyway setup target.
   *
   * @param urlProperty the URL system property
   * @param jdbcUrl the JDBC URL
   * @param schemaName the parsed schema name
   * @param appSchema the configured application schema
   * @param failures failures
   */
  private static void checkTarget(final String urlProperty,
    final String jdbcUrl, final String schemaName, final String appSchema,
    final List<String> failures) {

    if (isBlank(jdbcUrl)) {
      failures.add("set -D" + urlProperty + "=jdbc:mysql://...");
    } else if (isBlank(schemaName)) {
      failures.add("could not determine schema name from " + urlProperty);
    } else if (!schemaName.matches("[A-Za-z0-9_]+")) {
      failures.add(urlProperty + " schema may only contain letters, numbers, "
          + "and underscores: " + schemaName);
    } else if (!isBlank(appSchema) && schemaName.equalsIgnoreCase(appSchema)) {
      failures.add(urlProperty + " must not point to the configured "
          + "application schema " + appSchema);
    }
  }

  /**
   * Checks that a schema is safe to drop.
   *
   * @param urlProperty the URL system property
   * @param schemaName the schema name
   * @param failures failures
   */
  private static void checkDropTarget(final String urlProperty,
    final String schemaName, final List<String> failures) {

    final String runId = System.getProperty("flyway.it.runId", "");
    if (!Boolean.getBoolean("flyway.it.ephemeral")) {
      failures.add("set -Dflyway.it.ephemeral=true to drop Flyway schemas");
    }
    if (isBlank(runId)) {
      failures.add("set -Dflyway.it.runId=<generated id> to drop Flyway "
          + "schemas");
    } else if (!schemaName.endsWith("_" + runId)) {
      failures.add(urlProperty + " schema " + schemaName
          + " does not end with the expected generated run id " + runId);
    }
  }

  /**
   * Creates one schema if needed, then verifies it is empty.
   *
   * @param urlProperty the URL system property
   * @param jdbcUrl the JDBC URL
   * @param schemaName the schema name
   * @param properties application properties
   * @param failures failures
   */
  private static void prepareSchema(final String urlProperty,
    final String jdbcUrl, final String schemaName, final Properties properties,
    final List<String> failures) {

    System.out.println("  " + urlProperty + " schema: " + schemaName);
    final String user = System.getProperty("flyway.it.user", "root");
    final String password = System.getProperty("flyway.it.password", "");

    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      try (Connection connection =
          DriverManager.getConnection(ConfigUtility.validateJdbcServerUrl(
              serverJdbcUrl(jdbcUrl), urlProperty, properties), user, password);
          Statement statement = connection.createStatement()) {
        statement.execute("create database if not exists "
            + quoteIdentifier(schemaName));
      }

      try (Connection connection =
          DriverManager.getConnection(ConfigUtility.validateJdbcUrl(
              normalizeJdbcUrl(jdbcUrl), urlProperty, properties), user,
              password);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("show full tables")) {
        final List<String> tables = new ArrayList<>();
        while (resultSet.next()) {
          tables.add(resultSet.getString(1));
        }
        if (!tables.isEmpty()) {
          failures.add(urlProperty + " points to a non-empty schema "
              + schemaName + ": " + tables);
        }
      }
    } catch (Exception e) {
      failures.add("Flyway schema setup failed for " + urlProperty + ": "
          + e.getMessage());
    }
  }

  /**
   * Drops one generated schema.
   *
   * @param urlProperty the URL system property
   * @param jdbcUrl the JDBC URL
   * @param schemaName the schema name
   * @param properties application properties
   * @param failures failures
   */
  private static void dropSchema(final String urlProperty, final String jdbcUrl,
    final String schemaName, final Properties properties,
    final List<String> failures) {

    System.out.println("  " + urlProperty + " schema: " + schemaName);
    final String user = System.getProperty("flyway.it.user", "root");
    final String password = System.getProperty("flyway.it.password", "");

    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      try (Connection connection =
          DriverManager.getConnection(ConfigUtility.validateJdbcServerUrl(
              serverJdbcUrl(jdbcUrl), urlProperty, properties), user, password);
          Statement statement = connection.createStatement()) {
        statement.execute("drop database if exists "
            + quoteIdentifier(schemaName));
      }
    } catch (Exception e) {
      failures.add("Flyway schema drop failed for " + urlProperty + ": "
          + e.getMessage());
    }
  }

  /**
   * Returns a JDBC URL pointing at the MySQL server without a schema.
   *
   * @param jdbcUrl the JDBC URL
   * @return the server JDBC URL
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
   * Quotes a schema identifier.
   *
   * @param schemaName the schema name
   * @return the quoted schema name
   */
  private static String quoteIdentifier(final String schemaName) {
    return "`" + schemaName + "`";
  }

  /**
   * Normalizes a local MySQL JDBC URL.
   *
   * @param jdbcUrl the JDBC URL
   * @return the normalized URL
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
   * Indicates whether a value is blank.
   *
   * @param value the value
   * @return true if blank
   */
  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
