/*
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Loads Boot-style application properties into a legacy {@link Properties}
 * structure for compatibility with existing callers.
 */
public final class SpringConfigPropertiesLoader {

  /** Base application properties resource. */
  private static final String APPLICATION_PROPERTIES = "application.properties";

  /** Profile-specific application properties resource pattern. */
  private static final String PROFILE_APPLICATION_PROPERTIES =
      "application-%s.properties";

  /** Utility constructor. */
  private SpringConfigPropertiesLoader() {
    // n/a
  }

  /**
   * Loads application properties from the classpath.
   *
   * @return the merged application properties, or {@code null} if the base
   *         resource is not present
   * @throws IOException if the properties cannot be loaded
   */
  public static Properties load() throws IOException {

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
    if (ConfigUtility.isEmpty(configuredProfiles)) {
      configuredProfiles =
          mergedProperties.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    }

    if (ConfigUtility.isEmpty(configuredProfiles)) {
      configuredProfiles =
          environment.getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME);
    }
    if (ConfigUtility.isEmpty(configuredProfiles)) {
      configuredProfiles = mergedProperties
          .getProperty(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, "local");
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
}
