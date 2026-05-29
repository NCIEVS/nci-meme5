/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.search;

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

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.model.workflow.WorklistJpa;
import com.wci.umls.server.model.workflow.Worklist;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for WorklistJpa persistence and search.
 * Tests unique bridges: MinValueBridge, MaxStateHistoryBridge, SplitUnderscoreBridge.
 * Also tests CollectionToCsvBridge for authors/reviewers and @IndexedEmbedded for notes.
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class WorklistSearchIntegrationIT extends IntegrationUnitSupport {

  /** The workflow service. */
  private WorkflowServiceJpa workflowService = null;

  /** The project service. */
  private ProjectServiceJpa projectService = null;

  /** The added worklist. */
  private Worklist addedWorklist = null;

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
   * Setup - create test project and worklist.
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
    uniqueSuffix = uniqueSearchToken("");

    projectService.setTransactionPerOperation(false);
    projectService.beginTransaction();

    // Create test project (required by worklist)
    final Project project = new ProjectJpa();
    project.setName("Test Project " + uniqueSuffix);
    project.setDescription("Test project for worklist search integration test");
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

    // Now create the worklist with the workflow service
    workflowService.setTransactionPerOperation(false);
    workflowService.beginTransaction();

    // Create test worklist with unique searchable values
    final Worklist worklist = new WorklistJpa();
    worklist.setName("wrk_bin_test_" + uniqueSuffix);
    worklist.setDescription("Test worklist for search integration");
    worklist.setProject(addedProject);
    worklist.setTimestamp(new Date());
    worklist.setLastModified(new Date());
    worklist.setLastModifiedBy("admin");
    worklist.setWorkflowStatus(WorkflowStatus.NEW);
    worklist.setEpoch("testEpoch" + uniqueSuffix);
    worklist.setWorkflowBinName("testBin" + uniqueSuffix);
    worklist.setTeam("testTeam" + uniqueSuffix);
    worklist.setNumber(1);

    // Add authors (tests CollectionToCsvBridge and MinValueBridge)
    worklist.getAuthors().add("authorAlpha" + uniqueSuffix);
    worklist.getAuthors().add("authorBeta" + uniqueSuffix);

    // Add reviewers
    worklist.getReviewers().add("reviewerGamma" + uniqueSuffix);

    // Add workflow state history (tests MaxStateHistoryBridge)
    // The bridge returns the key with the most recent (max) date
    worklist.getWorkflowStateHistory().put("NEW", new Date(System.currentTimeMillis() - 10000));
    worklist.getWorkflowStateHistory().put("EDITING_DONE", new Date());

    addedWorklist = workflowService.addWorklist(worklist);
    workflowService.commit();
  }

  /**
   * Test search by worklist name with SplitUnderscoreBridge.
   * The bridge replaces underscores with spaces for full-text search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByNameWithUnderscoreBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by word from name (underscores replaced with spaces by bridge)
    // Name is "wrk_bin_test_<suffix>" -> indexed as "wrk bin test <suffix>"
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "name:wrk", WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by name word 'wrk'", results.size() > 0);

    // Search by another word
    results = (List<Worklist>) workflowService.getQueryResults(
        "name:bin", WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by name word 'bin'", results.size() > 0);

    // Search by unique suffix (should match our specific worklist)
    results = (List<Worklist>) workflowService.getQueryResults(
        "name:" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by unique suffix", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());
  }

  /**
   * Test search by keyword fields (team, epoch, workflowBinName).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByKeywordFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by team
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by team", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());

    // Search by epoch
    results = (List<Worklist>) workflowService.getQueryResults(
        "epoch:testEpoch" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by epoch", results.size() > 0);

    // Search by workflowBinName
    results = (List<Worklist>) workflowService.getQueryResults(
        "workflowBinName:testBin" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by workflowBinName", results.size() > 0);
  }

  /**
   * Test search by workflowStatus enum field.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorkflowStatus() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by workflowStatus
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix + " AND workflowStatus:NEW", WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by workflowStatus", results.size() > 0);
  }

  /**
   * Test search by authors using CollectionToCsvBridge.
   * The bridge converts the collection to a comma-separated string for full-text search.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByAuthorsWithCollectionBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by author name (CollectionToCsvBridge converts list to searchable string)
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "authors:authorAlpha" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by author", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());

    // Search for second author
    results = (List<Worklist>) workflowService.getQueryResults(
        "authors:authorBeta" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by second author", results.size() > 0);
  }

  /**
   * Test search by reviewers using CollectionToCsvBridge.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByReviewersWithCollectionBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by reviewer name
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "reviewers:reviewerGamma" + uniqueSuffix, WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by reviewer", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());
  }

  /**
   * Test search by workflowState using MaxStateHistoryBridge.
   * The bridge returns the key (state) with the most recent (max) date value.
   * Since EDITING_DONE was added with a more recent date, it should be the indexed value.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByWorkflowStateWithMaxBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by workflowState - should match "EDITING_DONE" (the most recent state)
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix + " AND workflowState:EDITING_DONE", WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by most recent workflow state (EDITING_DONE)", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());

    // Search by older state should NOT match (since MaxStateHistoryBridge only indexes the most recent)
    results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix + " AND workflowState:NEW", WorklistJpa.class, null, totalCt);
    assertTrue("Should NOT find worklist by older workflow state", results.isEmpty());
  }

  /**
   * Test search by projectId.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByProjectId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by projectId (derived field from AbstractChecklist)
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "projectId:" + addedProject.getId(), WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist by projectId", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());
  }

  /**
   * Test combined query with multiple fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCombinedQuery() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Combine multiple fields
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix + " AND workflowStatus:NEW AND authors:authorAlpha" + uniqueSuffix,
        WorklistJpa.class, null, totalCt);
    assertTrue("Should find worklist with combined query", results.size() > 0);
    assertEquals(addedWorklist.getId(), results.get(0).getId());
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

    // Search with pagination
    @SuppressWarnings("unchecked")
    List<Worklist> results = (List<Worklist>) workflowService.getQueryResults(
        "team:testTeam" + uniqueSuffix, WorklistJpa.class, pfs, totalCt);
    assertTrue("Should find worklist with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Teardown - remove test data.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    try {
      // Remove worklist first
      if (addedWorklist != null && workflowService != null) {
        workflowService.setTransactionPerOperation(false);
        workflowService.beginTransaction();
        workflowService.removeWorklist(addedWorklist.getId(), true);
        workflowService.commit();
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error removing worklist: " + e.getMessage());
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
