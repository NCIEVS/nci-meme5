/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.test.workflow;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.Project;
import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.ProxyTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.ModelUnitSupport;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.jpa.helpers.IndexedFieldTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.jpa.workflow.WorkflowEpochJpa;
import com.wci.umls.server.jpa.workflow.WorklistJpa;
import com.wci.umls.server.model.workflow.Worklist;

/**
 * Unit testing for {@link WorkflowEpochJpa}.
 */
public class WorklistJpaUnitTest extends ModelUnitSupport {

  /** The model object to test. */
  private WorklistJpa object;

  /** The fixture p1. */
  private Project p1;

  /** The fixture p2. */
  private Project p2;

  /** The fixture l1. */
  @SuppressWarnings("rawtypes")
  private List l1;

  /** The fixture l2. */
  @SuppressWarnings("rawtypes")
  private List l2;

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
  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    object = new WorklistJpa();

    final ProxyTester tester2 = new ProxyTester(new ProjectJpa());
    p1 = (ProjectJpa) tester2.createObject(1);
    p2 = (ProjectJpa) tester2.createObject(2);

    l1 = new ArrayList<>();
    l1.add(null);
    l2 = new ArrayList<>();
    l2.add(null);
    l2.add(null);

    object.setProject(p1);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    GetterSetterTester tester = new GetterSetterTester(object);
    tester.exclude("projectId");
    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("description");
    tester.include("name");
    tester.include("project");
    tester.include("authors");
    tester.include("reviewers");
    tester.include("team");
    tester.include("workflowBinName");
    tester.include("epoch");

    tester.proxy(List.class, 1, l1);
    tester.proxy(List.class, 2, l2);
    tester.proxy(Project.class, 1, p1);
    tester.proxy(Project.class, 2, p2);

    assertTrue(tester.testIdentityFieldEquals());
    assertTrue(tester.testNonIdentityFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentityFieldHashcode());
    assertTrue(tester.testNonIdentityFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test deep copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCollectionCopy() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    CopyConstructorTester tester = new CopyConstructorTester(object);
    tester.proxy(List.class, 1, l1);
    tester.proxy(List.class, 2, l2);
    tester.proxy(Project.class, 1, p1);
    tester.proxy(Project.class, 2, p2);
    assertTrue(tester.testCopyConstructorCollection(Worklist.class));

  }

  /**
   * Test XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlSerialization() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    XmlSerializationTester tester = new XmlSerializationTester(object);

    Project p1 = new ProjectJpa();
    p1.setId(1L);
    tester.proxy(Project.class, 1, p1);
    assertTrue(tester.testXmlSerialization());

    // TODO: need to also test the authors/reviewers and notes features here.

  }

  /**
   * Test not null fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelNotNullField() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());
    NullableFieldTester tester = new NullableFieldTester(object);
    tester.include("timestamp");
    tester.include("lastModified");
    tester.include("lastModifiedBy");
    tester.include("name");
    tester.include("description");
    tester.include("workflowStatus");
    tester.include("number");
    tester.include("epoch");
    assertTrue(tester.testNotNullFields());
  }

  /**
   * Test field indexing.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelIndexedFields() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    // Test analyzed fields
    IndexedFieldTester tester = new IndexedFieldTester(object);
    tester.include("authors");
    tester.include("reviewers");
    tester.include("name");
    tester.include("workflowState");
    assertTrue(tester.testAnalyzedIndexedFields());

    // Test non analyzed fields
    tester = new IndexedFieldTester(object);
    tester.include("nameSort");
    tester.include("projectId");
    tester.include("lastModified");
    tester.include("lastModifiedBy");
    tester.include("team");
    tester.include("epoch");
    tester.include("workflowStatus");
    tester.include("workflowBinName");
    tester.include("workflowStateSort");
    tester.include("reviewersSort");
    tester.include("authorsSort");
    tester.include("timestamp");

    assertTrue(tester.testNotAnalyzedIndexedFields());
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
