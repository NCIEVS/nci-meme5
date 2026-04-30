/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

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
    System.clearProperty("config.legacy.runConfig.enabled");
    System.clearProperty("spring.profiles.active");
    System.clearProperty("run.config.umls");
    System.clearProperty("run.config.label");
    ConfigUtility.resetConfigProperties();
  }

  /**
   * Verifies runtime label override no longer depends on packaged label.prop.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRuntimeConfigLabelOverride() throws Exception {
    System.setProperty("run.config.label", "ncim");
    assertEquals("ncim", ConfigUtility.getConfigLabel());
  }

  /**
   * Verifies Boot-style classpath properties are loaded and resolved.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesBridge() throws Exception {
    System.setProperty("app.dir", "/tmp/nm278-config-test");
    System.setProperty("spring.profiles.active", "local");

    final Properties properties = SpringConfigPropertiesLoader.load();
    assertNotNull(properties);
    assertEquals("/tmp/nm278-config-test", properties.getProperty("app.dir"));
    assertEquals("/tmp/nm278-config-test/data",
        properties.getProperty("source.data.dir"));
    assertEquals("http://localhost:8080/umls-server-rest",
        properties.getProperty("base.url"));
    assertEquals("local", properties.getProperty("spring.profiles.active"));
  }

  /**
   * Verifies Spring-style properties win over legacy run.config settings.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplicationPropertiesPrecedeLegacyRunConfig() throws Exception {
    final File tempFile = File.createTempFile("config", ".properties");
    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("base.url=http://legacy.example\n");
    }
    System.setProperty("app.dir", "/tmp/nm278-config-test");
    System.setProperty("spring.profiles.active", "local");
    System.setProperty("config.legacy.runConfig.enabled", "true");
    System.setProperty("run.config.umls", tempFile.getAbsolutePath());

    final Properties properties = ConfigUtility.getConfigProperties();
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
    System.setProperty("spring.profiles.active", "local");

    final Map<String, String> homeDirs = ConfigUtility.getHomeDirs();
    assertEquals("/tmp/nm278-home/bin", homeDirs.get("bin"));
    assertEquals("/tmp/nm278-home/config", homeDirs.get("config"));
    assertEquals("/tmp/nm278-home/data", homeDirs.get("data"));
    assertEquals("/tmp/nm278-home/lvg", homeDirs.get("lvg"));
  }
}
