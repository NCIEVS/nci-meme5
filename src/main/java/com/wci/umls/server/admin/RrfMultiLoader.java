/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.algo.RrfLoaderAlgorithm;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.services.SecurityService;

/**
 * Admin tool which loads multiple RRF terminologies (no full Metathesaurus).
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadRrfMulti -Pprefix=MR -Pinput.dir=/data/rrf -Pserver=false -Pmode=create
 * </pre>
 */
public class RrfMultiLoader extends AbstractLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(RrfMultiLoader.class.getName());

  @Override
  public void run() throws Exception {
    final String prefix = System.getProperty("prefix", "MR");
    final String inputDir = System.getProperty("input.dir");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));
    final String mode = System.getProperty("mode");

    LOG.info("RRF Multi Terminology Loader");
    LOG.info("  Input directory : " + inputDir);
    LOG.info("  Expect server   : " + server);
    LOG.info("  Mode            : " + mode);

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

    if ("create".equals(mode)) {
      createDb(serverRunning);
    }

    SecurityService service = new SecurityServiceJpa();
    String authToken =
        service.authenticate(properties.getProperty("admin.user"),
            properties.getProperty("admin.password")).getAuthToken();
    service.close();

    if (!serverRunning) {
      LOG.info("Running directly");
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.loadTerminologyRrf("", "",
          RrfLoaderAlgorithm.Style.MULTI.toString(), prefix, inputDir,
          authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.loadTerminologyRrf(null, null,
          RrfLoaderAlgorithm.Style.MULTI.toString(), prefix, inputDir,
          authToken);
    }
  }

  /** Main entry point. */
  public static void main(String[] args) throws Exception {
    new RrfMultiLoader().run();
  }
}
