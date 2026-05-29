/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.model.workflow.TrackingRecordJpa;
import com.wci.umls.server.model.workflow.TrackingRecord;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for TrackingRecordJpa persistence and search.
 * Tests searching by all indexed field types including:
 * - GenericField with ObjectToStringBridge (id, clusterId, projectId, workflowStatus, finished)
 * - KeywordField (lastModifiedBy, clusterType, terminology, version, workflowBinName, worklistName, checklistName)
 * - FullTextField with CollectionToCsvBridge (componentIds, origConceptIds)
 * - FullTextField (indexedData)
 * - GenericField sortable (lastModified, clusterId)
 */
public class TrackingRecordSearchIntegrationIT extends IntegrationUnitSupport {

  /** The workflow service. */
  private WorkflowServiceJpa workflowService = null;

  /** The project service. */
  private ProjectServiceJpa projectService = null;

  /** The added tracking record. */
  private TrackingRecord addedRecord = null;

  /** The added project. */
  private Project addedProject = null;

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
   * Setup - create test project and tracking record.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    projectService = new ProjectServiceJpa();
    projectService.setLastModifiedBy("admin");

    workflowService = new WorkflowServiceJpa();
    workflowService.setLastModifiedBy("admin");

    // Generate unique suffix for this test run
    uniqueSuffix = uniqueNumericToken();

    projectService.setTransactionPerOperation(false);
    projectService.beginTransaction();

    // Create test project (required by tracking record)
    final Project project = new ProjectJpa();
    project.setName("Test Project " + uniqueSuffix);
    project.setDescription("Test project for tracking record search integration test");
    project.setTerminology("MTH");
    project.setVersion("latest");
    project.setLanguage("ENG");
    project.setTimestamp(new Date());
    project.setLastModified(new Date());
    project.setLastModifiedBy("admin");
    project.setPublic(false);
    project.setTeamBased(false);
    project.setWorkflowPath("DEFAULT");
    project.setEditingEnabled(true);
    project.setAutomationsEnabled(false);
    addedProject = projectService.addProject(project);

    projectService.commit();

    // Now create the tracking record with the workflow service
    workflowService.setTransactionPerOperation(false);
    workflowService.beginTransaction();

    // Create test tracking record with unique searchable values
    final TrackingRecord record = new TrackingRecordJpa();
    record.setProject(addedProject);
    record.setTerminology("MTH");
    record.setVersion("latest");
    record.setClusterId(Long.parseLong(uniqueSuffix));
    record.setClusterType("clusterType" + uniqueSuffix);
    record.setWorkflowBinName("testBin" + uniqueSuffix);
    record.setWorklistName("testWorklist" + uniqueSuffix);
    record.setChecklistName("testChecklist" + uniqueSuffix);
    record.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    record.setIndexedData("IndexedDataText unique " + uniqueSuffix);
    record.setFinished(false);
    record.setTimestamp(new Date());
    record.setLastModified(new Date());
    record.setLastModifiedBy("admin");

    // Add component IDs (tests CollectionToCsvBridge)
    final Set<Long> componentIds = new HashSet<>();
    componentIds.add(100001L);
    componentIds.add(100002L);
    record.setComponentIds(componentIds);

    // Add original concept IDs (tests CollectionToCsvBridge)
    final Set<Long> origConceptIds = new HashSet<>();
    origConceptIds.add(200001L);
    origConceptIds.add(200002L);
    record.setOrigConceptIds(origConceptIds);

