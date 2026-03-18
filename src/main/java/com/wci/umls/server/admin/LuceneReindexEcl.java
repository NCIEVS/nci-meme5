/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.services.SecurityService;

/**
 * Admin tool which rebuilds ECL (Expression Constraint Language) indexes.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminReindexEcl -Pterminology=SNOMEDCT_US -Pversion=latest -Pserver=false
 * </pre>
 */
public class LuceneReindexEcl {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(LuceneReindexEcl.class.getName());

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

    LOG.info("ECL indexing");
    LOG.info("  Terminology : " + terminology);
    LOG.info("  Version     : " + version);
    LOG.info("  Expect server up: " + server);

    final Properties properties = ConfigUtility.getConfigProperties();
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

    // Authenticate
    SecurityService service = new SecurityServiceJpa();
    String authToken =
        service.authenticate(properties.getProperty("admin.user"),
            properties.getProperty("admin.password")).getAuthToken();
    service.close();

    if (!serverRunning) {
      LOG.info("Running directly");
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.computeExpressionIndexes(terminology, version, authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.computeExpressionIndexes(terminology, version, authToken);
    }
  }
}
