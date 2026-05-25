/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Component;

/**
 * Central utility for application property access.
 */
@Component
public class PropertyUtility {

  /** Base application properties resource. */
  private static final String APPLICATION_PROPERTIES = "application.properties";

  /** Profile-specific application properties resource pattern. */
  private static final String PROFILE_APPLICATION_PROPERTIES =
      "application-%s.properties";

  /** The logger. */
  private static final Logger LOGGER =
      Logger.getLogger(PropertyUtility.class.getName());

  /** The environment. */
  @Autowired
  private Environment env;

  /** Cached properties. */
  private static Properties properties = null;

  /** Cache for derived subsets. */
  private static Map<String, Properties> cache = new HashMap<>();

  /**
   * Initializes the property cache from the Spring environment.
   *
   * @throws Exception the exception
   */
  @PostConstruct
  private void init() throws Exception {
    LOGGER.info("  INIT property utility");
    setProperties(loadEnvironmentProperties(env));
    LOGGER.info("Loaded application.properties configuration from Spring environment");
  }

  /**
   * Returns all configured properties.
   *
   * @return the properties
   */
  public static Properties getProperties() {
    if (properties == null) {
      try {
        setProperties(loadStandaloneProperties());
      } catch (Exception e) {
        throw new RuntimeException("Unable to load application properties", e);
      }
    }
    return properties;
  }

  /**
   * Sets the cached properties.
   *
   * @param props the properties
   */
  public static void setProperties(final Properties props) {
    properties = props;
    cache.clear();
  }

  /**
   * Resets cached properties.
   */
  public static void resetProperties() {
    properties = null;
    cache.clear();
  }

  /**
   * Clears cached properties.
   */
  public static void clearProperties() {
    resetProperties();
  }

  /**
   * Returns a property value.
   *
   * @param key the property key
   * @return the property value
   */
  public static String getProperty(final String key) {
    final Properties props = getProperties();
    return props == null ? null : props.getProperty(key);
  }

  /**
   * Sets a property value.
   *
   * @param key the property key
   * @param value the property value
   */
  public static void setProperty(final String key, final String value) {
    final Properties props = getProperties();
    if (props != null) {
      props.setProperty(key, value);
      cache.clear();
    }
  }

  /**
   * Loads application properties from classpath resources.
   *
   * @return the resolved application properties, or null if unavailable
   * @throws IOException if the properties cannot be loaded
   */
  public static Properties loadApplicationProperties() throws IOException {
    return loadApplicationResourceProperties();
  }

  /**
   * Get the config label.
   *
   * @return the label
   * @throws Exception the exception
   */
  public static String getConfigLabel() throws Exception {
    String label = "umls";
    final Properties labelProp = new Properties();

    String runtimeLabel = System.getProperty("run.config.label");
    if (isEmpty(runtimeLabel)) {
      runtimeLabel = System.getenv("RUN_CONFIG_LABEL");
    }
    if (!isEmpty(runtimeLabel)) {
      LOGGER.info("  run.config.label runtime override = " + runtimeLabel);
      return runtimeLabel;
    }

    try (InputStream input =
        PropertyUtility.class.getResourceAsStream("/label.prop")) {
      if (input != null) {
        labelProp.load(input);
        final String candidateLabel =
            labelProp.getProperty("run.config.label");
        if (candidateLabel != null
            && !candidateLabel.equals("${run.config.label}")) {
          label = candidateLabel;
        }
      } else {
        LOGGER.info("  label.prop resource cannot be found, using default");
      }
    }

    LOGGER.info("  run.config.label = " + label);
    return label;
  }

  /**
   * The get local config file.
   *
   * @return the local config file
   * @throws Exception the exception
   */
  public static String getLocalConfigFile() throws Exception {
    return getLocalConfigFolder() + "config.properties";
  }

  /**
   * Gets the local config folder.
   *
   * @return the local config folder
   * @throws Exception the exception
   */
  public static String getLocalConfigFolder() throws Exception {
    return System.getProperty("user.home") + "/.term-server/" + getConfigLabel()
        + "/";
  }

