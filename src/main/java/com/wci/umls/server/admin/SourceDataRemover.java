/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;

/**
 * Admin tool which removes source data and corresponding terminology from a
 * database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminRemoveSourceData -Pterminology=MY_TERM -Pversion=1.0 -Pserver=false
 * </pre>
 *
 * <p>Note: Direct (non-server) mode is not fully implemented (commented out
 * in the original source). Server mode currently throws an unsupported
 * exception — this is preserved from the original implementation.
 */
public class SourceDataRemover {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(SourceDataRemover.class.getName());

  /**
   * Main entry point.
   *
   * @param args ignored
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    final String terminology = System.getProperty("terminology");
    final String version = System.getProperty("version");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));

    LOG.info("Starting removing terminology and source data");
    LOG.info("  terminology = " + terminology);
    LOG.info("  version = " + version);

    final boolean serverRunning = ConfigUtility.isServerActive();
    LOG.info("Server status detected:  " + (!serverRunning ? "DOWN" : "UP"));

    if (serverRunning && !server) {
      throw new IllegalStateException(
          "Admin tool expects server to be down, but server is running");
    }
    if (!serverRunning && server) {
      throw new IllegalStateException(
          "Admin tool expects server to be running, but server is down");
    }

    if (!serverRunning) {
      LOG.info("Running directly");
      // Direct (JPA) removal not yet fully implemented
      LOG.info("Note: direct JPA-mode source data removal is not yet implemented.");
    } else {
      LOG.info("Running against server");
      throw new UnsupportedOperationException(
          "Running against the server is not supported at this time.");
    }

    LOG.info("done ...");
  }
}
