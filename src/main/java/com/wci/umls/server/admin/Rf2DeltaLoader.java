/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.services.SecurityService;

/**
 * Admin tool which loads an RF2 Delta of SNOMED CT data into a database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadRf2Delta -Pterminology=SNOMEDCT_US -Pversion=latest \
 *       -Pinput.dir=/data/rf2 -Pserver=false
 * </pre>
 */
public class Rf2DeltaLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(Rf2DeltaLoader.class.getName());

  /**
   * Main entry point.
   *
   * @param args ignored
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    final String terminology = System.getProperty("terminology");
    final String inputDir = System.getProperty("input.dir");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));

    LOG.info("RF2 Delta Terminology Loader");
    LOG.info("  Terminology     : " + terminology);
    LOG.info("  Input directory : " + inputDir);
    LOG.info("  Expect server   : " + server);

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

    SecurityService service = new SecurityServiceJpa();
    String authToken =
        service.authenticate(properties.getProperty("admin.user"),
            properties.getProperty("admin.password")).getAuthToken();
    service.close();

    if (!serverRunning) {
      LOG.info("Running directly");
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.loadTerminologyRf2Delta(terminology, inputDir, authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.loadTerminologyRf2Delta(terminology, inputDir, authToken);
    }
    System.exit(0);
  }
}
