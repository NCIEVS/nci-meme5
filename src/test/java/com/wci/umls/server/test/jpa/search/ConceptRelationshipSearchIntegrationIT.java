/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for ConceptRelationshipJpa persistence and search.
 * Tests searching by all indexed field types from ConceptRelationshipJpa,
 * AbstractRelationship, AbstractComponent, and AbstractHasLastModified.
 */
public class ConceptRelationshipSearchIntegrationIT extends IntegrationUnitSupport {

  /** The service. */
  private ContentServiceJpa service = null;

  /** The added relationship. */
  private ConceptRelationship addedRelationship = null;

  /** The from concept. */
  private Concept fromConcept = null;

  /** The to concept. */
  private Concept toConcept = null;

  /** Unique suffix for this test run. */
  private String uniqueSuffix = null;

  /** Setup class. */
  @BeforeClass
  public static void setupClass() {
    // do nothing
  }

  /**
   * Setup - create test concepts and relationship.
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

    // Create from concept
    fromConcept = new ConceptJpa();
    fromConcept.setBranch(Branch.ROOT);
    fromConcept.setName("FromConceptName unique" + uniqueSuffix);
    fromConcept.setTerminology("MTH");
    fromConcept.setTerminologyId("CFROM" + uniqueSuffix);
    fromConcept.setVersion("latest");
    fromConcept.setTimestamp(new Date());
    fromConcept.setObsolete(false);
    fromConcept.setSuppressible(false);
    fromConcept.setPublished(true);
    fromConcept.setPublishable(true);
    fromConcept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    fromConcept = service.addConcept(fromConcept);

    // Create to concept
    toConcept = new ConceptJpa();
    toConcept.setBranch(Branch.ROOT);
    toConcept.setName("ToConceptName unique" + uniqueSuffix);
    toConcept.setTerminology("SNOMEDCT_US");
    toConcept.setTerminologyId("CTO" + uniqueSuffix);
    toConcept.setVersion("20230901");
    toConcept.setTimestamp(new Date());
    toConcept.setObsolete(false);
    toConcept.setSuppressible(false);
    toConcept.setPublished(true);
    toConcept.setPublishable(true);
    toConcept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    toConcept = service.addConcept(toConcept);

    // Create relationship with unique ID based on timestamp
    final ConceptRelationship rel = new ConceptRelationshipJpa();
    rel.setId(Long.parseLong(uniqueSuffix));  // Use timestamp as unique ID
    rel.setFrom(fromConcept);
    rel.setTo(toConcept);
    rel.setRelationshipType("RO");
    rel.setAdditionalRelationshipType("has_part");
    rel.setGroup("GRP" + uniqueSuffix);
    rel.setInferred(false);
    rel.setStated(true);
    rel.setHierarchical(false);
    rel.setAssertedDirection(true);
    rel.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    rel.setBranch(Branch.ROOT);
    rel.setTerminology("MTH");
    rel.setTerminologyId("RTEST" + uniqueSuffix);
    rel.setVersion("latest");
    rel.setObsolete(false);
    rel.setSuppressible(false);
    rel.setPublished(true);
    rel.setPublishable(true);
    rel.setTimestamp(new Date());

    // Add alternate terminology ids
    final Map<String, String> altIds = new HashMap<>();
    altIds.put("SNOMEDCT_US", "ALT" + uniqueSuffix);
    rel.setAlternateTerminologyIds(altIds);

    service.addRelationship(rel);
    addedRelationship = rel;
    service.commit();
  }


  // ----------------------------------------
  // Tests for ConceptRelationshipJpa fields
  // ----------------------------------------

  /**
   * Test search by fromId (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFromId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "fromId:" + fromConcept.getId(), ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by fromId", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by fromTerminologyId (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFromTerminologyId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "fromTerminologyId:CFROM" + uniqueSuffix, ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by fromTerminologyId", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by fromTerminology (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFromTerminology() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND fromTerminology:MTH",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by fromTerminology", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by fromVersion (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFromVersion() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND fromVersion:latest",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by fromVersion", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by fromName (@FullTextField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFromName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Full-text search on fromName
    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "fromName:FromConceptName", ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by fromName", results.size() > 0);
  }

  /**
   * Test search by toId (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByToId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "toId:" + toConcept.getId(), ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by toId", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by toTerminologyId (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByToTerminologyId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "toTerminologyId:CTO" + uniqueSuffix, ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by toTerminologyId", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by toTerminology (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByToTerminology() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND toTerminology:SNOMEDCT_US",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by toTerminology", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by toVersion (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByToVersion() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND toVersion:20230901",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by toVersion", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by toName (@FullTextField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByToName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "toName:ToConceptName", ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by toName", results.size() > 0);
  }

  /**
   * Test search by alternateTerminologyIds (@FullTextField with MapKeyValueToCsvBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByAlternateTerminologyIds() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by key or value in the map
    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND alternateTerminologyIds:SNOMEDCT_US",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by alternateTerminologyIds key", results.size() > 0);

    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND alternateTerminologyIds:ALT" + uniqueSuffix,
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by alternateTerminologyIds value", results.size() > 0);
  }

  // ----------------------------------------
  // Tests for AbstractRelationship fields
  // ----------------------------------------

  /**
   * Test search by relationshipType (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByRelationshipType() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND relationshipType:RO",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by relationshipType", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by additionalRelationshipType (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByAdditionalRelationshipType() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND additionalRelationshipType:has_part",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by additionalRelationshipType", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by group (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByGroup() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "group:GRP" + uniqueSuffix, ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by group", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by boolean fields from AbstractRelationship.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByRelationshipBooleanFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by inferred
    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND inferred:false",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by inferred=false", results.size() > 0);

    // Search by stated
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND stated:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by stated=true", results.size() > 0);

    // Search by hierarchical
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND hierarchical:false",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by hierarchical=false", results.size() > 0);

    // Search by assertedDirection
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND assertedDirection:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by assertedDirection=true", results.size() > 0);
  }

  /**
   * Test search by workflowStatus (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorkflowStatus() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND workflowStatus:READY_FOR_PUBLICATION",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by workflowStatus", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  // ----------------------------------------
  // Tests for AbstractComponent fields
  // ----------------------------------------

  /**
   * Test search by terminology (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByTerminology() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND terminology:MTH",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by terminology", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by terminologyId (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByTerminologyId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix, ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by terminologyId", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by version (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByVersion() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND version:latest",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by version", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by branch (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByBranch() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND branch:" + Branch.ROOT,
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by branch", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test search by component boolean fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByComponentBooleanFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by obsolete
    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND obsolete:false",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by obsolete=false", results.size() > 0);

    // Search by suppressible
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND suppressible:false",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by suppressible=false", results.size() > 0);

    // Search by published
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND published:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by published=true", results.size() > 0);

    // Search by publishable
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND publishable:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by publishable=true", results.size() > 0);
  }

  // ----------------------------------------
  // Tests for AbstractHasLastModified fields
  // ----------------------------------------

  /**
   * Test search by lastModifiedBy (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByLastModifiedBy() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND lastModifiedBy:admin",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship by lastModifiedBy", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());
  }

  /**
   * Test date range search on lastModified field (@GenericField with DateToIsoFormatBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByDateRange() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Get the lastModified date from the added relationship
    final Date relDate = addedRelationship.getLastModified();
    final long relTime = relDate.getTime();

    // Define a range that INCLUDES the relationship's lastModified
    final long oneHourMs = 60 * 60 * 1000;
    final String rangeStart =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(relTime - oneHourMs));
    final String rangeEnd =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(relTime + oneHourMs));

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix
            + " AND lastModified:[" + rangeStart + " TO " + rangeEnd + "]",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship within date range", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());

    // Define a range that EXCLUDES the relationship's lastModified
    final long oneDayMs = 24 * 60 * 60 * 1000;
    final String excludeRangeStart =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(relTime - oneDayMs));
    final String excludeRangeEnd =
        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(relTime - oneHourMs));

    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix
            + " AND lastModified:[" + excludeRangeStart + " TO " + excludeRangeEnd + "]",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should NOT find relationship outside date range", results.isEmpty());
  }

  // ----------------------------------------
  // Complex query tests
  // ----------------------------------------

  /**
   * Test combined/complex queries.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCombinedQuery() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Combine multiple fields with AND
    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix
            + " AND relationshipType:RO AND stated:true AND published:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find relationship with combined query", results.size() > 0);
    assertEquals(addedRelationship.getId(), results.get(0).getId());

    // Negative query with NOT
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix + " AND NOT obsolete:true",
        ConceptRelationshipJpa.class, null, totalCt);
    assertTrue("Should find non-obsolete relationship", results.size() > 0);
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

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix, ConceptRelationshipJpa.class, pfs, totalCt);
    assertTrue("Should find relationship with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Test sorting by fromName and toName.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchWithSorting() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setStartIndex(0);
    pfs.setMaxResults(10);
    pfs.setSortField("fromName");

    @SuppressWarnings("unchecked")
    List<ConceptRelationship> results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix, ConceptRelationshipJpa.class, pfs, totalCt);
    assertTrue("Should find relationship with sorting", results.size() > 0);

    // Test sorting by toName
    pfs.setSortField("toName");
    results = (List<ConceptRelationship>) service.getQueryResults(
        "terminologyId:RTEST" + uniqueSuffix, ConceptRelationshipJpa.class, pfs, totalCt);
    assertTrue("Should find relationship with toName sorting", results.size() > 0);
  }

  /**
   * Teardown - remove test data.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    if (service != null) {
      service.setTransactionPerOperation(false);
      service.beginTransaction();
      if (addedRelationship != null) {
        service.removeRelationship(addedRelationship.getId(), ConceptRelationshipJpa.class);
      }
      if (fromConcept != null) {
        service.removeConcept(fromConcept.getId());
      }
      if (toConcept != null) {
        service.removeConcept(toConcept.getId());
      }
      service.commit();
      service.close();
    }
  }

  /** Teardown class. */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }
}