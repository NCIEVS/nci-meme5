/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.test.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.ProxyTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.ModelUnitSupport;
import com.wci.umls.server.jpa.actions.AtomicActionJpa;
import com.wci.umls.server.jpa.actions.MolecularActionJpa;
import com.wci.umls.server.jpa.helpers.IndexedFieldTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;

/**
 * Unit testing for {@link AtomicActionJpa}.
 */
public class AtomicActionJpaUnitTest extends ModelUnitSupport {

  /** The model object to test. */
  private AtomicActionJpa object;

  /** The a1. */
  private MolecularAction a1;

  /** The a2. */
  private MolecularAction a2;

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
    object = new AtomicActionJpa();
    ProxyTester tester = new ProxyTester(new MolecularActionJpa());
    a1 = (MolecularActionJpa) tester.createObject(1);
    a2 = (MolecularActionJpa) tester.createObject(2);

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
    tester.exclude("molecularActionId");
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

    tester.include("idType");
    tester.include("className");
    tester.include("collectionClassName");
    tester.include("objectId");
    tester.include("oldValue");
    tester.include("newValue");
    tester.include("field");

    tester.proxy(MolecularAction.class, 1, a1);
    tester.proxy(MolecularAction.class, 2, a2);

    assertTrue(tester.testIdentityFieldEquals());
    assertTrue(tester.testNonIdentityFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentityFieldHashcode());
    assertTrue(tester.testNonIdentityFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    CopyConstructorTester tester = new CopyConstructorTester(object);
    tester.proxy(MolecularAction.class, 1, a1);
    tester.proxy(MolecularAction.class, 2, a2);
    assertTrue(tester.testCopyConstructor(AtomicAction.class));
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

    tester.proxy(MolecularAction.class, 1, a1);
    tester.proxy(MolecularAction.class, 2, a2);

    assertTrue(tester.testXmlSerialization());
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

    tester.include("id");
    tester.include("idType");
    tester.include("className");
    tester.include("objectId");
    tester.include("field");
    tester.include("molecularAction");
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
    // No fields analyzed
    assertTrue(tester.testAnalyzedIndexedFields());

    // Test non analyzed fields
    tester = new IndexedFieldTester(object);
    tester.include("idType");
    tester.include("className");
    tester.include("objectId");
    tester.include("field");
    tester.include("oldValue");
    tester.include("newValue");
    tester.include("molecularActionId");

    assertTrue(tester.testNotAnalyzedIndexedFields());
  }

  /**
   * Test xml transient fields
   *
   * @throws Exception the exception
   */
  @Test
  public void testXmlTransient() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    final AtomicActionJpa action = new AtomicActionJpa(object);
    action.setMolecularAction(a1);
    String xml = ConfigUtility.getStringForGraph(action);

    assertTrue(xml.contains("<molecularActionId>"));
    assertFalse(xml.contains("<molecularAction>"));

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
//