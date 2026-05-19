/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import com.wci.umls.server.helpers.MigrationUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;

/**
 * Opt-in Flyway smoke tests for disposable local MySQL databases.
 *
 * <p>These tests are skipped unless {@code -Dflyway.it.enabled=true}. They
 * intentionally fail if the target schema is not empty, because they mutate the
 * target by applying Flyway migrations or recording a legacy baseline.
 *
 * <p>Fresh database smoke:
 * <pre>
 *   ./gradlew integrationTest --tests "*.FlywayMigrationIT" \
 *       -Dflyway.it.enabled=true \
 *       -Dflyway.it.jdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_it \
 *       -Dflyway.it.user=root
 * </pre>
 *
 * <p>Legacy baseline smoke, using a separate empty disposable schema:
 * <pre>
 *   ./gradlew integrationTest --tests "*.FlywayMigrationIT" \
 *       -Dflyway.it.enabled=true \
 *       -Dflyway.it.baselineJdbcUrl=jdbc:mysql://127.0.0.1:3306/ncimdb_flyway_base_it \
 *       -Dflyway.it.user=root
 * </pre>
 */
public class FlywayMigrationIT {

  /** Opt-in flag. */
  private static final String ENABLED = "flyway.it.enabled";

  /** Fresh database JDBC URL property. */
  private static final String JDBC_URL = "flyway.it.jdbcUrl";

  /** Legacy baseline database JDBC URL property. */
  private static final String BASELINE_JDBC_URL = "flyway.it.baselineJdbcUrl";

  /** Database user property. */
  private static final String USER = "flyway.it.user";

  /** Database password property. */
  private static final String PASSWORD = "flyway.it.password";

  /**
   * Reset shared property state after tests.
   */
  @After
  public void teardown() {
    PropertyUtility.resetProperties();
  }

  /**
   * Verifies migrations can create a fresh schema, record Flyway history, and
   * allow a JPA service to bootstrap.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFreshDatabaseMigrateAndJpaBootstrap() throws Exception {
    final Properties properties = optInProperties(JDBC_URL);
    assertEmptySchema(properties);

    MigrationUtility.migrate(properties);

    assertSuccessfulVersion(properties, "1.1");
    PropertyUtility.setProperties(properties);

    MetadataServiceJpa service = null;
    try {
      service = new MetadataServiceJpa();
      final Number historyCount = (Number) service.getEntityManager()
          .createNativeQuery("select count(*) from flyway_schema_history")
          .getSingleResult();
      assertTrue(historyCount.intValue() >= 2);
    } finally {
      if (service != null) {
        service.close();
        service.closeFactory();
      }
    }
  }

  /**
   * Verifies a legacy schema can be marked as baseline and then accept later
   * migrations without replaying the baseline schema creation script.
   *
   * @throws Exception the exception
   */
  @Test
  public void testLegacyBaselineThenMigrate() throws Exception {
    final Properties properties = optInProperties(BASELINE_JDBC_URL);
    assertEmptySchema(properties);

    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement()) {
      statement.execute("create table flyway_legacy_baseline_probe "
          + "(id bigint not null primary key)");
    }

    MigrationUtility.baseline(properties);
    MigrationUtility.migrate(properties);
    MigrationUtility.validate(properties);

    assertBaselineRecorded(properties);
    assertSuccessfulVersion(properties, "1.1");
    assertEquals(0, countSuccessfulVersion(properties, "1.0"));
  }

  /**
   * Builds integration properties after checking opt-in flags.
   *
   * @param jdbcUrlProperty the JDBC URL property key
   * @return the properties
   * @throws Exception the exception
   */
  private static Properties optInProperties(final String jdbcUrlProperty)
    throws Exception {

    assumeTrue("Set -D" + ENABLED + "=true to run Flyway MySQL smoke tests",
        Boolean.getBoolean(ENABLED));

    final String jdbcUrl = normalizeJdbcUrl(System.getProperty(jdbcUrlProperty));
    assumeTrue("Set -D" + jdbcUrlProperty + "=jdbc:mysql://... to run this test",
        jdbcUrl != null && !jdbcUrl.isBlank());

    final Properties properties = PropertyUtility.loadApplicationProperties();
    final String user = System.getProperty(USER,
        properties.getProperty("hibernate.hikari.username"));
    final String password = System.getProperty(PASSWORD,
        properties.getProperty("hibernate.hikari.password", ""));

    properties.setProperty("jakarta.persistence.jdbc.url", jdbcUrl);
    properties.setProperty("jakarta.persistence.jdbc.user", user);
    properties.setProperty("jakarta.persistence.jdbc.password", password);
    properties.setProperty("hibernate.hikari.jdbcUrl", jdbcUrl);
    properties.setProperty("hibernate.hikari.username", user);
    properties.setProperty("hibernate.hikari.password", password);
    properties.setProperty("hibernate.search.backend.directory.root",
        "build/flyway-it-indexes");
    return properties;
  }

  /**
   * Normalizes a supplied MySQL JDBC URL for local test runs.
   *
   * @param jdbcUrl the JDBC URL
   * @return the normalized JDBC URL
   */
  private static String normalizeJdbcUrl(final String jdbcUrl) {
    if (jdbcUrl == null || jdbcUrl.contains("serverTimezone=")) {
      return jdbcUrl;
    }
    final String separator = jdbcUrl.contains("?") ? "&" : "?";
    return jdbcUrl + separator + "serverTimezone=UTC";
  }

  /**
   * Verifies the target schema is empty before mutating it.
   *
   * @param properties the properties
   * @throws Exception the exception
   */
  private static void assertEmptySchema(final Properties properties)
    throws Exception {
    final List<String> names = new ArrayList<>();
    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("show full tables")) {
      while (resultSet.next()) {
        names.add(resultSet.getString(1));
      }
    }
    assertTrue("Flyway IT target schema must be empty, found: " + names,
        names.isEmpty());
  }

  /**
   * Verifies a successful SQL migration version.
   *
   * @param properties the properties
   * @param version the version
   * @throws Exception the exception
   */
  private static void assertSuccessfulVersion(final Properties properties,
    final String version) throws Exception {
    assertEquals(1, countSuccessfulVersion(properties, version));
  }

  /**
   * Counts successful SQL migrations for a version.
   *
   * @param properties the properties
   * @param version the version
   * @return the count
   * @throws Exception the exception
   */
  private static int countSuccessfulVersion(final Properties properties,
    final String version) throws Exception {
    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "select count(*) from flyway_schema_history where type = 'SQL' "
                + "and version = '" + version + "' and success = 1")) {
      resultSet.next();
      return resultSet.getInt(1);
    }
  }

  /**
   * Verifies the baseline marker exists.
   *
   * @param properties the properties
   * @throws Exception the exception
   */
  private static void assertBaselineRecorded(final Properties properties)
    throws Exception {
    try (Connection connection = connect(properties);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "select count(*) from flyway_schema_history where type = 'BASELINE' "
                + "and version = '1.0' and success = 1")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1));
    }
  }

  /**
   * Opens a JDBC connection.
   *
   * @param properties the properties
   * @return the connection
   * @throws Exception the exception
   */
  private static Connection connect(final Properties properties)
    throws Exception {
    return DriverManager.getConnection(
        properties.getProperty("hibernate.hikari.jdbcUrl"),
        properties.getProperty("hibernate.hikari.username"),
        properties.getProperty("hibernate.hikari.password"));
  }
}
