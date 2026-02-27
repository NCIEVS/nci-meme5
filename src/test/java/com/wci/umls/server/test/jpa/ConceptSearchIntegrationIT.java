/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for ConceptJpa persistence and search.
 * Tests searching by Concept's own fields and by embedded Atom fields.
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class ConceptSearchIntegrationIT extends IntegrationUnitSupport {

  /** The service. */
  private ContentServiceJpa service = null;

  /** The added concept. */
  private Concept addedConcept = null;

  /** The added atom. */
  private Atom addedAtom = null;

  /** The added semantic type. */
  private SemanticTypeComponent addedSty = null;

  /** Unique suffix for this test run. */
  private String uniqueSuffix = null;

  /**
   * Setup class.
   */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup - create test concept with atom and semantic type.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    service = new ContentServiceJpa();
    service.setLastModifiedBy("admin");
    service.setMolecularActionFlag(false);

    // Generate unique suffix for this test run
    uniqueSuffix = String.valueOf(System.currentTimeMillis());

    service.setTransactionPerOperation(false);
    service.beginTransaction();

    // Create test atom first (atoms need to exist before adding to concept)
    final Atom atom = new AtomJpa();
    atom.setBranch(Branch.ROOT);
    atom.setName("aspirin tablet 325mg testAtom" + uniqueSuffix);
    atom.setTerminology("MTH");
    atom.setTerminologyId("ATOMTEST" + uniqueSuffix);
    atom.setVersion("latest");
    atom.setCodeId("CODETEST" + uniqueSuffix);
    atom.setConceptId("CONCEPTTEST" + uniqueSuffix);
    atom.setDescriptorId("");
    atom.setLanguage("ENG");
    atom.setTermType("PT");
    atom.setLexicalClassId("LTEST" + uniqueSuffix);
    atom.setStringClassId("STEST" + uniqueSuffix);
    atom.setTimestamp(new Date());
    atom.setObsolete(false);
    atom.setSuppressible(false);
    atom.setPublished(true);
    atom.setPublishable(true);
    atom.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    addedAtom = service.addAtom(atom);

    // Create test concept first (semantic type requires concept)
    final Concept concept = new ConceptJpa();
    concept.setBranch(Branch.ROOT);
    concept.setName("Aspirin Concept testConcept" + uniqueSuffix);
    concept.setTerminology("MTH");
    concept.setTerminologyId("CLTEST" + uniqueSuffix);
    concept.setVersion("latest");
    concept.setTimestamp(new Date());
    concept.setObsolete(false);
    concept.setSuppressible(false);
    concept.setPublished(true);
    concept.setPublishable(true);
    concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    concept.setFullyDefined(false);
    concept.setAnonymous(false);
    concept.getAtoms().add(addedAtom);
    addedConcept = service.addConcept(concept);

    // Create semantic type component (requires concept as second argument)
    final SemanticTypeComponent sty = new SemanticTypeComponentJpa();
    sty.setBranch(Branch.ROOT);
    sty.setSemanticType("Pharmacologic Substance");
    sty.setTerminology("MTH");
    sty.setTerminologyId("STYTEST" + uniqueSuffix);
    sty.setVersion("latest");
    sty.setTimestamp(new Date());
    sty.setObsolete(false);
    sty.setSuppressible(false);
    sty.setPublished(true);
    sty.setPublishable(true);
    sty.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    addedSty = service.addSemanticTypeComponent(sty, addedConcept);

    // Manually add semantic type to concept's collection and update to trigger re-indexing
    addedConcept.getSemanticTypes().add(addedSty);
    service.updateConcept(addedConcept);

    service.commit();
  }

  /**
   * Test full-text search on concept name field.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by word in concept name
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "name:Aspirin", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by name word", results.size() > 0);

    // Search by unique identifier in concept name
    results = (List<Concept>) service.getQueryResults(
        "name:testConcept" + uniqueSuffix, ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by unique name token", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());
  }

  /**
   * Test keyword field search on concept fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptKeywordFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by terminologyId (exact match required)
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix, ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by terminologyId", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());

    // Search by terminology
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND terminology:MTH", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by terminology", results.size() > 0);
  }

  /**
   * Test boolean field search on concept fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptBooleanFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by fullyDefined=false
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND fullyDefined:false", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept with fullyDefined=false", results.size() > 0);

    // Search by anonymous=false
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND anonymous:false", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept with anonymous=false", results.size() > 0);

    // Search by published=true
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND published:true", ConceptJpa.class, null, totalCt);
    assertTrue("Should find published concept", results.size() > 0);
  }

  /**
   * Test enum field search on concept workflowStatus.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptEnumField() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by workflowStatus
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND workflowStatus:READY_FOR_PUBLICATION",
        ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by workflowStatus", results.size() > 0);
  }

  /**
   * Test Long/ID field search on concept.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByConceptId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by id
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "id:" + addedConcept.getId(), ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by id", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());
  }

  /**
   * Test searching concept by embedded atom name (@IndexedEmbedded).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByEmbeddedAtomName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by embedded atom name (full-text)
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "atoms.name:aspirin", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by embedded atom name", results.size() > 0);

    // Search by unique token in embedded atom name
    results = (List<Concept>) service.getQueryResults(
        "atoms.name:testAtom" + uniqueSuffix, ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by unique atom name token", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());
  }

  /**
   * Test searching concept by embedded atom keyword fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByEmbeddedAtomKeywordFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by embedded atom terminologyId
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "atoms.terminologyId:ATOMTEST" + uniqueSuffix, ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by embedded atom terminologyId", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());

    // Search by embedded atom termType
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND atoms.termType:PT", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by embedded atom termType", results.size() > 0);

    // Search by embedded atom language
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND atoms.language:ENG", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by embedded atom language", results.size() > 0);
  }

  /**
   * Test searching concept by embedded semantic type.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByEmbeddedSemanticType() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by embedded semantic type (exact match - keyword field)
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "semanticTypes.semanticType:\"Pharmacologic Substance\"", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept by embedded semantic type", results.size() > 0);

    // Combine with unique terminologyId to verify it's our concept
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND semanticTypes.semanticType:\"Pharmacologic Substance\"",
        ConceptJpa.class, null, totalCt);
    assertTrue("Should find our concept by semantic type", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());
  }

  /**
   * Test combined query with concept and atom fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCombinedConceptAndAtomQuery() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Combine concept name with atom name
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "name:Aspirin AND atoms.name:aspirin", ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept with combined concept and atom name query", results.size() > 0);

    // Combine concept terminologyId with atom termType and semantic type
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND atoms.termType:PT AND semanticTypes.semanticType:\"Pharmacologic Substance\"",
        ConceptJpa.class, null, totalCt);
    assertTrue("Should find concept with complex combined query", results.size() > 0);
    assertEquals(addedConcept.getId(), results.get(0).getId());

    // Negative query - search for atom property that doesn't match
    results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix + " AND atoms.termType:SY", ConceptJpa.class, null, totalCt);
    assertTrue("Should NOT find concept with non-matching atom termType", results.isEmpty());
  }

  /**
   * Test pagination with PfsParameter.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWithPagination() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setStartIndex(0);
    pfs.setMaxResults(10);

    // Search with pagination
    @SuppressWarnings("unchecked")
    List<Concept> results = (List<Concept>) service.getQueryResults(
        "terminologyId:CLTEST" + uniqueSuffix, ConceptJpa.class, pfs, totalCt);
    assertTrue("Should find concept with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Teardown - remove test data.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    if (service != null) {
      try {
        service.setTransactionPerOperation(false);
        service.beginTransaction();

        // Remove semantic type first (it references the concept)
        if (addedSty != null) {
          service.removeSemanticTypeComponent(addedSty.getId());
        }

        if (addedConcept != null) {
          service.removeConcept(addedConcept.getId());
        }

        if (addedAtom != null) {
          service.removeAtom(addedAtom.getId());
        }

        service.commit();
      } catch (Exception e) {
        Logger.getLogger(getClass()).error("Error during teardown: " + e.getMessage());
        service.rollback();
      } finally {
        service.close();
      }
    }
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }
}
