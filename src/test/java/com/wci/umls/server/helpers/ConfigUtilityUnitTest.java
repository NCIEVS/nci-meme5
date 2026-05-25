/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Unit tests for configuration loading.
 */
public class ConfigUtilityUnitTest {

  /**
   * Reset static state after each test.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    System.clearProperty("app.dir");
    System.clearProperty("DB_POOL_NAME");
    System.clearProperty("spring.profiles.active");
    System.clearProperty("run.config.umls");
    System.clearProperty("run.config.label");
    PropertyUtility.resetProperties();
  }

  /**
   * Verifies runtime label override no longer depends on packaged label.prop.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRuntimeConfigLabelOverride() throws Exception {
    System.setProperty("run.config.label", "ncim");
    assertEquals("ncim", PropertyUtility.getConfigLabel());
  }

  /**
   * Verifies Boot-style classpath properties are loaded and resolved.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesBridge() throws Exception {
    System.setProperty("app.dir", "/tmp/nm278-config-test");

    final Properties properties = PropertyUtility.loadApplicationProperties();
    assertNotNull(properties);
    assertEquals("/tmp/nm278-config-test", properties.getProperty("app.dir"));
    assertEquals("/tmp/nm278-config-test/data",
        properties.getProperty("source.data.dir"));
    assertEquals("http://localhost:8080/umls-server-rest",
        properties.getProperty("base.url"));
    assertFalse(properties.containsKey("spring.profiles.active"));
  }

  /**
   * Verifies Hibernate uses HikariCP settings and c3p0 keys are removed.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHikariConfigurationReplacesC3p0() throws Exception {
    System.setProperty("app.dir", "/tmp/nm279-hikari-test");
    System.setProperty("DB_POOL_NAME", "NciMemeHikariCPPool");

    final Properties properties = PropertyUtility.loadApplicationProperties();

    assertEquals("org.hibernate.hikaricp.internal.HikariCPConnectionProvider",
        properties.getProperty("hibernate.connection.provider_class"));
    assertEquals(properties.getProperty("jakarta.persistence.jdbc.url"),
        properties.getProperty("hibernate.hikari.jdbcUrl"));
    assertEquals(properties.getProperty("jakarta.persistence.jdbc.user"),
        properties.getProperty("hibernate.hikari.username"));
    assertEquals(properties.getProperty("jakarta.persistence.jdbc.password"),
        properties.getProperty("hibernate.hikari.password"));
    assertEquals("NciMemeHikariCPPool",
        properties.getProperty("hibernate.hikari.poolName"));

    for (final String key : properties.stringPropertyNames()) {
      assertFalse("Unexpected c3p0 property remains: " + key,
          key.startsWith("hibernate.c3p0."));
    }
  }

  /**
   * Verifies the runtime classpath has HikariCP and excludes c3p0 providers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHikariClasspathReplacesC3p0() throws Exception {
    Class.forName("org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
    Class.forName("com.zaxxer.hikari.HikariDataSource");

    assertClassMissing("org.hibernate.c3p0.internal.C3P0ConnectionProvider");
    assertClassMissing("com.mchange.v2.c3p0.ComboPooledDataSource");
  }

  /**
   * Verifies Spring environment property sources are flattened into Properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSpringEnvironmentProperties() throws Exception {
    final StandardEnvironment environment = new StandardEnvironment();
    final Properties sourceProperties = new Properties();
    sourceProperties.setProperty("nm302.test.value", "${app.dir}/data");
    environment.getPropertySources().addFirst(
        new PropertiesPropertySource("nm302Test", sourceProperties));
    System.setProperty("app.dir", "/tmp/nm302-env-test");

    final Properties properties =
        PropertyUtility.loadEnvironmentProperties(environment);

    assertEquals("/tmp/nm302-env-test/data",
        properties.getProperty("nm302.test.value"));
  }

  /**
   * Asserts a class is absent from the test runtime classpath.
   *
   * @param className the class name
   */
  private static void assertClassMissing(final String className) {
    try {
      Class.forName(className);
      fail("Unexpected class on runtime classpath: " + className);
    } catch (ClassNotFoundException e) {
      // expected
    }
  }

  /**
   * Verifies Spring-style properties ignore legacy run.config settings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesIgnoreLegacyRunConfig() throws Exception {
    final File tempFile = File.createTempFile("config", ".properties");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("base.url=http://legacy.example\n");
    }
    System.setProperty("app.dir", "/tmp/nm278-config-test");
    System.setProperty("run.config.umls", tempFile.getAbsolutePath());

    final Properties properties = PropertyUtility.getProperties();
    assertEquals("http://localhost:8080/umls-server-rest",
        properties.getProperty("base.url"));
    assertEquals("/tmp/nm278-config-test", properties.getProperty("app.dir"));
  }

  /**
   * Verifies home directories can be derived from Spring-style app.dir.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetHomeDirsFromApplicationProperties() throws Exception {
    System.setProperty("app.dir", "/tmp/nm278-home");

    final Map<String, String> homeDirs = PropertyUtility.getHomeDirs();
    assertEquals("/tmp/nm278-home/bin", homeDirs.get("bin"));
    assertEquals("/tmp/nm278-home/config", homeDirs.get("config"));
    assertEquals("/tmp/nm278-home/data", homeDirs.get("data"));
    assertEquals("/tmp/nm278-home/lvg", homeDirs.get("lvg"));
  }
}
