package com.wci.umls.server.jpa.helpers.meta;

import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.CopyConstructorTester;
import com.wci.umls.server.helpers.EqualsHashcodeTester;
import com.wci.umls.server.helpers.GetterSetterTester;
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.jpa.meta.TermTypeJpa;
import com.wci.umls.server.model.meta.TermType;

/**
 * Unit testing for {@link TermTypeJpa}.
 */
public class ModelUnit039Test {

  /** The model object to test. */
  private TermTypeJpa object;

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
    object = new TermTypeJpa();
  }

  /**
   * Test getter and setter methods of model object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelGetSet039() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelGetSet039");
    GetterSetterTester tester = new GetterSetterTester(object);
    tester.test();
  }

  /**
   * Test equals and hascode methods.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelEqualsHashcode039() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelEqualsHashcode039");
    EqualsHashcodeTester tester = new EqualsHashcodeTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("terminologyVersion");
    tester.include("codeVariantType");
    tester.include("hierarchicalType");
    tester.include("nameVariantType");
    tester.include("obsolete");
    tester.include("style");
    tester.include("suppressible");
    tester.include("usageType");


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
  public void testModelCopy039() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelCopy039");
    CopyConstructorTester tester = new CopyConstructorTester(object);

    assertTrue(tester.testCopyConstructor(TermType.class));
  }

  /**
   * Test XML serialization.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelXmlSerialization039() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelXmlSerialization039");
    XmlSerializationTester tester = new XmlSerializationTester(object);
    assertTrue(tester.testXmlSerialization());
  }

  /**
   * Test not null fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelNotNullField039() throws Exception {
    Logger.getLogger(getClass()).debug("TEST testModelNotNullField039");
    NullableFieldTester tester = new NullableFieldTester(object);
    tester.include("abbreviation");
    tester.include("expandedForm");
    tester.include("terminology");
    tester.include("terminologyVersion");
    tester.include("hierarchicalType");
    tester.include("obsolete");
    tester.include("suppressible");

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