  /**
   * Returns the ui config properties.
   *
   * @return the ui config properties
   * @throws Exception the exception
   */
  public static Properties getUiProperties() throws Exception {
    final Properties config = getProperties();
    final Properties p = new Properties();
    if (config == null) {
      return p;
    }
    for (final Object prop : config.keySet()) {
      final String str = prop.toString();

      if (str.startsWith("deploy.") || str.equals("base.url")
          || (str.startsWith("security") && str.contains("url"))) {
        p.put(prop, config.getProperty(prop.toString()));
      }

      if (str.contains("enabled")) {
        p.put(prop, config.getProperty(prop.toString()));
      }
    }
    return p;
  }

  /**
   * Return properties with the specified prefix.
   *
   * @param prefix the prefix of the properties to return
   * @param removePrefix Should the prefix be removed from returned keys
   * @return the properties with the specified prefix
   * @throws Exception the exception
   */
  public static Properties getPrefixedProperties(final String prefix,
    final boolean removePrefix) throws Exception {

    final String cacheKey = prefix + removePrefix;
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }

    final Properties propertiesSubset = new Properties();
    final Properties config = getProperties();
    if (config != null) {
      for (final Object keyObject : config.keySet()) {
        String key = keyObject.toString();
        final String originalKey = key;
        if (key.startsWith(prefix + ".")) {
          if (removePrefix) {
            key = key.substring((prefix + ".").length());
          }
          propertiesSubset.put(key, config.getProperty(originalKey));
        }
      }
    }

