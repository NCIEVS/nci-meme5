/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Test;

/**
 * Unit tests for Flyway migration configuration.
 */
public class MigrationUtilityUnitTest {

  /**
   * Reset static property state after each test.
   */
  @After
  public void teardown() {
    System.clearProperty("app.dir");
    System.clearProperty("spring.profiles.active");
    PropertyUtility.resetProperties();
  }

  /**
   * Verifies application properties include manual Flyway settings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesIncludeFlywayDefaults() throws Exception {
    System.setProperty("app.dir", "/tmp/nm280-flyway-test");
    System.setProperty("spring.profiles.active", "local");

    final Properties properties = PropertyUtility.loadApplicationProperties();

    assertEquals("false", properties.getProperty("spring.flyway.enabled"));
    assertEquals(MigrationUtility.DEFAULT_LOCATIONS,
        properties.getProperty("flyway.locations"));
    assertEquals(MigrationUtility.DEFAULT_BASELINE_VERSION,
        properties.getProperty("flyway.baseline.version"));
    assertEquals(MigrationUtility.DEFAULT_BASELINE_DESCRIPTION,
        properties.getProperty("flyway.baseline.description"));
    assertEquals("false", properties.getProperty("flyway.migrate.on.startup"));
    assertNotNull(Class.forName("org.flywaydb.core.Flyway"));
  }

  /**
   * Verifies Hikari connection keys take priority.
   */
  @Test
  public void testHikariConnectionPropertiesPreferred() {
    final Properties properties = new Properties();
    properties.setProperty("hibernate.hikari.jdbcUrl", "jdbc:mysql://hikari/db");
    properties.setProperty("hibernate.hikari.username", "hikari-user");
    properties.setProperty("hibernate.hikari.password", "hikari-password");
    properties.setProperty("jakarta.persistence.jdbc.url",
        "jdbc:mysql://jakarta/db");
    properties.setProperty("jakarta.persistence.jdbc.user", "jakarta-user");
    properties.setProperty("jakarta.persistence.jdbc.password",
        "jakarta-password");

    final MigrationUtility.ConnectionProperties connection =
        MigrationUtility.resolveConnectionProperties(properties);

    assertEquals("jdbc:mysql://hikari/db", connection.getJdbcUrl());
    assertEquals("hikari-user", connection.getUsername());
    assertEquals("hikari-password", connection.getPassword());
  }

  /**
   * Verifies Jakarta persistence keys are still supported during transition.
   */
  @Test
  public void testJakartaConnectionPropertiesFallback() {
    final Properties properties = new Properties();
    properties.setProperty("jakarta.persistence.jdbc.url",
        "jdbc:mysql://jakarta/db");
    properties.setProperty("jakarta.persistence.jdbc.user", "jakarta-user");
    properties.setProperty("jakarta.persistence.jdbc.password", "");

    final MigrationUtility.ConnectionProperties connection =
        MigrationUtility.resolveConnectionProperties(properties);

    assertEquals("jdbc:mysql://jakarta/db", connection.getJdbcUrl());
    assertEquals("jakarta-user", connection.getUsername());
    assertEquals("", connection.getPassword());
  }

  /**
   * Verifies configured migration locations are parsed and trimmed.
   */
  @Test
  public void testResolveLocations() {
    final Properties properties = new Properties();
    properties.setProperty("flyway.locations",
        " classpath:db.migration , classpath:db/migration ");

    assertArrayEquals(new String[] {
        "classpath:db.migration", "classpath:db/migration"
    }, MigrationUtility.resolveLocations(properties));
  }

  /**
   * Verifies Flyway is configured with the operational safety defaults.
   */
  @Test
  public void testBuildFlywayConfiguration() {
    final Properties properties = new Properties();
    properties.setProperty("hibernate.hikari.jdbcUrl", "jdbc:mysql://hikari/db");
    properties.setProperty("hibernate.hikari.username", "hikari-user");
    properties.setProperty("hibernate.hikari.password", "");
    properties.setProperty("flyway.baseline.version", "1.0");
    properties.setProperty("flyway.baseline.description", "Baseline test");

    final Flyway flyway = MigrationUtility.buildFlyway(properties);

    assertEquals("1.0",
        flyway.getConfiguration().getBaselineVersion().getVersion());
    assertEquals("Baseline test",
        flyway.getConfiguration().getBaselineDescription());
    assertEquals(false, flyway.getConfiguration().isBaselineOnMigrate());
    assertEquals(true, flyway.getConfiguration().isCleanDisabled());
    assertEquals("classpath:db.migration",
        flyway.getConfiguration().getLocations()[0].getDescriptor());
    assertEquals("classpath:db/migration",
        flyway.getConfiguration().getLocations()[1].getDescriptor());
  }

  /**
   * Verifies missing JDBC URL fails with a clear error.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testMissingJdbcUrlRejected() {
    final Properties properties = new Properties();
    properties.setProperty("hibernate.hikari.username", "user");

    MigrationUtility.resolveConnectionProperties(properties);
  }
}
