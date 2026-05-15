/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.util.Properties;

import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.rest.client.SecurityClientRest;
import com.wci.umls.server.services.SecurityService;

/**
 * Shared helpers for admin command-line tools.
 */
final class AdminUtility {

  /**
   * Instantiates an empty {@link AdminUtility}.
   */
  private AdminUtility() {
    // n/a
  }

  /**
   * Authenticates the configured admin user in the correct JVM.
   *
   * @param properties the configuration properties
   * @param serverRunning whether the operation will call a running server
   * @return the auth token
   * @throws Exception the exception
   */
  static String authenticateAdmin(Properties properties, boolean serverRunning)
    throws Exception {

    if (serverRunning) {
      SecurityClientRest securityClient = new SecurityClientRest(properties);
      return securityClient.authenticate(properties.getProperty("admin.user"),
          properties.getProperty("admin.password")).getAuthToken();
    }

    SecurityService service = new SecurityServiceJpa();
    try {
      return service.authenticate(properties.getProperty("admin.user"),
          properties.getProperty("admin.password")).getAuthToken();
    } finally {
      service.close();
    }
  }
}
