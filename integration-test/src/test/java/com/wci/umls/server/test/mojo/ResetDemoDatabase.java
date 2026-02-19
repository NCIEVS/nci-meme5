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

/**
 * A mechanism to reset to the stock dev database.
 */
public class ResetDemoDatabase {

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
    Logger.getLogger(ResetDemoDatabase.class).info("Using Maven home: " + mavenHome);
  }

  /**
   * Test the sequence:
   *
   * <pre>
   * 1. Load SNOMED, SNOMEDCT_US, ICD9CM, ICD10CM, RXNORM (from sample UMLS)
   * 2. Genereate demo data
   * </pre>
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void test() throws Exception {

    InvocationRequest request;
    Properties p;
    Invoker invoker;
    InvocationResult result;

    // Load RRF - LOINC
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RRF-multi"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("mode", "create");
    // This is actually the overall "meta" to ignore
    p.setProperty("terminology", "MTH");
    p.setProperty("version", "latest");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/SAMPLE_UMLS");
    request.setProperties(p);
    request.setMavenOpts("-Xmx2G");
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load RF2 snapshot - SNOMEDCT
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RF2-snapshot"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("mode", "update");
    p.setProperty("terminology", "SNOMEDCT");
    p.setProperty("version", "20160731");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/snomedct-20140731-mini");
    request.setProperties(p);
    request.setMavenOpts("-Xmx2G");
    request.setDebug(false);
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load RF2 snapshot - SNOMEDCT_US
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RF2-snapshot"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("mode", "update");
    p.setProperty("terminology", "SNOMEDCT_US");
    p.setProperty("version", "20160901");
    p.setProperty("input.dir",
        "../../config/src/main/resources/data/snomedct_us-20150301-mini");
    request.setProperties(p);
    request.setMavenOpts("-Xmx2G");
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load ClaML - ICD9CM
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("ClaML"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("mode", "update");
    p.setProperty("terminology", "ICD-9-CM");
    p.setProperty("version", "2016");
    p.setProperty("input.file",
        "../../config/src/main/resources/data/icd9cm-2013.xml");
    request.setProperties(p);
    request.setMavenOpts("-Xmx2G");
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load ClaML - ICD10CM
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("ClaML"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("server", server);
    p.setProperty("mode", "update");
    p.setProperty("terminology", "ICD-10-CM");
    p.setProperty("version", "2015");
    p.setProperty("input.file",
        "../../config/src/main/resources/data/icd10cm-2016.xml");
    request.setProperties(p);
    request.setMavenOpts("-Xmx2G");
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Generate Demo Data
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("GenerateDemoData"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config.umls", System.getProperty("run.config.umls"));
    p.setProperty("mode", "update");
    request.setProperties(p);
    invoker = new DefaultInvoker();
    invoker.setMavenHome(mavenHome);
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