/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.mojo;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.Project;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.HistoryServiceJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.HistoryService;
import com.wci.umls.server.services.ProjectService;

/**
 * Implementation of the "RF2 Single  Load and Unload Test Case".
 */
public class RrfSingleLoadAndUnloadTest {

  /** The properties. */
  static Properties config;

  /** The server. */
  static String server = "false";

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    // n/a
  }

  /**
   * Test the sequence:
   * 
   * <pre>
   * Run Updatedb mojo in "create" mode to clear the database
   *   TEST: verify there is a concepts table with no contents
   * Run Reindex mojo to clear the indexes
   *   TEST: verify there is a ConceptJpa index with no contents.
   * Run the RRF-single mojo against the sample config/src/resources/data/SCTMTH_2014AB" data.
   *   TEST: verify each content table exists with the expected number of entries.
   * Create a "SNOMEDCT_US" project (name="Sample Project" description="Sample project." terminology=SNOMEDCT_US version=latest scope.concepts=? scope.descendants.flag=true admin.user=admin)
   *   TEST: verify there is a project with the expected name
   * Start an editing cycle for "SNOMEDCT_US"
   *   TEST: verify there is a release info with the expected name and "planned" flag equal to true.
   * Remove SNOMEDCTUS
   *   TEST: verify there is a concepts table with no contents
   * Re-run "createdb" to restore database to initial state
   * </pre>
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {

    // Use the run.config.umls configuration for this build

    // Createdb
    Logger.getLogger(getClass()).info("Create database");
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/db/pom.xml"));
    request.setProfiles(Arrays.asList("Createdb"));
    request.setGoals(Arrays.asList("clean", "install"));
    Properties p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    request.setProperties(p);
    DefaultInvoker invoker = new DefaultInvoker();
    InvocationResult result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Reindex
    Logger.getLogger(getClass()).info("Clear indexes");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/lucene/pom.xml"));
    request.setProfiles(Arrays.asList("Reindex"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Verify no contents
    Logger.getLogger(getClass()).info("Verify no contents");
    ContentService service = new ContentServiceJpa();
    Assert
        .assertEquals(0,
            service.getAllConcepts("SNOMEDCT_US", "latest", Branch.ROOT)
                .size());
    service.close();
    service.closeFactory();

    // Load RRF single
    Logger.getLogger(getClass()).info("Load SNOMEDCT_US from RRF single");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RRF-single"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("terminology", "SNOMEDCT_US");
    p.setProperty("version", "latest");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/SCTMSH_2014AB");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Verify expected contents
    Logger.getLogger(getClass()).info("Verify contents");
    service = new ContentServiceJpa();
    Assert
        .assertEquals(3903,
            service.getAllConcepts("SNOMEDCT_US", "latest", Branch.ROOT)
                .size());

    // Print component Stats
    Logger.getLogger(getClass()).info(
        "  component stats = "
            + service.getComponentStats("SNOMEDCT_US", "latest", Branch.ROOT));

    service.close();
    service.closeFactory();

    // Verify release info
    Logger.getLogger(getClass()).info("Verify release info");
    HistoryService historyService = new HistoryServiceJpa();
    Assert
        .assertNotNull(historyService.getReleaseInfo("SNOMEDCT_US", "latest"));
    historyService.close();
    historyService.closeFactory();

    // Add a SNOMEDCT_US project
    Logger.getLogger(getClass()).info("Add SNOMEDCT_US project");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("Project"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("name", "Sample project");
    p.setProperty("description", "Sample project.");
    p.setProperty("terminology", "SNOMEDCT_US");
    p.setProperty("version", "latest");
    // scope ignored for now
    p.setProperty("scope.descendants.flag", "true");
    p.setProperty("admin.user", "admin");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Verify project exists
    Logger.getLogger(getClass()).info("Verify project");
    ProjectService projectService = new ProjectServiceJpa();
    boolean found = false;
    for (Project project : projectService.getProjects().getObjects()) {
      if (project.getName().equals("Sample project")
          && project.getDescription().equals("Sample project.")
          && project.getTerminology().equals("SNOMEDCT_US")) {
        // Scope ignored for now - &&
        // project.getScopeConcepts().iterator().next().equals("138875005")) {
        found = true;
      }
    }
    Assert.assertTrue(found);
    projectService.close();
    projectService.closeFactory();

    // Start SNOMEDCT editing cycle
    Logger.getLogger(getClass()).info("Start SNOMEDCT_US editing cycle");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/release/pom.xml"));
    request.setProfiles(Arrays.asList("StartEditingCycle"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("release.version", "20150131");
    p.setProperty("terminology", "SNOMEDCT_US");
    p.setProperty("version", "latest");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Verify release info for 2015AA as "planned"
    Logger.getLogger(getClass()).info("Verify release info");
    historyService = new HistoryServiceJpa();
    Assert.assertNotNull(historyService.getReleaseInfo("SNOMEDCT_US",
        "20150131"));
    Assert.assertFalse(historyService.getReleaseInfo("SNOMEDCT_US", "20150131")
        .isPublished());
    Assert.assertTrue(historyService.getReleaseInfo("SNOMEDCT_US", "20150131")
        .isPlanned());
    historyService.close();
    historyService.closeFactory();

    // QA Terminology
    Logger.getLogger(getClass()).info("QA database");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/qa/pom.xml"));
    request.setProfiles(Arrays.asList("Database"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Remove terminology
    Logger.getLogger(getClass()).info("Remove SNOMEDCT_US");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/remover/pom.xml"));
    request.setProfiles(Arrays.asList("Terminology"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("terminology", "SNOMEDCT_US");
    p.setProperty("version", "latest");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Verify no contents
    Logger.getLogger(getClass()).info("Verify no SNOMEDCT_US contents");
    service = new ContentServiceJpa();
    Assert
        .assertEquals(0,
            service.getAllConcepts("SNOMEDCT_US", "latest", Branch.ROOT)
                .size());
    // Print component Stats
    Logger.getLogger(getClass()).info(
        "  component stats = "
            + service.getComponentStats("SNOMEDCT_US", "latest", Branch.ROOT));
    service.close();
    service.closeFactory();

    // Finish by clearing the DB again
    Logger.getLogger(getClass()).info("Clear database");
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/db/pom.xml"));
    request.setProfiles(Arrays.asList("Createdb"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }
  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // n/a
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // n/a
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // n/a
  }

}
