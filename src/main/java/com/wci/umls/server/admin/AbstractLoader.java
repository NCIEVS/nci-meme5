/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.logging.Logger;

import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Base class for admin tools that load source data.
 * Provides a utility method for recreating the database.
 */
public abstract class AbstractLoader {

  /** Logger. */
  protected static final Logger LOG =
      Logger.getLogger(AbstractLoader.class.getName());

  /**
   * Returns a formatted source-data name from terminology/version pair.
   *
   * @param terminology the terminology
   * @param version the version
   * @return the name
   */
  protected String getName(String terminology, String version) {
    return terminology + " " + version + " source data";
  }

  /**
   * Creates (or drops-and-recreates) the database schema via JPA.
   *
   * @param serverRunning whether the server is currently running
   * @throws Exception the exception
   */
  public void createDb(boolean serverRunning) throws Exception {

    final java.util.Properties properties =
        PropertyUtility.getProperties();

    LOG.info("Recreate database");
    properties.setProperty("hibernate.hbm2ddl.auto", "create");
    String autoRegisterProperty =
        properties.getProperty("hibernate.listeners.envers.autoRegister");
    properties.setProperty("hibernate.listeners.envers.autoRegister", "true");

    // Trigger a JPA event to apply DDL
    com.wci.umls.server.jpa.services.ProjectServiceJpa projectService =
        new com.wci.umls.server.jpa.services.ProjectServiceJpa();
    projectService.close();
    projectService.closeFactory();

    properties.remove("hibernate.hbm2ddl.auto");
    if (autoRegisterProperty == null) {
      properties.remove("hibernate.listeners.envers.autoRegister");
    } else {
      properties.setProperty("hibernate.listeners.envers.autoRegister",
          autoRegisterProperty);
    }

    projectService.openFactory();

    // Re-authenticate and reindex
    final String authToken =
        AdminUtility.authenticateAdmin(properties, serverRunning);

    if (serverRunning) {
      com.wci.umls.server.rest.client.ContentClientRest client =
          new com.wci.umls.server.rest.client.ContentClientRest(properties);
      client.luceneReindex(null, authToken);
    } else {
      com.wci.umls.server.rest.impl.ContentServiceRestImpl contentService =
          new com.wci.umls.server.rest.impl.ContentServiceRestImpl();
      contentService.luceneReindex(null, authToken);
    }
  }

  /**
   * Run the admin operation. Subclasses must implement.
   *
   * @throws Exception the exception
   */
  public abstract void run() throws Exception;
}
