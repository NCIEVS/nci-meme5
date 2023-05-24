/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.test.content;

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
import com.wci.umls.server.helpers.XmlSerializationTester;
import com.wci.umls.server.jpa.ModelUnitSupport;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetJpa;
import com.wci.umls.server.jpa.content.ConceptSubsetMemberJpa;
import com.wci.umls.server.jpa.helpers.IndexedFieldTester;
import com.wci.umls.server.jpa.helpers.NullableFieldTester;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptSubset;
import com.wci.umls.server.model.content.ConceptSubsetMember;

/**
 * Unit testing for {@link ConceptSubsetMemberJpa}.
 */
public class ConceptSubsetMemberJpaUnitTest extends ModelUnitSupport {

  /** The model object to test. */
  private ConceptSubsetMemberJpa object;

  /** Test fixture. */
  private Concept concept1;

  /** Test fixture. */
  private Concept concept2;

  /** Test fixture. */
  private ConceptSubset subset1;

  /** Test fixture. */
  private ConceptSubset subset2;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup.
   */
  @Before
  public void setup() {
    object = new ConceptSubsetMemberJpa();

    concept1 = new ConceptJpa();
    concept1.setId(1L);
    concept1.setTerminologyId("1");
    concept1.setTerminology("1");
    concept1.setVersion("1");
    concept1.setName("1");
    concept2 = new ConceptJpa();
    concept2.setId(2L);
    concept2.setTerminologyId("2");
    concept2.setTerminology("2");
    concept2.setVersion("2");
    concept1.setName("2");

    subset1 = new ConceptSubsetJpa();
    subset1.setId(1L);
    subset1.setTerminologyId("1");
    subset1.setTerminology("1");
    subset1.setVersion("1");
    subset1.setName("2");
    subset2 = new ConceptSubsetJpa();
    subset2.setId(2L);
    subset2.setTerminologyId("2");
    subset2.setTerminology("2");
    subset2.setVersion("2");
    subset2.setName("2");
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
    tester.exclude("memberId");
    tester.exclude("memberTerminologyId");
    tester.exclude("memberTerminology");
    tester.exclude("memberVersion");
    tester.exclude("memberName");
    tester.exclude("subsetId");
    tester.exclude("subsetTerminologyId");
    tester.exclude("subsetTerminology");
    tester.exclude("subsetVersion");
    tester.exclude("subsetName");
    tester.exclude("type");
    tester.exclude("name");
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
    tester.include("suppressible");
    tester.include("obsolete");
    tester.include("branch");
    tester.include("terminology");
    tester.include("terminologyId");
    tester.include("member");
    tester.include("subset");

    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
    assertTrue(tester.testIdentityFieldEquals());
    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
    assertTrue(tester.testNonIdentityFieldEquals());
    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
    assertTrue(tester.testIdentityFieldNotEquals());
    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
    assertTrue(tester.testIdentityFieldHashcode());
    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
    assertTrue(tester.testNonIdentityFieldHashcode());
    tester.proxy(Concept.class, 1, new ConceptJpa(concept1, false));
    tester.proxy(Concept.class, 2, new ConceptJpa(concept2, false));
    tester.proxy(ConceptSubset.class, 1, new ConceptSubsetJpa(subset1, false));
    tester.proxy(ConceptSubset.class, 2, new ConceptSubsetJpa(subset2, false));
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
    tester.proxy(Concept.class, 1, concept1);
    tester.proxy(Concept.class, 2, concept2);
    tester.proxy(ConceptSubset.class, 1, subset1);
    tester.proxy(ConceptSubset.class, 2, subset2);
    assertTrue(tester.testCopyConstructorCollection(ConceptSubsetMember.class));
  }

  /**
   * Test xml transient fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testXmlTransient() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    object.setMember(concept1);
    object.setSubset(subset1);
    String xml = ConfigUtility.getStringForGraph(object);
    assertTrue(xml.contains("<subsetId>"));
    assertTrue(xml.contains("<subsetTerminologyId>"));
    assertTrue(xml.contains("<subsetTerminology>"));
    assertTrue(xml.contains("<subsetVersion>"));
    assertTrue(xml.contains("<subsetName>"));
    assertFalse(xml.contains("<subset>"));
    assertTrue(xml.contains("<memberId>"));
    assertTrue(xml.contains("<memberTerminologyId>"));
    assertTrue(xml.contains("<memberTerminology>"));
    assertTrue(xml.contains("<memberVersion>"));
    assertTrue(xml.contains("<memberName>"));
    assertFalse(xml.contains("<member>"));

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
    tester.proxy(Concept.class, 1, concept1);
    tester.proxy(Concept.class, 2, concept2);
    tester.proxy(ConceptSubset.class, 1, subset1);
    tester.proxy(ConceptSubset.class, 2, subset2);
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
    tester.include("timestamp");
    tester.include("lastModified");
    tester.include("lastModifiedBy");
    tester.include("suppressible");
    tester.include("obsolete");
    tester.include("published");
    tester.include("publishable");
    tester.include("terminology");
    tester.include("terminologyId");
    tester.include("version");
    tester.include("member");
    tester.include("subset");
    assertTrue(tester.testNotNullFields());
  }

  /**
   * Test field indexing.
   *
   * @throws Exception the exception
   */
  @Test
  public void testModelIndexedFields041() throws Exception {
    Logger.getLogger(getClass()).debug("TEST " + name.getMethodName());

    // Test analyzed fields
    IndexedFieldTester tester = new IndexedFieldTester(object);
    tester.include("membername");
    tester.include("subsetname");
    assertTrue(tester.testAnalyzedIndexedFields());

    // Test non analyzed fields
    tester = new IndexedFieldTester(object);
    tester.include("id");
    tester.include("lastModified");
    tester.include("lastModifiedBy");
    tester.include("suppressible");
    tester.include("obsolete");
    tester.include("published");
    tester.include("publishable");
    tester.include("terminologyId");
    tester.include("terminology");
    tester.include("version");
    tester.include("branch");
    tester.include("subsetid");
    tester.include("subsetterminologyId");
    tester.include("subsetterminology");
    tester.include("subsetversion");
    tester.include("memberId");
    tester.include("memberTerminologyId");
    tester.include("memberTerminology");
    tester.include("memberVersion");

    tester.include("timestamp");
    tester.include("memberNameSort");
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
//