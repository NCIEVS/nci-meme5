/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest.meta;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.test.helpers.RestIntegrationSupport;

/**
 * Integration test for REST content service.
 */
public class ContentServiceRestIT extends RestIntegrationSupport {

  /** The service. */
  protected static ContentClientRest contentService;

  /** The security service. */
  protected static SecurityClientRest securityService;

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
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // do nothing
  }

}
