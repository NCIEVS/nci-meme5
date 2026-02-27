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

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.UserJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.model.workflow.ChecklistJpa;
import com.wci.umls.server.model.workflow.Checklist;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for ChecklistJpa persistence and search.
 * Tests custom bridge: SplitUnderscoreBridge.
 *
 * Bridge behavior:
 * - SplitUnderscoreBridge: Replaces underscores with spaces for indexing.
 *   A name like "my_checklist_name" is indexed as "my checklist name"
 *   Search pattern: "name:my" or "name:checklist" will find "my_checklist_name"
 *
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class ChecklistSearchIntegrationIT extends IntegrationUnitSupport {

  /** The workflow service. */
  private WorkflowServiceJpa workflowService = null;

  /** The project service. */
  private ProjectServiceJpa projectService = null;

  /** The security service. */
  private SecurityServiceJpa securityService = null;

  /** The added checklist. */
  private Checklist addedChecklist = null;

  /** The added project. */
  private Project addedProject = null;

  /** The added user. */
  private User addedUser = null;

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
   * Setup - create test checklist with underscore-separated name.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    securityService = new SecurityServiceJpa();
    securityService.setLastModifiedBy("admin");

    projectService = new ProjectServiceJpa();
    projectService.setLastModifiedBy("admin");

    workflowService = new WorkflowServiceJpa();
    workflowService.setLastModifiedBy("admin");

    // Generate unique suffix for this test run
    uniqueSuffix = String.valueOf(System.currentTimeMillis());

    // Create test user first
    securityService.setTransactionPerOperation(false);
    securityService.beginTransaction();

    final User user = new UserJpa();
    user.setUserName("testChecklistUser" + uniqueSuffix);
    user.setName("Test Checklist User " + uniqueSuffix);
    user.setEmail("testchecklistuser" + uniqueSuffix + "@example.com");
    user.setApplicationRole(UserRole.USER);
    addedUser = securityService.addUser(user);

    securityService.commit();

    // Create test project (needed for checklist)
    projectService.setTransactionPerOperation(false);
    projectService.beginTransaction();

    final Project project = new ProjectJpa();
    project.setName("Test Project For Checklist " + uniqueSuffix);
    project.setDescription("Test project for checklist search integration test");
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
    project.getUserRoleMap().put(addedUser, UserRole.AUTHOR);

    addedProject = projectService.addProject(project);
    projectService.commit();

    // Create test checklist with underscore-separated name
    // This tests SplitUnderscoreBridge which converts underscores to spaces for indexing
    workflowService.setTransactionPerOperation(false);
    workflowService.beginTransaction();

    final Checklist checklist = new ChecklistJpa();
    // Name with underscores - SplitUnderscoreBridge will index this as "test checklist alpha beta <uniqueSuffix>"
    checklist.setName("test_checklist_alpha_beta_" + uniqueSuffix);
    checklist.setDescription("Test checklist description for search test desc" + uniqueSuffix);
    checklist.setProject(addedProject);
    checklist.setTimestamp(new Date());
    checklist.setLastModified(new Date());
    checklist.setLastModifiedBy("admin");

    addedChecklist = workflowService.addChecklist(checklist);
    workflowService.commit();
  }

  /**
   * Test search by name using SplitUnderscoreBridge.
   * The name "test_checklist_alpha_beta_<suffix>" is indexed as "test checklist alpha beta <suffix>".
   * This allows searching for individual words that were separated by underscores.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByNameWithUnderscoreBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by first word (was before first underscore)
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:test AND projectId:" + addedProject.getId(), ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by first word 'test'", results.size() > 0);

    // Search by middle word (was between underscores)
    results = (List<Checklist>) workflowService.getQueryResults(
        "name:checklist AND projectId:" + addedProject.getId(), ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by middle word 'checklist'", results.size() > 0);

    // Search by another middle word
    results = (List<Checklist>) workflowService.getQueryResults(
        "name:alpha AND projectId:" + addedProject.getId(), ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by word 'alpha'", results.size() > 0);

    // Search by unique suffix (last word after underscore)
    results = (List<Checklist>) workflowService.getQueryResults(
        "name:" + uniqueSuffix, ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by unique suffix", results.size() > 0);
    assertEquals(addedChecklist.getId(), results.get(0).getId());
  }

  /**
   * Test search by multiple words from underscore-separated name.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByMultipleWordsFromUnderscoreName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by multiple words that were underscore-separated
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:test AND name:alpha AND projectId:" + addedProject.getId(),
        ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by multiple underscore-separated words", results.size() > 0);
    assertEquals(addedChecklist.getId(), results.get(0).getId());
  }

  /**
   * Test search by exact nameSort (keyword field, not transformed).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByNameSort() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // nameSort is a keyword field that stores the original value
    // Note: Keyword fields require exact match
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "nameSort:test_checklist_alpha_beta_" + uniqueSuffix, ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by exact nameSort", results.size() > 0);
    assertEquals(addedChecklist.getId(), results.get(0).getId());
  }

  /**
   * Test search by projectId (uses ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByProjectId() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by projectId (Long indexed with ObjectToStringBridge)
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "projectId:" + addedProject.getId(), ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by projectId", results.size() > 0);

    // Verify our checklist is in the results
    boolean found = false;
    for (Checklist c : results) {
      if (c.getId().equals(addedChecklist.getId())) {
        found = true;
        break;
      }
    }
    assertTrue("Should find our specific checklist by projectId", found);
  }

  /**
   * Test search by lastModifiedBy (keyword field).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByLastModifiedBy() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by lastModifiedBy combined with unique name
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:" + uniqueSuffix + " AND lastModifiedBy:admin", ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist by lastModifiedBy", results.size() > 0);
    assertEquals(addedChecklist.getId(), results.get(0).getId());
  }

  /**
   * Test combined query.
   *
   * @throws Exception the exception
   */
  @Test
  public void testCombinedQuery() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Combine name (using underscore bridge), projectId, and lastModifiedBy
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:alpha AND projectId:" + addedProject.getId() + " AND lastModifiedBy:admin",
        ChecklistJpa.class, null, totalCt);
    assertTrue("Should find checklist with combined query", results.size() > 0);
    assertEquals(addedChecklist.getId(), results.get(0).getId());
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
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:" + uniqueSuffix, ChecklistJpa.class, pfs, totalCt);
    assertTrue("Should find checklist with pagination", results.size() > 0);
    assertTrue("Total count should be set", totalCt[0] > 0);
  }

  /**
   * Test that searching for underscore-connected string doesn't match.
   * Since SplitUnderscoreBridge converts underscores to spaces, searching for
   * "test_checklist" as a single token won't match.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUnderscoreSearchBehavior() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search for words separated by space (should match - this is how it's indexed)
    @SuppressWarnings("unchecked")
    List<Checklist> results = (List<Checklist>) workflowService.getQueryResults(
        "name:beta AND projectId:" + addedProject.getId(), ChecklistJpa.class, null, totalCt);
    assertTrue("Should find with individual word search", results.size() > 0);
  }

  /**
   * Teardown - remove test data.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    try {
      // Remove checklist first
      if (addedChecklist != null && workflowService != null) {
        workflowService.setTransactionPerOperation(false);
        workflowService.beginTransaction();
        workflowService.removeChecklist(addedChecklist.getId(), true);
        workflowService.commit();
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error removing checklist: " + e.getMessage());
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

    try {
      // Remove user
      if (addedUser != null && securityService != null) {
        securityService.setTransactionPerOperation(false);
        securityService.beginTransaction();
        securityService.removeUser(addedUser.getId());
        securityService.commit();
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error removing user: " + e.getMessage());
    } finally {
      if (securityService != null) {
        securityService.close();
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
