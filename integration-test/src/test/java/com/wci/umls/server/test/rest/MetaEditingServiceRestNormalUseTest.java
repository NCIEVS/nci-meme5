/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.wci.umls.server.Project;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.AttributeJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.meta.TerminologyJpa;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.actions.MolecularActionList;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.rest.client.IntegrationTestClientRest;

/**
 * Implementation of the "MetaEditing Service REST Normal Use" Test Cases.
 */
public class MetaEditingServiceRestNormalUseTest
    extends MetaEditingServiceRestTest {

  /** The auth token. */
  private static String authToken;

  /** The project. */
  private static Project project;

  /** The umls terminology. */
  private String umlsTerminology = "UMLS";

  /** The umls version. */
  private String umlsVersion = "latest";

  /**
   * The concept (will be copied from existing concept, to avoid affecting
   * database values.
   */
  private ConceptJpa concept;

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Override
  @Before
  public void setup() throws Exception {

    // authentication (admin for editing permissions)
    authToken =
        securityService.authenticate(adminUser, adminPassword).getAuthToken();

    // ensure there is a concept associated with the project
    ProjectList projects = projectService.getProjects(authToken);
    assertTrue(projects.getCount() > 0);
    project = projects.getObjects().get(0);

    // verify terminology and branch are expected values
    assertTrue(project.getTerminology().equals(umlsTerminology));
    // assertTrue(project.getBranch().equals(Branch.ROOT));

    // Copy existing concept to avoid messing with actual database data.
    concept = new ConceptJpa(contentService.getConcept("C0000294",
        umlsTerminology, umlsVersion, null, authToken), false);
    concept.setId(null);
    concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    concept = (ConceptJpa) testService.addConcept(concept, authToken);
  }

  /**
   * Test add and remove semanticType to concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAddAndRemoveSemanticTypeToConcept() throws Exception {
    Logger.getLogger(getClass()).debug("Start test");

    Logger.getLogger(getClass())
        .info("TEST - Add and remove semantic type to/from " + "C0000294,"
            + umlsTerminology + ", " + umlsVersion + ", " + authToken);

    //
    // Prepare the test and check prerequisites
    //
    // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    Date startDate = DateUtils.round(new Date(), Calendar.SECOND);

    // get the concept
    Concept c =
        contentService.getConcept(concept.getId(), project.getId(), authToken);
    assertNotNull(c);

    // check against project
    // assertTrue(c.getBranch().equals(project.getBranch()));

    // construct a semantic type not present on concept (here, Lipid)
    SemanticTypeComponentJpa semanticType = new SemanticTypeComponentJpa();
    semanticType.setBranch(Branch.ROOT);
    semanticType.setSemanticType("Lipid");
    semanticType.setTerminologyId("TestId");
    semanticType.setTerminology(umlsTerminology);
    semanticType.setVersion(umlsVersion);
    semanticType.setTimestamp(new Date());
    semanticType.setPublishable(true);

    //
    // Test addition
    //

    // add the semantic type to the concept
    ValidationResult v =
        metaEditingService.addSemanticType(project.getId(), c.getId(),
            c.getLastModified().getTime(), semanticType, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check semantic types
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    semanticType = null;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        semanticType = (SemanticTypeComponentJpa) s;
      }
    }
    assertNotNull(semanticType);

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService
        .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    assertTrue(list.getCount() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that two atomic actions exists for add Semantic Type, and update
    // Concept WorkflowStatus
    pfs.setSortField("idType");
    pfs.setAscending(true);

    List<AtomicAction> atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(2, atomicActions.size());
    assertEquals("CONCEPT", atomicActions.get(0).getIdType().toString());
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals("SEMANTIC_TYPE", atomicActions.get(1).getIdType().toString());
    assertNull(atomicActions.get(1).getOldValue());
    assertNotNull(atomicActions.get(1).getNewValue());

    // Verify the log entry exists
    String logEntry =
        projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry
        .contains("ADD_SEMANTIC_TYPE " + semanticType.getSemanticType()));

    //
    // Add second semantic type
    //

    // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    startDate = DateUtils.round(new Date(), Calendar.SECOND);

    // construct a second semantic type not present on concept (here, Enzyme)
    SemanticTypeComponentJpa semanticType2 = new SemanticTypeComponentJpa();
    semanticType2.setBranch(Branch.ROOT);
    semanticType2.setSemanticType("Enzyme");
    semanticType2.setTerminologyId("TestId");
    semanticType2.setTerminology(umlsTerminology);
    semanticType2.setVersion(umlsVersion);
    semanticType2.setTimestamp(new Date());
    semanticType2.setPublishable(true);

    // add the second semantic type to the concept
    v = metaEditingService.addSemanticType(project.getId(), c.getId(),
        c.getLastModified().getTime(), semanticType2, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check semantic types
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    semanticType = null;
    semanticType2 = null;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        semanticType = (SemanticTypeComponentJpa) s;
      }
      if (s.getSemanticType().equals("Enzyme")) {
        semanticType2 = (SemanticTypeComponentJpa) s;
      }
    }
    assertNotNull(semanticType);
    assertNotNull(semanticType2);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));

    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that ONE atomic actions exists for add Semantic Type (Concept
    // Workflow Status was already set during previous addition)
    pfs.setSortField("idType");
    pfs.setAscending(true);

    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(1, atomicActions.size());
    assertEquals("SEMANTIC_TYPE", atomicActions.get(0).getIdType().toString());
    assertNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry
        .contains("ADD_SEMANTIC_TYPE " + semanticType2.getSemanticType()));

    //
    // Test removal
    //

    // remove the first semantic type from the concept
    v = metaEditingService.removeSemanticType(project.getId(), c.getId(),
        c.getLastModified().getTime(), semanticType.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check semantic types
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean semanticTypePresent = false;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        semanticTypePresent = true;
      }
    }
    assertTrue(!semanticTypePresent);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic action exists for remove Semantic Type
    pfs.setAscending(true);

    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, null, authToken).getObjects();
    assertEquals(atomicActions.size(), 1);
    assertEquals(atomicActions.get(0).getIdType().toString(), "SEMANTIC_TYPE");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry
        .contains("REMOVE_SEMANTIC_TYPE " + semanticType.getSemanticType()));

    // remove the second semantic type from the concept (assume verification of
    // MA, atomic actions, and log entry since we just tested those)
    v = metaEditingService.removeSemanticType(project.getId(), c.getId(),
        c.getLastModified().getTime(), semanticType2.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean semanticType2Present = false;
    for (SemanticTypeComponent s : c.getSemanticTypes()) {
      if (s.getSemanticType().equals("Enzyme")) {
        semanticType2Present = true;
      }
    }
    assertTrue(!semanticType2Present);

  }

  /**
   * Test add and remove attribute to concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAddAndRemoveAttributeToConcept() throws Exception {
    Logger.getLogger(getClass()).debug("Start test");

    Logger.getLogger(getClass())
        .info("TEST - Add and remove attribute to/from " + "C0000294,"
            + umlsTerminology + ", " + umlsVersion + ", " + authToken);

    //
    // Prepare the test and check prerequisites
    //
    // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    Date startDate = DateUtils.round(new Date(), Calendar.SECOND);

    // get the concept
    Concept c =
        contentService.getConcept(concept.getId(), project.getId(), authToken);
    assertNotNull(c);

    // construct a attribute not present on concept (here, UMLSRELA)
    AttributeJpa attribute = new AttributeJpa();
    attribute.setBranch(Branch.ROOT);
    attribute.setName("UMLSRELA");
    attribute.setValue("VALUE");
    attribute.setTerminologyId("TestId");
    attribute.setTerminology(umlsTerminology);
    attribute.setVersion(umlsVersion);
    attribute.setTimestamp(new Date());
    attribute.setPublishable(true);

    //
    // Test addition
    //

    // add the attribute to the concept
    ValidationResult v = metaEditingService.addAttribute(project.getId(),
        c.getId(), c.getLastModified().getTime(), attribute, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    attribute = null;
    for (Attribute s : c.getAttributes()) {
      if (s.getName().equals("UMLSRELA")) {
        attribute = (AttributeJpa) s;
      }
    }
    assertNotNull(attribute);

    // verify that alternate ID was created and is correctly formed.
    assertNotNull(attribute.getAlternateTerminologyIds().get(umlsTerminology));
    assertTrue(attribute.getAlternateTerminologyIds().get(umlsTerminology)
        .startsWith("AT"));

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService
        .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    assertTrue(list.getCount() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that two atomic actions exists for add attribute, and update
    // Concept WorkflowStatus

    pfs.setSortField("idType");
    pfs.setAscending(true);

    List<AtomicAction> atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(2, atomicActions.size());
    assertEquals("ATTRIBUTE", atomicActions.get(0).getIdType().toString());
    assertNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals(atomicActions.get(1).getIdType().toString(), "CONCEPT");
    assertNotNull(atomicActions.get(1).getOldValue());
    assertNotNull(atomicActions.get(1).getNewValue());

    // Verify the log entry exists
    String logEntry =
        projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry.contains("ADD_ATTRIBUTE " + attribute.getName()));

    //
    // Add second attribute (also ensures alternateTerminologyId increments
    // correctly)
    //

    // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    startDate = DateUtils.round(new Date(), Calendar.SECOND);

    // construct a second attribute not present on concept (here, UMLSRELA with
    // Value of VALUE2)
    AttributeJpa attribute2 = new AttributeJpa();
    attribute2.setBranch(Branch.ROOT);
    attribute2.setName("UMLSRELA");
    attribute2.setValue("VALUE2");
    attribute2.setTerminologyId("TestId");
    attribute2.setTerminology(umlsTerminology);
    attribute2.setVersion(umlsVersion);
    attribute2.setTimestamp(new Date());
    attribute2.setPublishable(true);

    //
    // add the second attribute to the concept
    //

    // add the attribute to the concept
    v = metaEditingService.addAttribute(project.getId(), c.getId(),
        c.getLastModified().getTime(), attribute2, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check to make sure both attributes are still
    // there
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    attribute = null;
    attribute2 = null;
    for (Attribute s : c.getAttributes()) {
      if (s.getName().equals("UMLSRELA") && s.getValue().equals("VALUE")) {
        attribute = (AttributeJpa) s;
      }
      if (s.getName().equals("UMLSRELA") && s.getValue().equals("VALUE2")) {
        attribute2 = (AttributeJpa) s;
      }
    }
    assertNotNull(attribute);
    assertNotNull(attribute2);

    // verify that alternate ID was created and is correctly formed.
    assertNotNull(attribute2.getAlternateTerminologyIds().get(umlsTerminology));
    assertTrue(attribute2.getAlternateTerminologyIds().get(umlsTerminology)
        .startsWith("AT"));

    // verify that attribute2's alternate ID is different from the first one
    assertNotSame(attribute.getAlternateTerminologyIds().get(umlsTerminology),
        attribute2.getAlternateTerminologyIds().get(umlsTerminology));

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that ONE atomic actions exists for add attribute (Concept Workflow
    // Status was already set during previous addition)
    pfs.setSortField("idType");
    pfs.setAscending(true);

    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(1, atomicActions.size());
    assertEquals("ATTRIBUTE", atomicActions.get(0).getIdType().toString());
    assertNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry.contains("ADD_ATTRIBUTE " + attribute2.getName()));

    //
    // Test removal
    //

    // remove the first attribute from the concept
    v = metaEditingService.removeAttribute(project.getId(), c.getId(),
        c.getLastModified().getTime(), attribute.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean attributePresent = false;
    for (Attribute a : c.getAttributes()) {
      if (a.getName().equals("UMLSRELA") && a.getValue().equals("VALUE")) {
        attributePresent = true;
      }
    }
    assertTrue(!attributePresent);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic action exists for remove Attribute
    pfs.setAscending(true);

    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, null, authToken).getObjects();
    assertEquals(atomicActions.size(), 1);
    assertEquals(atomicActions.get(0).getIdType().toString(), "ATTRIBUTE");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry.contains("REMOVE_ATTRIBUTE " + attribute.getName()));

    // remove the second attribute from the concept (assume verification of MA,
    // atomic actions, and log entry since we just tested those)
    v = metaEditingService.removeAttribute(project.getId(), c.getId(),
        c.getLastModified().getTime(), attribute2.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean attribute2Present = false;
    for (Attribute a : c.getAttributes()) {
      if (a.getName().equals("UMLSRELA") && a.getValue().equals("VALUE2")) {
        attribute2Present = true;
      }
    }
    assertTrue(!attribute2Present);

  }

  /**
   * Test add and remove atom to concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAddAndRemoveAtomToConcept() throws Exception {
    Logger.getLogger(getClass()).debug("Start test");

    Logger.getLogger(getClass())
        .info("TEST - Add and remove atom to/from " + "C0000294,"
            + umlsTerminology + ", " + umlsVersion + ", " + authToken);

    //
    // Prepare the test and check prerequisites
    //
    // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    Date startDate = DateUtils.round(new Date(), Calendar.SECOND);

    // get the concept
    Concept c =
        contentService.getConcept(concept.getId(), project.getId(), authToken);
    assertNotNull(c);

    // construct an atom not present on concept (here, DCB)
    AtomJpa atom = new AtomJpa();
    atom.setBranch(Branch.ROOT);
    atom.setName("DCB");
    atom.setTerminologyId("TestId");
    atom.setTerminology(umlsTerminology);
    atom.setVersion(umlsVersion);
    atom.setTimestamp(new Date());
    atom.setPublishable(true);
    atom.setCodeId("C44314");
    atom.setConceptId(concept.getId().toString());
    atom.setDescriptorId("");
    atom.setLanguage("ENG");
    atom.setTermType("AB");

    //
    // Test addition
    //

    // add the attribute to the concept
    ValidationResult v = metaEditingService.addAtom(project.getId(), c.getId(),
        c.getLastModified().getTime(), atom, false, authToken);
    assertTrue(v.getErrors().isEmpty());

    // retrieve the concept and check attributes
    c = contentService.getConcept(concept.getId(), project.getId(), authToken); 
    
    atom = null;
    for (Atom a : c.getAtoms()) {
      if (a.getName().equals("DCB")) {
        atom = (AtomJpa) a;
      }
    }
    assertNotNull(atom);

    // verify that alternate ID was created and is correctly formed.
    assertNotNull(atom.getAlternateTerminologyIds().get(umlsTerminology));
    assertTrue(atom.getAlternateTerminologyIds().get(umlsTerminology)
        .startsWith("AT"));

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService
        .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    assertTrue(list.getCount() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that two atomic actions exists for add atom, and update
    // Concept WorkflowStatus

    pfs.setSortField("idType");
    pfs.setAscending(true);

    List<AtomicAction> atomicActions = contentService
        .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    assertEquals(2, atomicActions.size());
    assertEquals("ATOM", atomicActions.get(0).getIdType().toString());
    assertNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals(atomicActions.get(1).getIdType().toString(), "CONCEPT");
    assertNotNull(atomicActions.get(1).getOldValue());
    assertNotNull(atomicActions.get(1).getNewValue());

    // Verify the log entry exists
    String logEntry =
        projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry.contains("ADD_ATOM " + atom.getName()));

    // //
    // // Add second attribute (also ensures alternateTerminologyId increments
    // correctly)
    // //
    //
    // // Due to MySQL rounding to the second, we must also round our comparison
    // startDate.
    // startDate = DateUtils.round(new Date(),Calendar.SECOND);
    //
    //
    // // construct a second attribute not present on concept (here, UMLSRELA
    // with Value of VALUE2)
    // AttributeJpa attribute2 = new AttributeJpa();
    // attribute2.setBranch(Branch.ROOT);
    // attribute2.setName("UMLSRELA");
    // attribute2.setValue("VALUE2");
    // attribute2.setTerminologyId("TestId");
    // attribute2.setTerminology(umlsTerminology);
    // attribute2.setVersion(umlsVersion);
    // attribute2.setTimestamp(new Date());
    // attribute2.setPublishable(true);
    //
    // //
    // // add the second attribute to the concept
    // //
    //
    // // add the attribute to the concept
    // v = metaEditingService.addAttribute(project.getId(),
    // c.getId(), c.getLastModified().getTime(), attribute2, false, authToken);
    // assertTrue(v.getErrors().isEmpty());
    //
    // // retrieve the concept and check to make sure both attributes are still
    // there
    // c = contentService.getConcept(concept.getId(), project.getId(),
    // authToken);
    //
    // atom = null;
    // attribute2 = null;
    // for (Attribute s : c.getAttributes()) {
    // if (s.getName().equals("UMLSRELA") && s.getValue().equals("VALUE")) {
    // atom = (AttributeJpa) s;
    // }
    // if (s.getName().equals("UMLSRELA") && s.getValue().equals("VALUE2")) {
    // attribute2 = (AttributeJpa) s;
    // }
    // }
    // assertNotNull(atom);
    // assertNotNull(attribute2);
    //
    //
    // // verify that alternate ID was created and is correctly formed.
    // assertNotNull(attribute2.getAlternateTerminologyIds().get(umlsTerminology));
    // assertTrue(attribute2.getAlternateTerminologyIds().get(umlsTerminology).startsWith("AT"));
    //
    // // verify that attribute2's alternate ID is different from the first one
    // assertNotSame(atom.getAlternateTerminologyIds().get(umlsTerminology),
    // attribute2.getAlternateTerminologyIds().get(umlsTerminology));
    //
    // // verify the molecular action exists
    // pfs = new PfsParameterJpa();
    // pfs.setSortField("lastModified");
    // pfs.setAscending(false);
    // list = contentService
    // .findMolecularActionsForConcept(c.getId(), null, pfs, authToken);
    // assertTrue(list.getCount() > 0);
    // ma = list.getObjects().get(0);
    // assertNotNull(ma);
    // assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    // assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    // assertNotNull(ma.getAtomicActions());
    //
    // // Verify that ONE atomic actions exists for add attribute (Concept
    // Workflow Status was already set during previous addition)
    // pfs.setSortField("idType");
    // pfs.setAscending(true);
    //
    // atomicActions = contentService
    // .findAtomicActions(ma.getId(), null, pfs, authToken).getObjects();
    // assertEquals(1, atomicActions.size());
    // assertEquals("ATTRIBUTE", atomicActions.get(0).getIdType().toString());
    // assertNull(atomicActions.get(0).getOldValue());
    // assertNotNull(atomicActions.get(0).getNewValue());
    //
    // // Verify the log entry exists
    // logEntry =
    // projectService.getLog(project.getId(), c.getId(), 1, authToken);
    // assertTrue(logEntry
    // .contains("ADD_ATTRIBUTE " + attribute2.getName()));

    //
    // Test removal
    //

    // remove the first attribute from the concept
    v = metaEditingService.removeAttribute(project.getId(), c.getId(),
        c.getLastModified().getTime(), atom.getId(), false, authToken);
    assertTrue(v.getErrors().isEmpty());

    c = contentService.getConcept(concept.getId(), project.getId(), authToken);

    boolean attributePresent = false;
    for (Attribute a : c.getAttributes()) {
      if (a.getName().equals("UMLSRELA") && a.getValue().equals("VALUE")) {
        attributePresent = true;
      }
    }
    assertTrue(!attributePresent);

    // verify the molecular action exists
    pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    list = contentService.findMolecularActionsForConcept(c.getId(), null, pfs,
        authToken);
    assertTrue(list.getCount() > 0);
    ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertTrue(ma.getTerminologyId().equals(c.getTerminologyId()));
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic action exists for remove Attribute
    pfs.setAscending(true);

    atomicActions = contentService
        .findAtomicActions(ma.getId(), null, null, authToken).getObjects();
    assertEquals(atomicActions.size(), 1);
    assertEquals(atomicActions.get(0).getIdType().toString(), "ATTRIBUTE");
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNull(atomicActions.get(0).getNewValue());

    // Verify the log entry exists
    logEntry = projectService.getLog(project.getId(), c.getId(), 1, authToken);
    assertTrue(logEntry.contains("REMOVE_ATTRIBUTE " + atom.getName()));

    // // remove the second attribute from the concept (assume verification of
    // MA, atomic actions, and log entry since we just tested those)
    // v = metaEditingService.removeAttribute(project.getId(), c.getId(),
    // c.getLastModified().getTime(), attribute2.getId(), false, authToken);
    // assertTrue(v.getErrors().isEmpty());
    //
    // // retrieve the concept and check attributes
    // c = contentService.getConcept(concept.getId(), project.getId(),
    // authToken);
    //
    // boolean attribute2Present = false;
    // for (Attribute a : c.getAttributes()) {
    // if (a.getName().equals("UMLSRELA") && a.getValue().equals("VALUE2")) {
    // attribute2Present = true;
    // }
    // }
    // assertTrue(!attribute2Present);

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @Override
  @After
  public void teardown() throws Exception {

    // Copy existing concept to avoid messing with actual database data.
    IntegrationTestClientRest testService =
        new IntegrationTestClientRest(ConfigUtility.getConfigProperties());
    testService.removeConcept(concept.getId(), authToken);

    // logout
    securityService.logout(authToken);

  }

}
