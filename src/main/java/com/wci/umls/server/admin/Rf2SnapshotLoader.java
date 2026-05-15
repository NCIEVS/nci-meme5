/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;

/**
 * Admin tool which loads an RF2 Snapshot of SNOMED CT data into a database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadRf2Snapshot -Pterminology=SNOMEDCT_US -Pversion=latest \
 *       -Pinput.dir=/data/rf2 -Pserver=false -Pmode=create
 * </pre>
 */
public class Rf2SnapshotLoader extends AbstractLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(Rf2SnapshotLoader.class.getName());

  @Override
  public void run() throws Exception {
    final String terminology = System.getProperty("terminology");
    final String version = System.getProperty("version");
    final String inputDir = System.getProperty("input.dir");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));
    final String mode = System.getProperty("mode");

    LOG.info("RF2 Snapshot Terminology Loader");
    LOG.info("  Terminology     : " + terminology);
    LOG.info("  Version         : " + version);
    LOG.info("  Input directory : " + inputDir);
    LOG.info("  Expect server   : " + server);
    LOG.info("  Mode            : " + mode);

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
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.loadTerminologyRf2Snapshot(terminology, version, inputDir,
          authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.loadTerminologyRf2Snapshot(terminology, version, inputDir,
          authToken);
    }
  }

  /** Main entry point. */
  public static void main(String[] args) throws Exception {
    new Rf2SnapshotLoader().run();
    System.exit(0);
  }
}
