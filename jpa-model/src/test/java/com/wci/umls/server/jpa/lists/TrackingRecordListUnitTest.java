/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.lists;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.Project;
import com.wci.umls.server.helpers.ProxyTester;
import com.wci.umls.server.helpers.TrackingRecordList;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.jpa.helpers.TrackingRecordListJpa;
import com.wci.umls.server.jpa.workflow.TrackingRecordJpa;
import com.wci.umls.server.model.workflow.TrackingRecord;

/**
 * Unit testing for {@link TrackingRecordList}.
 */
public class TrackingRecordListUnitTest extends
    AbstractListUnit<TrackingRecord> {

  /** The list1 test fixture . */
  private TrackingRecordList list1;

  /** The list2 test fixture . */
  private TrackingRecordList list2;

  /** The test fixture o1. */
  private TrackingRecord o1;

  /** The test fixture o2. */
  private TrackingRecord o2;

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
    list1 = new TrackingRecordListJpa();
    list2 = new TrackingRecordListJpa();

    ProxyTester tester = new ProxyTester(new TrackingRecordJpa());
    o1 = (TrackingRecord) tester.createObject(1);
    o2 = (TrackingRecord) tester.createObject(2);

    Project p1 = new ProjectJpa();
    p1.setId(1L);
    Project p2 = new ProjectJpa();
    p2.setId(2L);
    o1.setProject(p1);
    o2.setProject(p2);

  }

  /**
   * Test normal use of a list.
   * @throws Exception the exception
   */
  @Test
  public void testNormalUse() throws Exception {
    testNormalUse(list1, list2, o1, o2);
  }

  /**
   * Test degenerate use of a list. Show that the underlying data structure
   * should NOT be manipulated.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testDegenerateUse() throws Exception {
    testDegenerateUse(list1, list2, o1, o2);
  }

  /**
   * Test edge cases of a list.
   * 
   * @throws Exception the exception
   */
  @Test
  public void testEdgeCases() throws Exception {
    testEdgeCases(list1, list2, o1, o2);
  }

  /**
   * Test XML serialization of a list.
   *
   * 
   * @throws Exception the exception
   */
  @Test
  public void testXmlSerialization() throws Exception {
    testXmllSerialization(list1, list2, o1, o2);
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
