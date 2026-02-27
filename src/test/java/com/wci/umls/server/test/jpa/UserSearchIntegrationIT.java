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
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for UserJpa persistence and search.
 * Tests custom bridges: ProjectRoleBridge and MapIdBridge.
 *
 * Bridge behaviors:
 * - ProjectRoleBridge: Indexes project+role combinations like "10ADMIN,20AUTHOR,"
 *   Search pattern: "projectRoleMap:10ADMIN" finds users with ADMIN role on project 10
 * - MapIdBridge: Indexes just project IDs space-separated like "10 20 "
 *   Search pattern: "projectAnyRole:10" finds users with any role on project 10
 *
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class UserSearchIntegrationIT extends IntegrationUnitSupport {

  /** The security service. */
  private SecurityServiceJpa securityService = null;

  /** The project service. */
  private ProjectServiceJpa projectService = null;

  /** The added user. */
  private User addedUser = null;

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
   * Setup - create test user with project role mapping.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    projectService = new ProjectServiceJpa();
    projectService.setLastModifiedBy("admin");

    securityService = new SecurityServiceJpa();
    securityService.setLastModifiedBy("admin");

    // Generate unique suffix for this test run
    uniqueSuffix = String.valueOf(System.currentTimeMillis());

    // Create test project first (needed for projectRoleMap)
    projectService.setTransactionPerOperation(false);
    projectService.beginTransaction();

    final Project project = new ProjectJpa();
    project.setName("Test Project For User " + uniqueSuffix);
    project.setDescription("Test project for user search integration test");
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

    // Create test user with project role mapping
    securityService.setTransactionPerOperation(false);
    securityService.beginTransaction();

    final User user = new UserJpa();
    user.setUserName("testUserSearch" + uniqueSuffix);
    user.setName("Test User Search " + uniqueSuffix);
    user.setEmail("testusersearch" + uniqueSuffix + "@example.com");
    user.setApplicationRole(UserRole.USER);
    user.setEditorLevel(3);
    user.setTeam("TestTeam" + uniqueSuffix);

    // Add project role mapping - this tests ProjectRoleBridge and MapIdBridge
    user.getProjectRoleMap().put(addedProject, UserRole.AUTHOR);

    addedUser = securityService.addUser(user);
    securityService.commit();
  }

  /**
   * Test search by userName (keyword field).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByUserName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by exact userName
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "userName:testUserSearch" + uniqueSuffix, UserJpa.class, null, totalCt);
    assertTrue("Should find user by userName", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());
  }

  /**
   * Test search by name (full-text field).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by word in name
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "name:Search", UserJpa.class, null, totalCt);
    assertTrue("Should find user by name word", results.size() > 0);

    // Search by unique suffix
    results = (List<User>) securityService.getQueryResults(
        "name:" + uniqueSuffix, UserJpa.class, null, totalCt);
    assertTrue("Should find user by unique name", results.size() > 0);
  }

  /**
   * Test search by team (keyword field).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByTeam() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by team
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "team:TestTeam" + uniqueSuffix, UserJpa.class, null, totalCt);
    assertTrue("Should find user by team", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());
  }

  /**
   * Test search by applicationRole (enum with ObjectToStringBridge).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByApplicationRole() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by application role
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "userName:testUserSearch" + uniqueSuffix + " AND applicationRole:USER",
        UserJpa.class, null, totalCt);
    assertTrue("Should find user by applicationRole", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());
  }

  /**
   * Test search by projectRoleMap using ProjectRoleBridge.
   * ProjectRoleBridge indexes user's project+role combinations like "10ADMIN,20AUTHOR,".
   * Search pattern: "projectRoleMap:<projectId><Role>" (e.g., "projectRoleMap:10AUTHOR")
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByProjectRoleMap() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by project+role combination (ProjectRoleBridge indexes "projectIdROLE")
    // Our test user has AUTHOR role on addedProject
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "projectRoleMap:" + addedProject.getId() + "AUTHOR", UserJpa.class, null, totalCt);
    assertTrue("Should find user by project+role combination", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());

    // Search for wrong role should not match this specific user
    results = (List<User>) securityService.getQueryResults(
        "userName:testUserSearch" + uniqueSuffix + " AND projectRoleMap:" + addedProject.getId() + "ADMIN",
        UserJpa.class, null, totalCt);
    assertTrue("Should NOT find user with wrong role on project", results.isEmpty());
  }

  /**
   * Test search by projectAnyRole using MapIdBridge.
   * MapIdBridge indexes just the project IDs space-separated like "10 20 ".
   * Search pattern: "projectAnyRole:<projectId>" (e.g., "projectAnyRole:10")
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByProjectAnyRole() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by project ID only (MapIdBridge indexes just project IDs)
    // This finds users with ANY role on the project
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "projectAnyRole:" + addedProject.getId(), UserJpa.class, null, totalCt);
    assertTrue("Should find user by project ID (any role)", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());
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

    // Combine userName, applicationRole, and projectAnyRole
    @SuppressWarnings("unchecked")
    List<User> results = (List<User>) securityService.getQueryResults(
        "userName:testUserSearch" + uniqueSuffix +
        " AND applicationRole:USER" +
        " AND projectAnyRole:" + addedProject.getId(),
        UserJpa.class, null, totalCt);
    assertTrue("Should find user with combined query", results.size() > 0);
    assertEquals(addedUser.getId(), results.get(0).getId());
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
    List<User> results = (List<User>) securityService.getQueryResults(
        "userName:testUserSearch" + uniqueSuffix, UserJpa.class, pfs, totalCt);
    assertTrue("Should find user with pagination", results.size() > 0);
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
      // Remove user first (has reference to project)
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
