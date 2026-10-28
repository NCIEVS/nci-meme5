package com.wci.umls.server.helpers;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.wci.umls.server.jpa.services.handlers.Rf2ComputePreferredNameHandler;
import com.wci.umls.server.jpa.services.handlers.RrfComputePreferredNameHandler;
import com.wci.umls.server.services.handlers.ComputePreferredNameHandler;

/** Checks deployment handler selection without opening a database service. */
public class PreferredNameHandlerConfigurationUnitTest {

  private Properties originalProperties;

  @Before
  public void setUp() {
    originalProperties = PropertyUtility.getProperties();
  }

  @After
  public void tearDown() {
    PropertyUtility.setProperties(originalProperties);
  }

  @Test
  public void defaultsToHistoricalNciMemeRrfHandlerForSnomed() throws Exception {
    final Properties properties = loadConfiguration(null);

    assertEquals("DEFAULT", properties.getProperty("compute.preferred.name.handler"));
    assertEquals(RrfComputePreferredNameHandler.class,
        configuredHandler(properties, "DEFAULT").getClass());
  }

  @Test
  public void allowsNativeRf2DeploymentsToEnableSnomedHandlers() throws Exception {
    final Properties properties = loadConfiguration("DEFAULT,SNOMEDCT,SNOMEDCT_US");

    assertEquals("DEFAULT,SNOMEDCT,SNOMEDCT_US",
        properties.getProperty("compute.preferred.name.handler"));
    assertEquals(Rf2ComputePreferredNameHandler.class,
        configuredHandler(properties, "SNOMEDCT_US").getClass());
    assertEquals(Rf2ComputePreferredNameHandler.class,
        configuredHandler(properties, "SNOMEDCT").getClass());
    assertEquals(RrfComputePreferredNameHandler.class,
        configuredHandler(properties, "DEFAULT").getClass());
  }

  private static Properties loadConfiguration(String handlers) throws Exception {
    final StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    if (handlers != null) {
      final Properties overrides = new Properties();
      overrides.setProperty("COMPUTE_PREFERRED_NAME_HANDLERS", handlers);
      environment.getPropertySources().addFirst(
          new PropertiesPropertySource("deployment", overrides));
    }
    environment.getPropertySources().addLast(new PropertiesPropertySource("application",
        PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))));
    final Properties properties = new Properties();
    final String key = "compute.preferred.name.handler";
    properties.setProperty(key, environment.getRequiredProperty(key));
    for (String handler : new String[] {"DEFAULT", "SNOMEDCT", "SNOMEDCT_US"}) {
      final String classKey = key + "." + handler + ".class";
      properties.setProperty(classKey, environment.getRequiredProperty(classKey));
    }
    return properties;
  }

  private static ComputePreferredNameHandler configuredHandler(Properties properties,
    String handlerName) throws Exception {
    PropertyUtility.setProperties(properties);
    final String key = "compute.preferred.name.handler";
    return ConfigUtility.newStandardHandlerInstanceWithConfiguration(key, handlerName,
        ComputePreferredNameHandler.class);
  }
}