    addedRecord = workflowService.addTrackingRecord(record);
    workflowService.commit();
  }

  // ----------------------------------------
  // Tests for TrackingRecordJpa specific fields
  // ----------------------------------------

  /**
   * Test search by id (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchById() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "id:" + addedRecord.getId(), TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by id", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by clusterId (@GenericField with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByClusterId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by clusterId", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by clusterType (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByClusterType() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterType:clusterType" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by clusterType", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

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
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND terminology:MTH",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by terminology", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
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
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND version:latest",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by version", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by workflowBinName (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorkflowBinName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "workflowBinName:testBin" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by workflowBinName", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by worklistName (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorklistName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "worklistName:testWorklist" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by worklistName", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by checklistName (@KeywordField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByChecklistName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "checklistName:testChecklist" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by checklistName", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by projectId (@GenericField with ObjectToStringBridge - derived field).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByProjectId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "projectId:" + addedProject.getId(), TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by projectId", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by workflowStatus (@GenericField with ObjectToStringBridge - enum).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorkflowStatus() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND workflowStatus:READY_FOR_PUBLICATION",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by workflowStatus", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by finished (@GenericField with ObjectToStringBridge - boolean).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByFinished() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND finished:false",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by finished=false", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by indexedData (@FullTextField).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByIndexedData() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Full-text search on indexedData
    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "indexedData:IndexedDataText", TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by indexedData", results.size() > 0);

    // Search by unique suffix in indexedData
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "indexedData:" + uniqueSuffix, TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by unique suffix in indexedData", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
  }

  /**
   * Test search by componentIds (@FullTextField with CollectionToCsvBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByComponentIds() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by component ID (CollectionToCsvBridge converts Set<Long> to searchable string)
    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "componentIds:100001", TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by componentId", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());

    // Search for second component ID
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "componentIds:100002", TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by second componentId", results.size() > 0);
  }

  /**
   * Test search by origConceptIds (@FullTextField with CollectionToCsvBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByOrigConceptIds() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by original concept ID
    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "origConceptIds:200001", TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by origConceptId", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());

    // Search for second original concept ID
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "origConceptIds:200002", TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by second origConceptId", results.size() > 0);
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
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND lastModifiedBy:admin",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record by lastModifiedBy", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());
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
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix
            + " AND terminology:MTH AND workflowStatus:READY_FOR_PUBLICATION AND finished:false",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record with combined query", results.size() > 0);
    assertEquals(addedRecord.getId(), results.get(0).getId());

    // Query with NOT
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix + " AND NOT finished:true",
        TrackingRecordJpa.class, null, totalCt);
    assertTrue("Should find tracking record with NOT query", results.size() > 0);
  }

  /**
   * Test search with pagination.
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
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix, TrackingRecordJpa.class, pfs, totalCt);
    assertTrue("Should find tracking record with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Test search with sorting by clusterId.
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
    pfs.setSortField("clusterId");

    @SuppressWarnings("unchecked")
    List<TrackingRecord> results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix, TrackingRecordJpa.class, pfs, totalCt);
    assertTrue("Should find tracking record with sorting by clusterId", results.size() > 0);

    // Test sorting by lastModified
    pfs.setSortField("lastModified");
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix, TrackingRecordJpa.class, pfs, totalCt);
    assertTrue("Should find tracking record with sorting by lastModified", results.size() > 0);

    // Test sorting by indexedData (using indexedDataSort keyword field)
    pfs.setSortField("indexedData");
    results = (List<TrackingRecord>) workflowService.getQueryResults(
        "clusterId:" + uniqueSuffix, TrackingRecordJpa.class, pfs, totalCt);
    assertTrue("Should find tracking record with sorting by indexedData", results.size() > 0);
  }

  /**
   * Teardown - remove test data.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    try {
      // Remove tracking record first
      if (addedRecord != null && workflowService != null) {
        workflowService.setTransactionPerOperation(false);
        workflowService.beginTransaction();
        workflowService.removeTrackingRecord(addedRecord.getId());
        workflowService.commit();
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error removing tracking record: " + e.getMessage());
    } finally {
      if (workflowService != null) {
        workflowService.close();
      }
    }

    try {
      // Remove project
      if (addedProject != null && projectService != null) {
        projectService.setTransactionPerOperation(false);
        projectService.beginTransaction();
        projectService.removeProject(addedProject.getId());
        projectService.commit();
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error removing project: " + e.getMessage());
    } finally {
      if (projectService != null) {
        projectService.close();
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
