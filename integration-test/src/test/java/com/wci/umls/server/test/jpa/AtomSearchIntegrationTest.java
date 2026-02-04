/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for AtomJpa persistence and search.
 * Tests searching by all indexed field types: String, Date, Boolean, Long, Enum.
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class AtomSearchIntegrationTest extends IntegrationUnitSupport {

  /** The service. */
  private ContentServiceJpa service = null;

  /** The added atom. */
  private Atom addedAtom = null;

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
   * Setup - create test atom.
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

    // Create test atom with specific searchable values
    final Atom atom = new AtomJpa();
    atom.setBranch(Branch.ROOT);
    atom.setName("acetaminophen tablet 500mg unique" + uniqueSuffix);
    atom.setTerminology("MTH");
    atom.setTerminologyId("ATEST" + uniqueSuffix);
    atom.setVersion("latest");
    atom.setCodeId("CTEST" + uniqueSuffix);
    atom.setConceptId("CLTEST" + uniqueSuffix);
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

    // Persist the atom and commit to update the index
    service.setTransactionPerOperation(false);
    service.beginTransaction();
    addedAtom = service.addAtom(atom);
    service.commit();
  }

  /**
   * Test full-text search on name field (analyzed @FullTextField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by single word in name
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "name:acetaminophen", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by name word", results.size() > 0);

    // Search by unique identifier in name
    results = (List<Atom>) service.getQueryResults(
        "name:unique" + uniqueSuffix, AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by unique name token", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());
  }

  /**
   * Test autocomplete fields derived from name (@FullTextField with analyzers).
   *
   * The name field has multiple index representations:
   * - name: full-text with noStopWord analyzer
   * - edgeNGramName: autocomplete edge analyzer (prefix matching)
   * - nGramName: autocomplete ngram analyzer (partial matching)
   * - nameSort: keyword for sorting
   * - nameNorm: normalized keyword (derived field)
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByNameFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by edgeNGramName (prefix autocomplete) - should match "aceta" prefix
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND edgeNGramName:aceta", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by edgeNGramName prefix 'aceta'", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());

    // Search by nGramName (ngram autocomplete) - should match partial "amino"
    results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND nGramName:amino", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by nGramName partial 'amino'", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());

    // Search by nameNorm (normalized name - derived field)
    // nameNorm is the result of ConfigUtility.normalize(name)
    results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND nameNorm:*", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom with nameNorm indexed", results.size() > 0);
  }

  /**
   * Test keyword field search (not analyzed @KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByKeywordFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by terminologyId (exact match required)
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix, AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by terminologyId", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());

    // Search by codeId
    results = (List<Atom>) service.getQueryResults(
        "codeId:CTEST" + uniqueSuffix, AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by codeId", results.size() > 0);

    // Search by termType
    results = (List<Atom>) service.getQueryResults(
        "termType:PT", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by termType", results.size() > 0);

    // Search by language
    results = (List<Atom>) service.getQueryResults(
        "language:ENG", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by language", results.size() > 0);
  }

  /**
   * Test boolean field search (@GenericField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByBooleanFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by obsolete=false
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND obsolete:false", AtomJpa.class, null, totalCt);
    assertTrue("Should find non-obsolete atom", results.size() > 0);

    // Search by published=true
    results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND published:true", AtomJpa.class, null, totalCt);
    assertTrue("Should find published atom", results.size() > 0);
  }

  /**
   * Test enum field search (@GenericField with bridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByEnumField() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by workflowStatus combined with unique terminologyId
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND workflowStatus:READY_FOR_PUBLICATION", AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by workflowStatus", results.size() > 0);
  }

  /**
   * Test Long/ID field search (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchById() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by id
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "id:" + addedAtom.getId(), AtomJpa.class, null, totalCt);
    assertTrue("Should find atom by id", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());
  }

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
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND termType:PT AND published:true",
        AtomJpa.class, null, totalCt);
    assertTrue("Should find atom with combined query", results.size() > 0);

    // Negative query with NOT
    results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND NOT obsolete:true",
        AtomJpa.class, null, totalCt);
    assertTrue("Should find non-obsolete atom", results.size() > 0);
  }

  /**
   * Test date range search on lastModified field (@GenericField with DateToIsoFormatBridge).
   *
   * Uses ISO 8601 date format for range queries (e.g., "2024-12-11T03:10:27.000Z").
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByDateRange() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Get the lastModified date from the added atom
    final Date atomDate = addedAtom.getLastModified();
    final long atomTime = atomDate.getTime();

    // Define a range that INCLUDES the atom's lastModified (1 hour before to 1 hour after)
    final long oneHourMs = 60 * 60 * 1000;
    final String rangeStart = DateTimeFormatter.ISO_INSTANT
        .format(Instant.ofEpochMilli(atomTime - oneHourMs));
    final String rangeEnd = DateTimeFormatter.ISO_INSTANT
        .format(Instant.ofEpochMilli(atomTime + oneHourMs));

    // Search with date range that includes the atom - use terminologyId to isolate our test atom
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND lastModified:[" + rangeStart + " TO " + rangeEnd + "]",
        AtomJpa.class, null, totalCt);
    assertTrue("Should find atom within date range", results.size() > 0);
    assertEquals(addedAtom.getId(), results.get(0).getId());

    // Define a range that EXCLUDES the atom's lastModified (1 day ago to 1 hour ago)
    final long oneDayMs = 24 * 60 * 60 * 1000;
    final String excludeRangeStart = DateTimeFormatter.ISO_INSTANT
        .format(Instant.ofEpochMilli(atomTime - oneDayMs));
    final String excludeRangeEnd = DateTimeFormatter.ISO_INSTANT
        .format(Instant.ofEpochMilli(atomTime - oneHourMs));

    // Search with date range that excludes the atom
    results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix + " AND lastModified:[" + excludeRangeStart + " TO " + excludeRangeEnd + "]",
        AtomJpa.class, null, totalCt);
    assertTrue("Should NOT find atom outside date range", results.isEmpty());
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

    // Search with pagination using unique terminologyId
    @SuppressWarnings("unchecked")
    List<Atom> results = (List<Atom>) service.getQueryResults(
        "terminologyId:ATEST" + uniqueSuffix, AtomJpa.class, pfs, totalCt);
    assertTrue("Should find atom with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Teardown - remove test atom.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    if (addedAtom != null && service != null) {
      service.setTransactionPerOperation(false);
      service.beginTransaction();
      service.removeAtom(addedAtom.getId());
      service.commit();
    }
    if (service != null) {
      service.close();
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
