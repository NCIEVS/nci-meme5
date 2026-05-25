/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;

/**
 * Admin tool which rebuilds Lucene indexes based on hibernate-search
 * annotations.
 *
 * <p>Usage:
 * <pre>
 *   source config/local/setenv.sh
 *   # Normal direct admin mode; Tomcat should be stopped.
 *   ./gradlew adminReindex -Pindexed.objects=ProjectJpa -Pserver=false
 *
 *   # Leave indexed.objects blank to rebuild all Lucene indexes.
 *   ./gradlew adminReindex -Pserver=false
 *
 *   # Direct Java invocation still reads Spring-style application.properties
 *   # and environment variables.
 *   java -Dindexed.objects=ConceptJpa -Dserver=false -cp ... \
 *       com.wci.umls.server.admin.LuceneReindex
 * </pre>
 */
public class LuceneReindex {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(LuceneReindex.class.getName());

  /**
   * Main entry point.
   *
   * @param args ignored
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {

    final String indexedObjects = System.getProperty("indexed.objects");
    final boolean server =
        Boolean.parseBoolean(System.getProperty("server", "false"));

    LOG.info("Lucene reindexing");
    LOG.info("  Indexed objects : " + indexedObjects);
    LOG.info("  Expect server up: " + server);

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
      ContentServiceRestImpl contentService = new ContentServiceRestImpl();
      contentService.luceneReindex(indexedObjects, authToken);
    } else {
      LOG.info("Running against server");
      ContentClientRest client = new ContentClientRest(properties);
      client.luceneReindex(indexedObjects, authToken);
    }
    System.exit(0);
  }
}
