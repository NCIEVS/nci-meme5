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

import com.wci.umls.server.Project;
import com.wci.umls.server.User;
import com.wci.umls.server.UserRole;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.jpa.UserJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration test for ProjectJpa persistence and search.
 * Tests unique bridges: UserRoleBridge, UserMapUserNameBridge, MapKeyValueToCsvBridge.
 * Serves as documentation for query patterns after Hibernate Search 7.x migration.
 */
public class ProjectSearchIntegrationTest extends IntegrationUnitSupport {

  /** The project service. */
  private ProjectServiceJpa projectService = null;

  /** The security service. */
  private SecurityServiceJpa securityService = null;

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
   * Setup - create test user and project.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    securityService = new SecurityServiceJpa();
    securityService.setLastModifiedBy("admin");

    projectService = new ProjectServiceJpa();
    projectService.setLastModifiedBy("admin");

    // Generate unique suffix for this test run
    uniqueSuffix = String.valueOf(System.currentTimeMillis());

    // Create test user first (needed for userRoleMap)
    securityService.setTransactionPerOperation(false);
    securityService.beginTransaction();

    final User user = new UserJpa();
    user.setUserName("testUser" + uniqueSuffix);
    user.setName("Test User " + uniqueSuffix);
    user.setEmail("testuser" + uniqueSuffix + "@example.com");
    user.setApplicationRole(UserRole.USER);
    addedUser = securityService.addUser(user);

    securityService.commit();

    // Create test project with user role mapping
    projectService.setTransactionPerOperation(false);
    projectService.beginTransaction();

    final Project project = new ProjectJpa();
    project.setName("Test Project " + uniqueSuffix);
    project.setDescription("Test project for search integration testDesc" + uniqueSuffix);
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

    // Add user to project with AUTHOR role (tests UserRoleBridge and UserMapUserNameBridge)
    project.getUserRoleMap().put(addedUser, UserRole.AUTHOR);

    // Add semantic type category mapping (tests MapKeyValueToCsvBridge)
    project.getSemanticTypeCategoryMap().put("Pharmacologic Substance", "CHEM");
    project.getSemanticTypeCategoryMap().put("Disease or Syndrome", "DISO");

    addedProject = projectService.addProject(project);
    projectService.commit();
  }

  /**
   * Test search by project name (full-text).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByName() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by word in project name
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "name:Project", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by name word", results.size() > 0);

    // Search by unique suffix in name
    results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix, ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by unique name", results.size() > 0);
    assertEquals(addedProject.getId(), results.get(0).getId());
  }

  /**
   * Test search by description (full-text).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByDescription() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by unique token in description
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "description:testDesc" + uniqueSuffix, ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by description", results.size() > 0);
    assertEquals(addedProject.getId(), results.get(0).getId());
  }

  /**
   * Test search by keyword fields (terminology, lastModifiedBy).
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByKeywordFields() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by terminology
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND terminology:MTH", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by terminology", results.size() > 0);

    // Search by lastModifiedBy
    results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND lastModifiedBy:admin", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by lastModifiedBy", results.size() > 0);
  }

  /**
   * Test search by userRoleMap using UserRoleBridge.
   * The bridge indexes user+role combinations like "user1ADMIN,user2AUTHOR,".
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByUserRoleWithBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by user+role combination (UserRoleBridge indexes "usernameROLE")
    // Our test user has AUTHOR role, so search for "testUser<suffix>AUTHOR"
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "userRoleMap:testUser" + uniqueSuffix + "AUTHOR", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by user+role combination", results.size() > 0);
    assertEquals(addedProject.getId(), results.get(0).getId());

    // Search for wrong role should not match
    results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND userRoleMap:testUser" + uniqueSuffix + "ADMIN", ProjectJpa.class, null, totalCt);
    assertTrue("Should NOT find project with wrong role", results.isEmpty());
  }

  /**
   * Test search by userAnyRole using UserMapUserNameBridge.
   * The bridge indexes just usernames (space-separated) regardless of role.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchByUserAnyRoleWithBridge() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by username only (UserMapUserNameBridge indexes just usernames)
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "userAnyRole:testUser" + uniqueSuffix, ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by username (any role)", results.size() > 0);
    assertEquals(addedProject.getId(), results.get(0).getId());
  }

  /**
   * Test search by semanticTypeCategoryMap using MapKeyValueToCsvBridge.
   * The bridge indexes key=value pairs as comma-separated string.
   *
   * @throws Exception the exception
   */
  @Test
  public void testSearchBySemanticTypeCategoryMap() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    int[] totalCt = new int[1];

    // Search by semantic type category (MapKeyValueToCsvBridge indexes "key=value")
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND semanticTypeCategoryMap:Pharmacologic", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by semantic type in category map", results.size() > 0);

    // Search by category value
    results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND semanticTypeCategoryMap:CHEM", ProjectJpa.class, null, totalCt);
    assertTrue("Should find project by category value in map", results.size() > 0);
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

    // Combine name, terminology, and user role
    @SuppressWarnings("unchecked")
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix + " AND terminology:MTH AND userAnyRole:testUser" + uniqueSuffix,
        ProjectJpa.class, null, totalCt);
    assertTrue("Should find project with combined query", results.size() > 0);
    assertEquals(addedProject.getId(), results.get(0).getId());
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
    List<Project> results = (List<Project>) projectService.getQueryResults(
        "name:" + uniqueSuffix, ProjectJpa.class, pfs, totalCt);
    assertTrue("Should find project with pagination", results.size() > 0);
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
      // Remove project first
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
