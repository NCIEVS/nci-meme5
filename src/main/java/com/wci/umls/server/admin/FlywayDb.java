/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.MigrationUtility;
import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Admin tool for running explicit Flyway database migration commands.
 *
 * <p>The supported commands map to the Gradle admin tasks:
 * <ul>
 *   <li>{@code info}: read-only status check. Shows available, applied,
 *       pending, failed, and current-version migration state.</li>
 *   <li>{@code migrate}: applies pending migrations in version order. Use this
 *       for a fresh empty database, where Flyway should run the baseline schema
 *       migration and then later migrations.</li>
 *   <li>{@code validate}: read-only consistency check. Verifies migration files
 *       still match the checksums recorded in {@code flyway_schema_history}.</li>
 *   <li>{@code baseline}: marks an existing populated legacy database as being
 *       at the configured baseline version. This avoids replaying the baseline
 *       schema creation migration over a database that already has the MEME
 *       schema.</li>
 * </ul>
 *
 * <p>Normal flow:
 * <ul>
 *   <li>Fresh empty database: run {@code adminFlywayMigrate}.</li>
 *   <li>Existing populated legacy database: run {@code adminFlywayBaseline}
 *       once, then use {@code adminFlywayMigrate} for later migrations.</li>
 *   <li>Use {@code adminFlywayInfo} and {@code adminFlywayValidate} before or
 *       after either path to inspect state and catch drift.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminFlywayInfo
 *   ./gradlew adminFlywayMigrate
 *   ./gradlew adminFlywayValidate
 *   ./gradlew adminFlywayBaseline -Pflyway.baseline.confirm=true
 * </pre>
 */
public class FlywayDb {

  /** Supported command names. */
  private static final Set<String> COMMANDS =
      Set.of("migrate", "info", "validate", "baseline");

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(FlywayDb.class.getName());

  /**
   * Main entry point.
   *
   * @param args optional command argument
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {

    final String command = resolveCommand(args);
    final Properties properties = PropertyUtility.getProperties();
    LOG.info("Starting Flyway command: " + command);

    switch (command) {
      case "migrate":
        MigrationUtility.migrate(properties);
        break;
      case "info":
        LOG.info(System.lineSeparator() + MigrationUtility.info(properties));
        break;
      case "validate":
        MigrationUtility.validate(properties);
        break;
      case "baseline":
        requireBaselineConfirmation(properties);
        MigrationUtility.baseline(properties);
        break;
      default:
        throw new IllegalArgumentException(usage());
    }

    LOG.info("Finished Flyway command: " + command);
  }

  /**
   * Resolves the requested Flyway command.
   *
   * @param args optional command argument
   * @return the command
   */
  private static String resolveCommand(String[] args) {
    String command = System.getProperty("flyway.command");
    if (isBlank(command) && args != null && args.length > 0) {
      command = args[0];
    }
    if (isBlank(command)) {
      throw new IllegalArgumentException(usage());
    }
    command = command.toLowerCase(Locale.ROOT);
    if (!COMMANDS.contains(command)) {
      throw new IllegalArgumentException(usage());
    }
    return command;
  }

  /**
   * Requires an explicit confirmation flag before marking a legacy database as
   * baselined.
   *
   * @param properties the application properties
   */
  private static void requireBaselineConfirmation(final Properties properties) {
    String confirmed = System.getProperty("flyway.baseline.confirm");
    if (isBlank(confirmed) && properties != null) {
      confirmed = properties.getProperty("flyway.baseline.confirm");
    }
    if (!"true".equalsIgnoreCase(confirmed)) {
      throw new IllegalArgumentException(
          "Refusing to run Flyway baseline without "
              + "-Pflyway.baseline.confirm=true. This command is intended "
              + "only for existing populated legacy databases.");
    }
  }

  /**
   * Returns usage text.
   *
   * @return the usage text
   */
  private static String usage() {
    return "System property flyway.command must be one of: "
        + String.join(", ", COMMANDS);
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
}
