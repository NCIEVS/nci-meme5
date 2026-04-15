/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.algorithm;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Test to run all of the insertion loader algorithm tests.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InsertionLoaderAlgorithmsIT extends IntegrationUnitSupport {

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // Do nothing
  }

  /**
   * Test metadata loader normal use.
   *
   * @throws Exception the exception
   */
  @Test
  public void test1MetadataLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    MetadataLoaderAlgorithmIT algo = new MetadataLoaderAlgorithmIT();
    algo.setup();
    algo.testMetadataLoader();
    algo.teardown();    
  }
  
  /**
   * Test atom loader.
   *
   * @throws Exception the exception
   */
  @Test
  public void test2AtomLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    
    AtomLoaderAlgorithmIT algo2 = new AtomLoaderAlgorithmIT();
    algo2.setup();
    algo2.testAtomLoader();
    algo2.teardown();       
  }  

  /**
   * Test relationship loader.
   *
   * @throws Exception the exception
   */
  @Test
  public void test3RelationshipLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    
    RelationshipLoaderAlgorithmIT algo2 = new RelationshipLoaderAlgorithmIT();
    algo2.setup();
    algo2.testRelationshipLoader();
    algo2.teardown();       
  }    
  
  /**
   * Test context loader.
   *
   * @throws Exception the exception
   */
  @Test
  public void test4ContextLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    
    ContextLoaderAlgorithmIT algo2 = new ContextLoaderAlgorithmIT();
    algo2.setup();
    algo2.testContextLoader();
    algo2.teardown();       
  }    
  
  /**
   * Test semantic type loader.
   *
   * @throws Exception the exception
   */
  @Test
  public void test5SemanticTypeLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    
    SemanticTypeLoaderAlgorithmIT algo2 = new SemanticTypeLoaderAlgorithmIT();
    algo2.setup();
    algo2.testSemanticTypeLoader();
    algo2.teardown();       
  }    
  
  /**
   * Test attribute loader.
   *
   * @throws Exception the exception
   */
  @Test
  public void test6AttributeLoader() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    
    AttributeLoaderAlgorithmIT algo2 = new AttributeLoaderAlgorithmIT();
    algo2.setup();
    algo2.testAttributeLoader();
    algo2.teardown();       
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
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }

}
