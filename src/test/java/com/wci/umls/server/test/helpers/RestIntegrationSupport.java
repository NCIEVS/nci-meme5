/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.util.Properties;

import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.rest.client.SecurityClientRest;

/**
 * Shared setup helpers for REST integration tests.
 */
public class RestIntegrationSupport extends IntegrationUnitSupport {

  /**
   * Load REST properties and verify that the client base URL is available.
   *
   * @return the properties
   * @throws Exception the exception
   */
  protected static Properties loadRestProperties() throws Exception {
    final Properties properties = PropertyUtility.getProperties();
    requireProperty(properties, "base.url");
    return properties;
  }

  /**
   * Return a required integration-test property.
   *
   * @param properties the properties
   * @param key the key
   * @return the value
   * @throws Exception the exception
   */
  protected static String requireProperty(Properties properties, String key)
    throws Exception {
    final String value = properties.getProperty(key);
    if (value == null || value.isEmpty()) {
      throw new Exception(
          "Test prerequisite: " + key + " must be specified");
    }
    return value;
  }

  /**
   * Return the standard viewer and administrator credentials.
   *
   * @param properties the properties
   * @return the credentials
   * @throws Exception the exception
   */
  protected static RestCredentials restCredentials(Properties properties)
    throws Exception {
    return new RestCredentials(requireProperty(properties, "viewer.user"),
        requireProperty(properties, "viewer.password"),
        requireProperty(properties, "admin.user"),
        requireProperty(properties, "admin.password"));
  }

  /**
   * Logout only when authentication reached the point of creating a token.
   *
   * @param securityService the security service
   * @param authToken the auth token
   * @throws Exception the exception
   */
  protected void logoutIfAuthenticated(SecurityClientRest securityService,
    String authToken) throws Exception {
    if (securityService != null && authToken != null) {
      securityService.logout(authToken);
    }
  }

  /**
   * Value object for common REST integration credentials.
   */
  protected static class RestCredentials {

    /** The viewer user. */
    private final String viewerUser;

    /** The viewer password. */
    private final String viewerPassword;

    /** The admin user. */
    private final String adminUser;

    /** The admin password. */
    private final String adminPassword;

    /**
     * Instantiates a {@link RestCredentials}.
     *
     * @param viewerUser the viewer user
     * @param viewerPassword the viewer password
     * @param adminUser the admin user
     * @param adminPassword the admin password
     */
    RestCredentials(String viewerUser, String viewerPassword, String adminUser,
        String adminPassword) {
      this.viewerUser = viewerUser;
      this.viewerPassword = viewerPassword;
      this.adminUser = adminUser;
      this.adminPassword = adminPassword;
    }

    /**
     * Returns the viewer user.
     *
     * @return the viewer user
     */
    public String getViewerUser() {
      return viewerUser;
    }

    /**
     * Returns the viewer password.
     *
     * @return the viewer password
     */
    public String getViewerPassword() {
      return viewerPassword;
    }

    /**
     * Returns the admin user.
     *
     * @return the admin user
     */
    public String getAdminUser() {
      return adminUser;
    }

    /**
     * Returns the admin password.
     *
     * @return the admin password
     */
    public String getAdminPassword() {
      return adminPassword;
    }
  }
}
