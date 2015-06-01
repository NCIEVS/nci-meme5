/*
 * Copyright 2015 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.examples;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.helpers.SearchResult;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.jpa.helpers.PfscParameterJpa;

/**
 * Example demonstrating finding descriptors by query.
 */
public class FindCodesByStringQueryTest extends ExampleSupport {

  /** The auth token. */
  private static String authToken;

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Override
  @Before
  public void setup() throws Exception {

    // authentication
    authToken = securityClient.authenticate(testUser, testPassword);

  }

  /**
   * Demonstrates how to find a SNOMED codes with search strings.
   *
   * @throws Exception the exception
   */
  @Test
  public void findSnomedCodesByStringQueryTest() throws Exception {

    // Terminology/version
    String terminology = "SNOMEDCT_US";
    String version = "2014_09_01";

    // Find codes using a simple query
    Logger.getLogger(getClass()).info("Find codes for 'aspirin'");
    SearchResultList list =
        contentClient.findCodesForQuery(terminology, version, "aspirin", null,
            authToken);
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Find codes using a multi-word query
    Logger.getLogger(getClass()).info("Find codes for 'gestational diabetes'");
    list =
        contentClient.findCodesForQuery(terminology, version,
            "gestational diabetes", null, authToken);
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Find codes using a wildcard query that return many results, page first
    // results
    Logger.getLogger(getClass()).info(
        "Find codes for 'ge*' with page size 10, first page");
    PfscParameterJpa pfsc = new PfscParameterJpa();
    pfsc.setStartIndex(0);
    pfsc.setMaxResults(10);
    list =
        contentClient.findCodesForQuery(terminology, version, "ge*", pfsc,
            authToken);
    Logger.getLogger(getClass()).info(
        "  Total results = " + list.getTotalCount());
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Same test, but this time sort on name
    Logger.getLogger(getClass()).info(
        "Find codes for 'ge*' with page size 10, first page");
    pfsc = new PfscParameterJpa();
    pfsc.setStartIndex(0);
    pfsc.setMaxResults(10);
    pfsc.setSortField("name");
    list =
        contentClient.findCodesForQuery(terminology, version, "ge*", pfsc,
            authToken);
    Logger.getLogger(getClass()).info(
        "  Total results = " + list.getTotalCount());
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

  }

  /**
   * Demonstrates how to find a MSH code by query.
   *
   * @throws Exception the exception
   */
  @Test
  public void findMshCodesByStringQueryTest() throws Exception {

    // Terminology/version
    String terminology = "MSH";
    String version = "2015_2014_09_08";

    // Find codes using a simple query
    Logger.getLogger(getClass()).info("Find codes for 'aspirin'");
    SearchResultList list =
        contentClient.findCodesForQuery(terminology, version, "aspirin", null,
            authToken);
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Find codes using a multi-word query
    Logger.getLogger(getClass()).info("Find codes for 'gestational diabetes'");
    list =
        contentClient.findCodesForQuery(terminology, version,
            "gestational diabetes", null, authToken);
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Find codes using a wildcard query that return many results, page first
    // results
    Logger.getLogger(getClass()).info(
        "Find codes for 'ge*' with page size 10, first page");
    PfscParameterJpa pfsc = new PfscParameterJpa();
    pfsc.setStartIndex(0);
    pfsc.setMaxResults(10);
    list =
        contentClient.findCodesForQuery(terminology, version, "ge*", pfsc,
            authToken);
    Logger.getLogger(getClass()).info(
        "  Total results = " + list.getTotalCount());
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

    // Same test, but this time sort on name
    Logger.getLogger(getClass()).info(
        "Find codes for 'ge*' with page size 10, first page");
    pfsc = new PfscParameterJpa();
    pfsc.setStartIndex(0);
    pfsc.setMaxResults(10);
    pfsc.setSortField("name");
    list =
        contentClient.findCodesForQuery(terminology, version, "ge*", pfsc,
            authToken);
    Logger.getLogger(getClass()).info(
        "  Total results = " + list.getTotalCount());
    for (SearchResult result : list.getObjects()) {
      Logger.getLogger(getClass()).info("  " + result);
    }

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @Override
  @After
  public void teardown() throws Exception {

    // logout
    securityClient.logout(authToken);
  }

  /**
   * Returns the parameter types.
   *
   * @param parameters the parameters
   * @return the parameter types
   */
  @SuppressWarnings("static-method")
  public Class<?>[] getParameterTypes(Object[] parameters) {
    Class<?>[] types = new Class<?>[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      types[i] = parameters[i].getClass();
    }
    return types;
  }

}
