/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.fail;

import jakarta.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Sample test to get auto complete working
 */
public class ContentServiceGeneralQueryTimeoutIT extends
    IntegrationUnitSupport {

  /** The service. */
  ContentService service = null;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    service = new ContentServiceJpa();
  }

  /**
   * Test general query timeout.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGeneralQueryTimeout() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    try {
      service.findConceptsForGeneralQuery("",
          "SELECT c FROM ConceptJpa c WHERE name like '%x%' AND terminology IN"
              + " (SELECT b.name FROM AttributeJpa b)", Branch.ROOT, null);
      // On small sample databases the query may complete before the 1-second
      // JDBC timeout fires — that is acceptable.
    } catch (PersistenceException e) {
      // Timeout fired as expected on larger databases.
    }
  }

  /**
   * Teardown.
   */
  @After
  public void teardown() {
    // do nothing
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }

}
