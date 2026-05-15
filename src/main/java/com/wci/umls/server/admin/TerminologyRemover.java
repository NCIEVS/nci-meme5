/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.model.algo.ReleaseInfo;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.rest.ContentServiceRest;
import com.wci.umls.server.jpa.services.rest.HistoryServiceRest;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.client.HistoryClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.rest.impl.HistoryServiceRestImpl;

/**
 * Admin tool which removes a terminology and its release info from the database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminRemoveTerminology -Pterminology=SNOMEDCT_US -Pversion=latest -Pserver=false
 * </pre>
 */
public class TerminologyRemover {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(TerminologyRemover.class.getName());

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

    LOG.info("Starting removing terminology");
    LOG.info("  terminology = " + terminology);
    LOG.info("  version = " + version);

    final Properties properties = PropertyUtility.getProperties();
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

    final String authToken =
        AdminUtility.authenticateAdmin(properties, serverRunning);

    if (!serverRunning) {
      LOG.info("Running directly");
      LOG.info("  Remove concepts");
      ContentServiceRest contentService = new ContentServiceRestImpl();
      contentService.removeTerminology(terminology, version, authToken);

      LOG.info("  Remove release info");
      HistoryServiceRest historyService = new HistoryServiceRestImpl();
      for (final ReleaseInfo info : historyService
          .getReleaseHistory(terminology, authToken).getObjects()) {
        HistoryServiceRest historyService2 = new HistoryServiceRestImpl();
        if (info.getTerminology().equals(terminology)
            && info.getVersion().equals(version)) {
          historyService2.removeReleaseInfo(info.getId(), authToken);
        }
      }
    } else {
      LOG.info("Running against server");
      LOG.info("  Remove concepts");
      ContentClientRest contentService = new ContentClientRest(properties);
      contentService.removeTerminology(terminology, version, authToken);

      LOG.info("  Remove release info");
      HistoryClientRest historyService = new HistoryClientRest(properties);
      for (final ReleaseInfo info : historyService
          .getReleaseHistory(terminology, authToken).getObjects()) {
        if (info.getTerminology().equals(terminology)
            && info.getVersion().equals(version)) {
          historyService.removeReleaseInfo(info.getId(), authToken);
        }
      }
    }
    LOG.info("done ...");
    System.exit(0);
  }
}
