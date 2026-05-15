/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.rest.ContentServiceRest;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;

/**
 * Admin tool which loads a ClaML terminology into a database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadClaml -Pterminology=ICD10 -Pversion=2016 \
 *       -Pinput.file=/data/icd10.xml -Pserver=false -Pmode=create
 * </pre>
 */
public class ClamlLoader extends AbstractLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(ClamlLoader.class.getName());

  @Override
  public void run() throws Exception {
    final String terminology = System.getProperty("terminology");
    final String version = System.getProperty("version");
    final String inputFile = System.getProperty("input.file");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));
    final String mode = System.getProperty("mode");

    LOG.info("ClaML Terminology Loader");
    LOG.info("  Terminology  : " + terminology);
    LOG.info("  Version      : " + version);
    LOG.info("  Input file   : " + inputFile);
    LOG.info("  Expect server: " + server);
    LOG.info("  Mode         : " + mode);

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

    if ("create".equals(mode)) {
      createDb(serverRunning);
    }

    final String authToken =
        AdminUtility.authenticateAdmin(properties, serverRunning);

    if (!serverRunning) {
      LOG.info("Running directly");
      ContentServiceRest contentService = new ContentServiceRestImpl();
      contentService.loadTerminologyClaml(terminology, version, inputFile,
          authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest contentService = new ContentClientRest(properties);
      contentService.loadTerminologyClaml(terminology, version, inputFile,
          authToken);
    }
    LOG.info("done ...");
  }

  /** Main entry point. */
  public static void main(String[] args) throws Exception {
    new ClamlLoader().run();
    System.exit(0);
  }
}
