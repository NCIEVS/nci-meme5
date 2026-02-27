/*
 *    Copyright 2015 West Coast Informatics, LLC
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
import org.apache.maven.shared.invoker.Invoker;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.algo.RrfUnpublishedLoaderAlgorithm;
import com.wci.umls.server.jpa.algo.maint.BuildDeepRelTablesAlgorithm;

/**
 * A mechanism to reset to the stock dev database for NCI-META testing.
 */
public class ResetNciMetaDatabase {

  /** The properties. */
  static Properties config;

  /** The server. */
  static String server = "false";

  /** The maven home. */
  static File mavenHome;

  /**
   * Find Maven home from various locations.
   *
   * @return the maven home directory
   */
  private static File findMavenHome() {
    // Check M2_HOME environment variable
    String m2Home = System.getenv("M2_HOME");
    if (m2Home != null) {
      File dir = new File(m2Home);
      if (dir.exists()) {
        return dir;
      }
    }

    // Check MAVEN_HOME environment variable
    String mavenHomeEnv = System.getenv("MAVEN_HOME");
    if (mavenHomeEnv != null) {
      File dir = new File(mavenHomeEnv);
      if (dir.exists()) {
        return dir;
      }
    }

    // Check for Maven wrapper downloaded version
    String userHome = System.getProperty("user.home");
    File wrapperDir = new File(userHome, ".m2/wrapper/dists");
    if (wrapperDir.exists()) {
      // Find the first apache-maven directory
      File[] mavenDirs = wrapperDir.listFiles(f -> f.getName().startsWith("apache-maven"));
      if (mavenDirs != null && mavenDirs.length > 0) {
        // Navigate into the hash directory to find the actual maven installation
        File[] hashDirs = mavenDirs[0].listFiles(File::isDirectory);
        if (hashDirs != null && hashDirs.length > 0) {
          File[] installDirs = hashDirs[0].listFiles(f -> f.getName().startsWith("apache-maven"));
          if (installDirs != null && installDirs.length > 0) {
            return installDirs[0];
          }
        }
      }
    }

    // Check IntelliJ bundled Maven
    File intellijMaven = new File("/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3");
    if (intellijMaven.exists()) {
      return intellijMaven;
    }

    // Check common Homebrew locations
    File[] homebrewLocations = {
        new File("/opt/homebrew/Cellar/maven"),
        new File("/usr/local/Cellar/maven")
    };
    for (File homebrewDir : homebrewLocations) {
      if (homebrewDir.exists()) {
        File[] versions = homebrewDir.listFiles(File::isDirectory);
        if (versions != null && versions.length > 0) {
          return new File(versions[0], "libexec");
        }
      }
    }

    return null;
  }

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    config = ConfigUtility.getConfigProperties();
    if (ConfigUtility.isServerActive()) {
      server = "true";
    }
    mavenHome = findMavenHome();
    if (mavenHome == null) {
      throw new Exception("Could not find Maven home. Set M2_HOME or MAVEN_HOME environment variable.");
    }
    Logger.getLogger(ResetNciMetaDatabase.class).info("Using Maven home: " + mavenHome);
  }

  /**
   * Test the sequence:
   * 
   * <pre>
   * Load sample data
   * GenrateNciMetaData
   * </pre>
   * 
   * .
   *
   * @throws Exception the exception
   */
  @Test
  public void test() throws Exception {

    // Set up vars
    InvocationRequest request;
    Properties p;
    Invoker invoker;
    InvocationResult result;

    // Load RF2 full
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RRF-umls"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("edit.mode", "true");
    p.setProperty("server", server);
    p.setProperty("mode", "create");
    p.setProperty("terminology", "NCIMTH");
    p.setProperty("version", "latest");
    request.setMavenOpts("-Xmx15G");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/SAMPLE_NCI");
    if (System.getProperty("input.dir") != null) {
      p.setProperty("input.dir", System.getProperty("input.dir"));
    }
    request.setProperties(p);
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Build the deep_relationships tables
    Logger.getLogger(getClass())
        .info("Run rebuild deep relationship tables algorithm");
    final BuildDeepRelTablesAlgorithm buildAlgo =
        new BuildDeepRelTablesAlgorithm();
    buildAlgo.setTransactionPerOperation(false);
    buildAlgo.beginTransaction();
    buildAlgo.compute();
    buildAlgo.commit();
    buildAlgo.close();
    // Close the EntityManagerFactory to release Hibernate Search index locks
    buildAlgo.closeFactory();
    Logger.getLogger(getClass())
        .info("Finished rebuild deep relationship tables algorithm");

    // Generate MetaData
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("GenerateNciMetaData"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("terminology", "NCIMTH");
    p.setProperty("version", "latest");
    p.setProperty("mode", "update");
    request.setProperties(p);
    request.setMavenOpts("-Xmx15G");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/SAMPLE_NCI");
    if (System.getProperty("input.dir") != null) {
      p.setProperty("input.dir", System.getProperty("input.dir"));
    }
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // RRF Unpublished
    Logger.getLogger(getClass()).info("Run unpublished loader algorithm");
    final RrfUnpublishedLoaderAlgorithm algo =
        new RrfUnpublishedLoaderAlgorithm();
    algo.setActivityId("LOADER");
    algo.setWorkId("LOADER");
    // ASSUMPTION: one project
    algo.setProject(algo.getProjects().getObjects().get(0));
    // ONLY one ".." here because it's running from integration-tests
    algo.setInputPath(
        "../config/src/main/resources/data/SAMPLE_NCI/unpublished");
    if (System.getProperty("input.dir") != null) {
      algo.setInputPath(System.getProperty("input.dir") + "/unpublished");
    }
    algo.setLastModifiedBy("admin");
    algo.setLastModifiedFlag(true);
    algo.setMolecularActionFlag(false);
    algo.setTransactionPerOperation(false);
    algo.beginTransaction();
    algo.setTerminology("NCIMTH");
    algo.setVersion("latest");
    algo.compute();
    algo.commit();
    algo.close();
    Logger.getLogger(getClass()).info("Finished unpublished loader algorithm");

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
