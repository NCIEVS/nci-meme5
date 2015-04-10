package com.wci.umls.server.jpa.helpers.meta;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.ProxyTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.jpa.meta.SemanticTypeGroupJpa;
import com.wci.umls.server.jpa.meta.SemanticTypeJpa;
import com.wci.umls.server.model.meta.SemanticTypeGroup;

/**
 * Unit testing for {@link SemanticTypeGroupJpa}.
 */
public class ModelUnit037Test {

  /** The model object to test. */
  private SemanticTypeGroupJpa object;

  /** list proxy */
  private List<SemanticTypeJpa> listProxy;

  /** list proxy2 */
  private List<SemanticTypeJpa> listProxy2;

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
    // set up text fixtures
    object = new SemanticTypeGroupJpa();

    ProxyTester tester = new ProxyTester(new SemanticTypeJpa());
    SemanticTypeJpa sty1 = (SemanticTypeJpa) tester.createObject(1);
    SemanticTypeJpa sty2 = (SemanticTypeJpa) tester.createObject(2);
    listProxy = new ArrayList<>();
    listProxy.add(sty1);
    listProxy2 = new ArrayList<>();
    listProxy2.add(sty2);
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet036() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelGetSet036");
    GetterSetterTester tester = new GetterSetterTester(object);
    tester.proxy(List.class, 1, listProxy);
    tester.proxy(List.class, 2, listProxy2);
    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode036() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelEqualsHashcode036");
    EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("terminologyVersion");

    tester.proxy(List.class, 1, listProxy);
    tester.proxy(List.class, 2, listProxy2);

    assertTrue(tester.testIdentitiyFieldEquals());
    assertTrue(tester.testNonIdentitiyFieldEquals());
    assertTrue(tester.testIdentityFieldNotEquals());
    assertTrue(tester.testIdentitiyFieldHashcode());
    assertTrue(tester.testNonIdentitiyFieldHashcode());
    assertTrue(tester.testIdentityFieldDifferentHashcode());
  }

  /**
   * Test copy constructor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelCopy036() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelCopy036");
    CopyConstructorTester tester = new CopyConstructorTester(object);

    tester.proxy(List.class, 1, listProxy);
    tester.proxy(List.class, 2, listProxy2);

    assertTrue(tester.testCopyConstructor(SemanticTypeGroup.class));
  }

  /**
   * Test XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlSerialization036() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelXmlSerialization036");
    XmlSerializationTester tester = new XmlSerializationTester(object);
    // serialization only recovers id and abbreviation
    tester.proxy(List.class, 1, listProxy);
    tester.proxy(List.class, 2, listProxy2);
    assertTrue(tester.testXmlSerialization());
  }

  /**
   * Test not null fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelNotNullField036() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelNotNullField036");
    NullableFieldTester tester = new NullableFieldTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("terminologyVersion");

    assertTrue(tester.testNotNullFields());
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
