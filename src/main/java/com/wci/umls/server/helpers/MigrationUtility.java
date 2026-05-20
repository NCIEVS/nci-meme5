/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

/**
 * Utility class for manually managed Flyway database migrations.
 */
public final class MigrationUtility {

  /** Default Flyway migration locations. */
  public static final String DEFAULT_LOCATIONS =
      "classpath:db.migration,classpath:db/migration";

  /** Default legacy baseline version. */
  public static final String DEFAULT_BASELINE_VERSION = "1.0";

  /** Default legacy baseline description. */
  public static final String DEFAULT_BASELINE_DESCRIPTION =
      "Baseline current MEME schema";

  /**
   * Instantiates an empty {@link MigrationUtility}.
   */
  private MigrationUtility() {
    // n/a
  }

  /**
   * Runs Flyway migrate using application configuration.
   *
   * @throws Exception the exception
   */
  public static void migrate() throws Exception {
    migrate(PropertyUtility.getProperties());
  }

  /**
   * Runs Flyway migrate.
   *
   * @param properties the application properties
   */
  public static void migrate(final Properties properties) {
    buildFlyway(properties).migrate();
  }

  /**
   * Runs Flyway validate using application configuration.
   *
   * @throws Exception the exception
   */
  public static void validate() throws Exception {
    validate(PropertyUtility.getProperties());
  }

  /**
   * Runs Flyway validate.
   *
   * @param properties the application properties
   */
  public static void validate(final Properties properties) {
    buildFlyway(properties).validate();
  }

  /**
   * Runs Flyway baseline using application configuration.
   *
   * @throws Exception the exception
   */
  public static void baseline() throws Exception {
    baseline(PropertyUtility.getProperties());
  }

  /**
   * Runs Flyway baseline.
   *
   * @param properties the application properties
   */
  public static void baseline(final Properties properties) {
    buildFlyway(properties).baseline();
  }

  /**
   * Returns Flyway info using application configuration.
   *
   * @return migration info text
   * @throws Exception the exception
   */
  public static String info() throws Exception {
    return info(PropertyUtility.getProperties());
  }

  /**
   * Returns Flyway info.
   *
   * @param properties the application properties
   * @return migration info text
   */
  public static String info(final Properties properties) {
    final StringBuilder builder = new StringBuilder();
    for (final MigrationInfo info : buildFlyway(properties).info().all()) {
      builder.append(String.format("%s | %s | %s | %s%n",
          String.valueOf(info.getVersion()), info.getType(), info.getState(),
          info.getDescription()));
    }
    if (builder.length() == 0) {
      builder.append("No Flyway migrations found.").append(System.lineSeparator());
    }
    return builder.toString();
  }

  /**
   * Builds a configured Flyway instance.
   *
   * @param properties the application properties
   * @return the configured Flyway instance
   */
  public static Flyway buildFlyway(final Properties properties) {
    final ConnectionProperties connection =
        resolveConnectionProperties(properties);
    return Flyway.configure()
        .dataSource(connection.getJdbcUrl(), connection.getUsername(),
            connection.getPassword())
        .locations(resolveLocations(properties))
        .baselineVersion(getProperty(properties, "flyway.baseline.version",
            DEFAULT_BASELINE_VERSION))
        .baselineDescription(getProperty(properties,
            "flyway.baseline.description", DEFAULT_BASELINE_DESCRIPTION))
        .baselineOnMigrate(false)
        .cleanDisabled(true)
        .load();
  }

  /**
   * Resolves database connection properties for Flyway.
   *
   * @param properties the application properties
   * @return resolved connection properties
   */
  public static ConnectionProperties resolveConnectionProperties(
    final Properties properties) {

    final String jdbcUrl = firstNonBlank(properties, "hibernate.hikari.jdbcUrl",
        "jakarta.persistence.jdbc.url");
    final String username = firstNonBlank(properties, "hibernate.hikari.username",
        "jakarta.persistence.jdbc.user");
    final String password = firstPresent(properties, "hibernate.hikari.password",
        "jakarta.persistence.jdbc.password");

    if (isBlank(jdbcUrl)) {
      throw new IllegalArgumentException(
          "Flyway requires hibernate.hikari.jdbcUrl or "
              + "jakarta.persistence.jdbc.url");
    }
    if (isBlank(username)) {
      throw new IllegalArgumentException(
          "Flyway requires hibernate.hikari.username or "
              + "jakarta.persistence.jdbc.user");
    }
    return new ConnectionProperties(jdbcUrl, username,
        password == null ? "" : password);
  }

  /**
   * Resolves Flyway migration locations.
   *
   * @param properties the application properties
   * @return configured locations
   */
  public static String[] resolveLocations(final Properties properties) {
    final String configured =
        getProperty(properties, "flyway.locations", DEFAULT_LOCATIONS);
    final List<String> locations = new ArrayList<>();
    for (final String location : configured.split(",")) {
      final String trimmed = location.trim();
      if (!trimmed.isEmpty()) {
        locations.add(trimmed);
      }
    }
    if (locations.isEmpty()) {
      return DEFAULT_LOCATIONS.split(",");
    }
    return locations.toArray(new String[0]);
  }

  /**
   * Returns a property value or default.
   *
   * @param properties the application properties
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value
   */
  private static String getProperty(final Properties properties, final String key,
    final String defaultValue) {
    if (properties == null) {
      return defaultValue;
    }
    final String value = properties.getProperty(key);
    return isBlank(value) ? defaultValue : value;
  }

  /**
   * Returns the first non-blank property value.
   *
   * @param properties the application properties
   * @param keys property keys in priority order
   * @return the first non-blank value
   */
  private static String firstNonBlank(final Properties properties,
    final String... keys) {
    if (properties == null) {
      return null;
    }
    for (final String key : keys) {
      final String value = properties.getProperty(key);
      if (!isBlank(value)) {
        return value;
      }
    }
    return null;
  }

  /**
   * Returns the first present property value.
   *
   * @param properties the application properties
   * @param keys property keys in priority order
   * @return the first present value
   */
  private static String firstPresent(final Properties properties,
    final String... keys) {
    if (properties == null) {
      return null;
    }
    for (final String key : keys) {
      if (properties.containsKey(key)) {
        return properties.getProperty(key);
      }
    }
    return null;
  }

  /**
   * Indicates whether a string is blank.
   *
   * @param value the value
   * @return true if blank
   */
  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Resolved Flyway database connection settings.
   */
  public static final class ConnectionProperties {

    /** JDBC URL. */
    private final String jdbcUrl;

    /** Database username. */
    private final String username;

    /** Database password. */
    private final String password;

    /**
     * Instantiates connection properties.
     *
     * @param jdbcUrl the JDBC URL
     * @param username the username
     * @param password the password
     */
    private ConnectionProperties(final String jdbcUrl, final String username,
      final String password) {
      this.jdbcUrl = jdbcUrl;
      this.username = username;
      this.password = password;
    }

    /**
     * Returns the JDBC URL.
     *
     * @return the JDBC URL
     */
    public String getJdbcUrl() {
      return jdbcUrl;
    }

    /**
     * Returns the username.
     *
     * @return the username
     */
    public String getUsername() {
      return username;
    }

    /**
     * Returns the password.
     *
     * @return the password
     */
    public String getPassword() {
      return password;
    }
  }
}
