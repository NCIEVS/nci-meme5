/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.client;

import java.util.Properties;

import com.wci.umls.server.helpers.ConfigUtility;

/**
 * A root rest client.
 */
public class RootClientRest {

  /**
   * Validate not empty.
   *
   * @param parameter the parameter
   * @param parameterName the parameter name
   */
  @SuppressWarnings("static-method")
  protected void validateNotEmpty(String parameter, String parameterName) {
    if (parameter == null) {
      throw new IllegalArgumentException("Parameter " + parameterName
          + " must not be null.");
    }
    if (parameter.isEmpty()) {
      throw new IllegalArgumentException("Parameter " + parameterName
          + " must not be empty.");
    }
  }

  /**
   * Validate not empty.
   *
   * @param parameter the parameter
   * @param parameterName the parameter name
   */
  @SuppressWarnings("static-method")
  protected void validateNotEmpty(Long parameter, String parameterName) {
    if (parameter == null) {
      throw new IllegalArgumentException("Parameter " + parameterName
          + " must not be null.");
    }
  }

  /**
   * Returns the validated REST base URL.
   *
   * @param config the client config
   * @return the base URL
   */
  @SuppressWarnings("static-method")
  protected String getBaseUrl(final Properties config) {
    return ConfigUtility.getRestBaseUrl(config);
  }

}
