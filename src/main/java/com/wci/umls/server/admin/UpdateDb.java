/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;

/**
 * Admin tool which updates or creates the database schema to sync it with the
 * JPA model.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminCreateDb              # mode=create
 *   ./gradlew adminUpdateDb              # mode=update
 *   # or directly: java -Dmode=create -cp ... com.wci.umls.server.admin.UpdateDb
 * </pre>
 */
public class UpdateDb {

  /** Logger. */
  private static final Logger LOG = Logger.getLogger(UpdateDb.class.getName());

  /**
   * Main entry point. Reads {@code -Dmode} system property (create|update).
   *
   * @param args ignored
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {

    final String mode = System.getProperty("mode");
    LOG.info("Starting updating database schema...");
    LOG.info("  mode = " + mode);

    if (mode == null || (!mode.equals("update") && !mode.equals("create"))) {
      throw new IllegalArgumentException(
          "System property 'mode' must be 'create' or 'update', got: " + mode);
    }

    Properties config = ConfigUtility.getConfigProperties();
    config.setProperty("hibernate.hbm2ddl.auto", mode);
    config.setProperty("hibernate.listeners.envers.autoRegister", "true");

    // Trigger a JPA event to apply DDL
    new MetadataServiceJpa().close();
    LOG.info("done ...");
  }
}