    cache.put(cacheKey, propertiesSubset);
    return propertiesSubset;
  }

  /**
   * Returns the home dirs of the operating environment.
   *
   * @return the home dirs
   * @throws Exception the exception
   */
  public static Map<String, String> getHomeDirs() throws Exception {
    final Map<String, String> map = new HashMap<>();

    final Properties props = getProperties();
    final String appDir = props == null ? null : props.getProperty("app.dir");

    final String dir;
    if (!isEmpty(appDir)) {
      dir = FilenameUtils.separatorsToUnix(appDir);
    } else {
      String configFile = null;
      final java.net.URL url = PropertyUtility.class.getResource("/config.properties");
      if (url != null) {
        configFile = url.getPath();
      } else if (new File(getLocalConfigFile()).exists()) {
        configFile = getLocalConfigFile();
      }

      if (configFile != null) {
        dir = FilenameUtils
            .separatorsToUnix(new File(configFile).getParentFile().getParent());
      } else {
        throw new Exception("Unable to determine home directories from configuration");
      }
    }

    for (final String f : new String[] {
        "bin", "config", "data", "lvg"
    }) {
      map.put(f, dir + "/" + f);
    }

    return map;
  }

  /**
   * Loads properties from the Spring environment.
   *
   * @param environment the environment
   * @return the properties
   */
  @SuppressWarnings("rawtypes")
  static Properties loadEnvironmentProperties(final Environment environment) {
    final Properties props = new Properties();
    final MutablePropertySources sources =
        ((AbstractEnvironment) environment).getPropertySources();
    StreamSupport.stream(sources.spliterator(), false)
        .filter(ps -> ps instanceof EnumerablePropertySource)
        .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
        .flatMap(Arrays::stream)
        .distinct().forEach(prop -> {
          final String value = environment.getProperty(prop);
          if (value != null) {
            props.setProperty(prop, value);
          }
        });
    return props;
  }

  /**
   * Loads properties for non-Spring entry points.
   *
   * @return the loaded properties
   * @throws Exception the exception
   */
  private static Properties loadStandaloneProperties() throws Exception {
    Properties props = loadApplicationResourceProperties();
    if (props != null) {
      LOGGER.info("Loaded application.properties configuration");
      return props;
    }

    final InputStream configStream =
        PropertyUtility.class.getResourceAsStream("/config.properties");
    LOGGER.info("Cannot find Spring application.properties resources"
        + ", looking for config.properties in the classpath");
    if (configStream != null) {
      try (InputStream is = configStream) {
        props = new Properties();
        props.load(is);
        return props;
      }
    }

    if (new File(getLocalConfigFile()).exists()) {
      return loadLegacyConfigFile(getLocalConfigFile());
    }

    return null;
  }

  /**
   * Loads application properties from the classpath.
   *
   * @return the merged application properties, or null if unavailable
   * @throws IOException if the properties cannot be loaded
   */
  private static Properties loadApplicationResourceProperties() throws IOException {

    final ClassPathResource baseResource =
        new ClassPathResource(APPLICATION_PROPERTIES);
    if (!baseResource.exists()) {
      return null;
    }

    final StandardEnvironment environment = new StandardEnvironment();
    final Properties mergedProperties = new Properties();
    mergedProperties.putAll(PropertiesLoaderUtils.loadProperties(baseResource));

    final List<String> profiles =
        resolveProfiles(environment, mergedProperties);
    for (final String profile : profiles) {
      final ClassPathResource profileResource = new ClassPathResource(
          String.format(PROFILE_APPLICATION_PROPERTIES, profile));
      if (profileResource.exists()) {
        mergedProperties.putAll(PropertiesLoaderUtils.loadProperties(profileResource));
      }
    }

    final MutablePropertySources propertySources = new MutablePropertySources();
    propertySources.addLast(new MapPropertySource("systemProperties",
        environment.getSystemProperties()));
    propertySources.addLast(new SystemEnvironmentPropertySource("systemEnvironment",
        environment.getSystemEnvironment()));
    propertySources.addLast(new PropertiesPropertySource("applicationConfig",
        mergedProperties));

    final PropertySourcesPropertyResolver resolver =
        new PropertySourcesPropertyResolver(propertySources);
    final Properties resolvedProperties = new Properties();
    final Set<String> propertyNames =
        new LinkedHashSet<>(mergedProperties.stringPropertyNames());

    for (final String propertyName : propertyNames) {
      final String value = resolver.getProperty(propertyName);
      if (value != null) {
        resolvedProperties.setProperty(propertyName,
            resolver.resolvePlaceholders(value));
      }
    }

    if (!profiles.isEmpty()) {
      resolvedProperties.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME,
          String.join(",", profiles));
    }
    return resolvedProperties;
  }

  /**
   * Resolves the active profiles using Spring conventions.
   *
   * @param environment the environment
   * @param mergedProperties the merged base properties
   * @return the ordered active profiles
   */
  private static List<String> resolveProfiles(StandardEnvironment environment,
    Properties mergedProperties) {

    String configuredProfiles =
        environment.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    if (isEmpty(configuredProfiles)) {
      configuredProfiles =
          mergedProperties.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    }

    if (isEmpty(configuredProfiles)) {
      configuredProfiles =
          environment.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
    }
    if (isEmpty(configuredProfiles)) {
      configuredProfiles = mergedProperties
          .getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
    }
    if (isEmpty(configuredProfiles)) {
      return new ArrayList<>();
    }

    final List<String> profiles = new ArrayList<>();
    for (final String profile : configuredProfiles.split(",")) {
      final String trimmed = profile.trim();
      if (!trimmed.isEmpty()) {
        profiles.add(trimmed);
      }
    }
    return profiles;
  }

  /**
   * Loads a legacy config.properties file.
   *
   * @param configFileName the legacy config file path
   * @return the loaded properties
   * @throws Exception the exception
   */
  private static Properties loadLegacyConfigFile(String configFileName)
    throws Exception {
    final Properties props = new Properties();
    try (FileReader in = new FileReader(new File(configFileName))) {
      props.load(in);
    }
    return props;
  }

  /**
   * Indicates whether or not a string is empty.
   *
   * @param str the str
   * @return true if empty
   */
  private static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
  }
}
