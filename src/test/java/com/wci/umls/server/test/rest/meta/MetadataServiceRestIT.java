/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest.meta;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.wci.umls.server.rest.client.MetadataClientRest;
import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.test.helpers.RestIntegrationSupport;

/**
 * Implementation of the "Metadata Service REST Normal Use" Test Cases.
 */
@Ignore("Base REST fixture; subclasses provide runnable tests")
public class MetadataServiceRestIT extends RestIntegrationSupport {

  /** The service. */
  protected static MetadataClientRest metadataService;

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
    metadataService = new MetadataClientRest(properties);
    securityService = new SecurityClientRest(properties);

    /**
     * Test prerequisites Terminology SNOMEDCT exists in database Terminology
     * ICD9CM exists in database The run.config.umls has "viewer.user" and
     * "viewer.password" specified
     */

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
