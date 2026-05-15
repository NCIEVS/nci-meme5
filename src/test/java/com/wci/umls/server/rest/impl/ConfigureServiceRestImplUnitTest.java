/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;

import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Unit tests for configure bootstrap behavior.
 */
public class ConfigureServiceRestImplUnitTest {

  /**
   * Reset relevant system properties after each test.
   */
  @After
  public void teardown() throws Exception {
    System.clearProperty("app.dir");
    System.clearProperty("config.legacy.runConfig.enabled");
    System.clearProperty("spring.profiles.active");
    System.clearProperty("user.home");
    System.clearProperty("run.config.umls");
    System.clearProperty("run.config.label");
    PropertyUtility.resetProperties();
  }

  /**
   * Verifies the configure flow prefers Spring-style application properties.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetStartingConfigurationPrefersSpringProperties()
    throws Exception {
    System.setProperty("app.dir", "/tmp/nm278-configure-test");
    System.setProperty("spring.profiles.active", "local");

    final ConfigureServiceRestImpl service = new ConfigureServiceRestImpl();
    final Properties properties = service.getStartingConfiguration();

    assertNotNull(properties);
    assertEquals("/tmp/nm278-configure-test",
        properties.getProperty("app.dir"));
    assertEquals("/tmp/nm278-configure-test/data",
        properties.getProperty("source.data.dir"));
    assertEquals("http://localhost:8080/umls-server-rest",
        properties.getProperty("base.url"));
  }

  /**
   * Verifies configure bootstrap fails cleanly if Spring properties are missing.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetStartingConfigurationRequiresSpringProperties()
    throws Exception {
    final ConfigureServiceRestImpl service = new ConfigureServiceRestImpl() {
      @Override
      Properties getSpringStartingConfiguration() throws Exception {
        return null;
      }
    };

    try {
      service.getStartingConfiguration();
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Could not load starting configuration"));
      return;
    }
    throw new AssertionError("Expected starting configuration load to fail");
  }

  /**
   * Verifies configure persists a usable local config file under the new bridge.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConfigureWritesUsableLocalConfigFile() throws Exception {
    final File tempHome =
        new File(System.getProperty("java.io.tmpdir"),
            "nm278-configure-home-" + System.nanoTime());
    final File appDir = new File(tempHome, "app");
    assertTrue(tempHome.mkdirs());
    assertTrue(appDir.mkdirs());

    System.setProperty("user.home", tempHome.getAbsolutePath());
    System.setProperty("spring.profiles.active", "local");
    System.setProperty("app.dir", appDir.getAbsolutePath());
    PropertyUtility.resetProperties();

    final ConfigureServiceRestImpl service = new ConfigureServiceRestImpl() {
      @Override
      void initializeConfiguredDatabase() throws Exception {
        // no-op for focused config-write coverage
      }
    };

    final HashMap<String, String> parameters = new HashMap<>();
    parameters.put("app.dir", appDir.getAbsolutePath());
    parameters.put("source.data.dir", new File(appDir, "data").getAbsolutePath());
    parameters.put("hibernate.search.backend.directory.root",
        new File(appDir, "indexes").getAbsolutePath());
    parameters.put("jakarta.persistence.jdbc.url",
        "jdbc:mysql://127.0.0.1:3306/testdb");
    parameters.put("jakarta.persistence.jdbc.user", "testuser");
    parameters.put("jakarta.persistence.jdbc.password", "testpass");

    service.configure(parameters);

    final File configFile = new File(PropertyUtility.getLocalConfigFile());
    assertTrue(configFile.exists());
    assertFalse(configFile.length() == 0L);

    final Properties writtenProperties = new Properties();
    try (FileReader reader = new FileReader(configFile)) {
      writtenProperties.load(reader);
    }

    assertEquals(appDir.getAbsolutePath(),
        writtenProperties.getProperty("app.dir"));
    assertEquals(new File(appDir, "data").getAbsolutePath(),
        writtenProperties.getProperty("source.data.dir"));
    assertEquals("jdbc:mysql://127.0.0.1:3306/testdb",
        writtenProperties.getProperty("jakarta.persistence.jdbc.url"));
    assertEquals("jdbc:mysql://127.0.0.1:3306/testdb",
        PropertyUtility.getProperties().getProperty("jakarta.persistence.jdbc.url"));
    assertEquals("testuser", PropertyUtility.getProperties()
        .getProperty("jakarta.persistence.jdbc.user"));
  }
}
