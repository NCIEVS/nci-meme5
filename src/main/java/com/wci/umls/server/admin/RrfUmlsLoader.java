/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.algo.RrfLoaderAlgorithm;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;

/**
 * Admin tool which loads UMLS RRF data into a database.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadRrfUmls -Pterminology=MTH -Pversion=2023AA \
 *       -Pprefix=MR -Pinput.dir=/data/umls -Pedit.mode=false -Pserver=false -Pmode=create
 * </pre>
 */
public class RrfUmlsLoader extends AbstractLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(RrfUmlsLoader.class.getName());

  @Override
  public void run() throws Exception {
    final String terminology = System.getProperty("terminology");
    final String version = System.getProperty("version");
    final String prefix = System.getProperty("prefix", "MR");
    final String inputDir = System.getProperty("input.dir");
    final boolean editMode =
        Boolean.parseBoolean(System.getProperty("edit.mode", "false"));
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));
    final String mode = System.getProperty("mode");

    LOG.info("RRF UMLS Terminology Loader");
    LOG.info("  Terminology     : " + terminology);
    LOG.info("  Version         : " + version);
    LOG.info("  Input directory : " + inputDir);
    LOG.info("  Edit Mode       : " + editMode);
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

    final String style = editMode
        ? RrfLoaderAlgorithm.Style.META_EDIT.toString()
        : RrfLoaderAlgorithm.Style.META_BROWSE.toString();
    final String authToken =
        AdminUtility.authenticateAdmin(properties, serverRunning);

    if (!serverRunning) {
      LOG.info("Running directly");
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.loadTerminologyRrf(terminology, version, style, prefix,
          inputDir, authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.loadTerminologyRrf(terminology, version, style, prefix, inputDir,
          authToken);
    }
  }

  /** Main entry point. */
  public static void main(String[] args) throws Exception {
    new RrfUmlsLoader().run();
    System.exit(0);
  }
}
