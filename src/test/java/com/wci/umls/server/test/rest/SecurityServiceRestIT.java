/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.rest;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.test.helpers.RestIntegrationSupport;

/**
 * The Class SecurityServiceRestIT.
 */
@Ignore("Base REST fixture; subclasses provide runnable tests")
public class SecurityServiceRestIT extends RestIntegrationSupport {
  /** The service. */
  protected static SecurityClientRest service;

  /** The properties. */
  protected static Properties properties;

  /** The viewer user password. */
  protected static String viewerUserName;

  /** The viewer user password. */
  protected static String viewerUserPassword;

  /** The admin user password. */
  protected static String adminUserName;

  /** The admin user password. */
  protected static String adminUserPassword;

  /** The bad user password. */
  protected static String badUserName;

  /** The bad user password. */
  protected static String badUserPassword;

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // get the properties
    properties = loadRestProperties();

    // instantiate the service
    service = new SecurityClientRest(properties);

    /**
     * Prerequisites
     */

    final RestCredentials credentials = restCredentials(properties);
    viewerUserName = credentials.getViewerUser();
    viewerUserPassword = credentials.getViewerPassword();
    adminUserName = credentials.getAdminUser();
    adminUserPassword = credentials.getAdminPassword();

    // bad user must be specified
    badUserName = requireProperty(properties, "bad.user");
    badUserPassword = requireProperty(properties, "bad.password");

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
