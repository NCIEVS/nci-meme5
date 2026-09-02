/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
    System.clearProperty("app.display.timezone");
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
    assertNotNull(properties.getProperty("database.allowed.hosts"));
    assertNotNull(properties.getProperty("rest.client.allowed.hosts"));
    assertFalse(properties.getProperty("algorithm.handler").contains("RUNMMSYS"));
    assertFalse(properties.containsKey("algorithm.handler.RUNMMSYS.class"));
    assertEquals("meme-team@westcoastinformatics.com",
        properties.getProperty("insertion.notification.recipients"));
    assertFalse(properties.containsKey("spring.profiles.active"));
  }

  /**
   * Verifies user-facing timestamps use the configured display timezone.
   *
   * @throws Exception the exception
   */
  @Test
  public void testFormatDisplayTimestampUsesConfiguredTimeZone()
    throws Exception {
    System.setProperty("app.display.timezone", "America/New_York");

    final Date timestamp =
        Date.from(Instant.parse("2026-06-30T17:10:45Z"));

    assertEquals("2026-06-30 13:10:45.000 EDT",
        ConfigUtility.formatDisplayTimestamp(timestamp));
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
    environment.getPropertySources().remove(
        StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources().remove(
        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    final Properties sourceProperties = new Properties();
    sourceProperties.setProperty("app.dir", "/tmp/nm302-env-test");
    sourceProperties.setProperty("nm302.test.value", "${app.dir}/data");
    environment.getPropertySources().addFirst(
        new PropertiesPropertySource("nm302Test", sourceProperties));

    final Properties properties =
        PropertyUtility.loadEnvironmentProperties(environment);

    assertEquals("/tmp/nm302-env-test/data",
        properties.getProperty("nm302.test.value"));
  }

  /**
   * Verifies REST base URLs are normalized after host allowlist validation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRestBaseUrlValidationAllowsConfiguredHost()
    throws Exception {

    final Properties properties = new Properties();
    properties.setProperty("base.url",
        "https://terminology.example.org/ncim-server-rest/");
    properties.setProperty(ConfigUtility.REST_CLIENT_ALLOWED_HOSTS_PROPERTY,
        "terminology.example.org");

    assertEquals("https://terminology.example.org/ncim-server-rest",
        ConfigUtility.getRestBaseUrl(properties));
    assertEquals(
        "https://terminology.example.org/ncim-server-rest/security/logout/dummy",
        ConfigUtility.getRestUrl(properties, "security/logout/dummy"));
  }

  /**
   * Verifies REST base URL validation rejects unexpected targets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRestBaseUrlValidationRejectsUnexpectedTargets()
    throws Exception {

    final Properties properties = new Properties();
    properties.setProperty("base.url",
        "http://169.254.169.254/umls-server-rest");

    assertIllegalArgument(() -> ConfigUtility.getRestBaseUrl(properties));

    properties.setProperty("base.url",
        "http://user@localhost:8080/umls-server-rest");
    assertIllegalArgument(() -> ConfigUtility.getRestBaseUrl(properties));

    properties.setProperty("base.url",
        "http://localhost:8080/umls-server-rest");
    assertIllegalArgument(
        () -> ConfigUtility.getRestUrl(properties, "http://example.org/path"));
  }

  /**
   * Verifies MySQL JDBC URLs are allowed after host allowlist validation.
   *
   * @throws Exception the exception
   */
  @Test
  public void testJdbcUrlValidationAllowsConfiguredHost()
    throws Exception {

    final Properties properties = new Properties();
    properties.setProperty(ConfigUtility.DATABASE_ALLOWED_HOSTS_PROPERTY,
        "db.example.org");
    final String jdbcUrl =
        "jdbc:mysql://db.example.org:3306/ncimdb?serverTimezone=UTC";

    assertEquals(jdbcUrl, ConfigUtility.validateJdbcUrl(jdbcUrl,
        "jakarta.persistence.jdbc.url", properties));
    assertEquals("jdbc:mysql://db.example.org:3306/?serverTimezone=UTC",
        ConfigUtility.validateJdbcServerUrl(
            "jdbc:mysql://db.example.org:3306/?serverTimezone=UTC",
            "jakarta.persistence.jdbc.url", properties));
  }

  /**
   * Verifies JDBC URL validation rejects unexpected targets.
   *
   * @throws Exception the exception
   */
  @Test
  public void testJdbcUrlValidationRejectsUnexpectedTargets()
    throws Exception {

    final Properties properties = new Properties();
    properties.setProperty(ConfigUtility.DATABASE_ALLOWED_HOSTS_PROPERTY,
        "db.example.org");

    assertIllegalArgument(() -> ConfigUtility.validateJdbcUrl(
        "jdbc:mysql://169.254.169.254:3306/ncimdb",
        "jakarta.persistence.jdbc.url", properties));
    assertIllegalArgument(() -> ConfigUtility.validateJdbcUrl(
        "jdbc:h2:mem:test", "jakarta.persistence.jdbc.url", properties));
    assertIllegalArgument(() -> ConfigUtility.validateJdbcUrl(
        "jdbc:mysql://db.example.org:3306/?serverTimezone=UTC",
        "jakarta.persistence.jdbc.url", properties));
  }

  /**
   * Verifies release QA target validation rejects command-like values.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRunQaChecksRejectsUnexpectedTarget() throws Exception {
    assertIllegalArgument(() -> ConfigUtility.runQaChecks(null, null, null,
        "MRCONSO;rm -rf /", null, null));
  }

  /**
   * Verifies release QA paths must stay under source.data.dir.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRunQaChecksRejectsMetaOutsideSourceDataDir()
    throws Exception {

    final File dir = Files.createTempDirectory("nm-command-validation").toFile();
    try {
      final File sourceDataDir = new File(dir, "data");
      final File binDir = new File(dir, "bin");
      final File outsideMetaDir = new File(dir, "outside/META");
      final File previousMetaDir = new File(sourceDataDir, "mr/2025/META");
      Files.createDirectories(sourceDataDir.toPath());
      Files.createDirectories(binDir.toPath());
      Files.createDirectories(outsideMetaDir.toPath());
      Files.createDirectories(previousMetaDir.toPath());

      assertIllegalArgument(() -> ConfigUtility.runQaChecks(sourceDataDir,
          binDir, outsideMetaDir, "MRCONSO", previousMetaDir, null));
    } finally {
      ConfigUtility.deleteDirectory(dir);
    }
  }

  /**
   * Verifies release QA execution uses the fixed script and validated arguments.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRunQaChecksExecutesValidatedScript() throws Exception {
    assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

    final File dir = Files.createTempDirectory("nm-command-execution").toFile();
    try {
      final File sourceDataDir = new File(dir, "data");
      final File binDir = new File(dir, "bin");
      final File metaDir = new File(sourceDataDir, "release/2026/META");
      final File previousMetaDir = new File(sourceDataDir, "mr/2025/META");
      Files.createDirectories(binDir.toPath());
      Files.createDirectories(metaDir.toPath());
      Files.createDirectories(previousMetaDir.toPath());

      final File script = new File(binDir, "qa_checks.csh");
      Files.write(script.toPath(),
          Arrays.asList("#!/bin/sh", "printf 'cwd=%s\\n' \"$(pwd)\"",
              "printf 'dir=%s\\n' \"$1\"",
              "printf 'target=%s\\n' \"$2\"",
              "printf 'prev=%s\\n' \"$3\"", "test \"$2\" = MRCONSO"),
          StandardCharsets.UTF_8);
      assertTrue(script.setExecutable(true));

      final String output = ConfigUtility.runQaChecks(sourceDataDir, binDir,
          metaDir, "MRCONSO", previousMetaDir, null);

      assertEquals("cwd=" + binDir.getCanonicalPath() + "\n" + "dir="
          + metaDir.getCanonicalPath() + "\n" + "target=MRCONSO\n" + "prev="
          + previousMetaDir.getCanonicalPath() + "\n", output);
    } finally {
      ConfigUtility.deleteDirectory(dir);
    }
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
   * Asserts that a runnable throws IllegalArgumentException.
   *
   * @param runnable the runnable
   * @throws Exception for unexpected exceptions
   */
  private static void assertIllegalArgument(final ThrowingRunnable runnable)
    throws Exception {
    try {
      runnable.run();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /**
   * Runnable that can throw checked exceptions.
   */
  private interface ThrowingRunnable {

    /**
     * Runs the operation.
     *
     * @throws Exception the exception
     */
    void run() throws Exception;
  }

  /**
   * Verifies Spring-style properties ignore legacy run.config settings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesIgnoreLegacyRunConfig() throws Exception {
    final File tempFile = File.createTempFile("config", ".properties");
    try (Writer writer = Files.newBufferedWriter(tempFile.toPath(),
        StandardCharsets.UTF_8)) {
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
   * Verifies merge sort reads and writes text with an explicit UTF-8 charset.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMergeSortedFilesPreservesUtf8Text() throws Exception {
    final File dir = Files.createTempDirectory("nm304-utf8-merge").toFile();
    try {
      final File file1 = new File(dir, "one.txt");
      final File file2 = new File(dir, "two.txt");
      Files.write(file1.toPath(), Arrays.asList("béta", "éclair"),
          StandardCharsets.UTF_8);
      Files.write(file2.toPath(), Arrays.asList("alpha", "delta"),
          StandardCharsets.UTF_8);

      final File merged = ConfigUtility.mergeSortedFiles(file1, file2,
          String::compareTo, dir, "id|name");
      final List<String> lines =
          Files.readAllLines(merged.toPath(), StandardCharsets.UTF_8);

      assertEquals(Arrays.asList("id|name", "alpha", "béta", "delta",
          "éclair"), lines);
    } finally {
      ConfigUtility.deleteDirectory(dir);
    }
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
