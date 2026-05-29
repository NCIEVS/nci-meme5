/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest.ncimeta;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.client.IntegrationTestClientRest;
import com.wci.umls.server.rest.client.MetaEditingClientRest;
import com.wci.umls.server.rest.client.ProjectClientRest;
import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.test.helpers.RestIntegrationSupport;

/**
 * Integration test for REST content service.
 */
@Ignore("Base NCI-META REST editing fixture; subclasses are explicit")
public class MetaEditingServiceRestIT extends RestIntegrationSupport  {

  /** The service. */
  protected static ContentClientRest contentService;

  /** The security service. */
  protected static SecurityClientRest securityService;

  /** the project service */
  protected static ProjectClientRest projectService;

  /** The meta editing service */
  protected static MetaEditingClientRest metaEditingService;

  /**  The test service. */
  protected static IntegrationTestClientRest testService;

  /** The properties. */
  protected static Properties properties;

  /** The test password. */
  protected static String testUser;

  /** The test password. */
  protected static String testPassword;

  /** The test password. */
  protected static String adminUser;

  /** The test password. */
  protected static String adminPassword;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // instantiate properties
    properties = loadRestProperties();

    // instantiate required services
    metaEditingService = new MetaEditingClientRest(properties);
    projectService = new ProjectClientRest(properties);
    testService = new IntegrationTestClientRest(properties);
    contentService = new ContentClientRest(properties);
    securityService = new SecurityClientRest(properties);

    final RestCredentials credentials = restCredentials(properties);
    testUser = credentials.getViewerUser();
    testPassword = credentials.getViewerPassword();
    adminUser = credentials.getAdminUser();
    adminPassword = credentials.getAdminPassword();

  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {

    /**
     * Prerequisites
     */

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // do nothing
  }

  /**
   * Remove a copied concept if it was created by a test.
   *
   * @param concept the concept
   * @param project the project
   * @param authToken the auth token
   * @throws Exception the exception
   */
  protected void removeCopiedConcept(Concept concept, Project project,
    String authToken) throws Exception {
    if (concept == null || project == null || authToken == null) {
      return;
    }
    if (contentService.getConcept(concept.getId(), project.getId(),
        authToken) != null) {
      testService.removeConcept(concept.getId(), true, authToken);
    }
  }

  /**
   * Logout only when authentication reached the point of creating a token.
   *
   * @param authToken the auth token
   * @throws Exception the exception
   */
  protected void logoutIfAuthenticated(String authToken) throws Exception {
    logoutIfAuthenticated(securityService, authToken);
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // do nothing
  }

}
