/*
 * Copyright 2016 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.test.rest.ncimeta;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.SemanticTypeComponentJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.rest.client.IntegrationTestClientRest;

import java.util.stream.Collectors;

/**
 * Implementation of the "MetaEditing Service REST Edge Cases" Test Cases.
 */
@Ignore
public class MetaEditingServiceRestEdgeCasesIT
    extends MetaEditingServiceRestIT {

  /** The auth tokens. */
  private String authToken;

  /** The project. */
  private Project project;

  /** The umls terminology. */
  private String umlsTerminology = "NCIMTH";

  /** The umls version. */
  private String umlsVersion = "latest";

  /**
   * The concept (will be copied from existing concept, to avoid affecting
   * database values.
   */
  private ConceptJpa concept;

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Override
  @Before
  public void setup() throws Exception {

    // authenticate the viewer user
    authToken =
            securityService.authenticate(adminUser, adminPassword).getAuthToken();

    // ensure there is a concept associated with the project
    ProjectList projects = projectService.findProjects(null, null, authToken);
    assertTrue(projects.size() > 0);

    // Find the project with the expected terminology - don't rely on sort order
    project = projects.getObjects().stream()
            .filter(p -> umlsTerminology.equals(p.getTerminology()))
            .findFirst().orElse(null);

    // verify terminology and branch are expected values
    assertTrue("Expected project with terminology '" + umlsTerminology
                    + "' but found: " + projects.getObjects().stream()
                    .map(p -> p.getName() + "=" + p.getTerminology())
                    .collect(Collectors.joining(", ")),
            project != null && project.getTerminology().equals(umlsTerminology));
    // assertTrue(project.getBranch().equals(Branch.ROOT));

    // Copy existing concept to avoid messing with actual database data.
    concept = new ConceptJpa(contentService.getConcept("C0000294",
            umlsTerminology, umlsVersion, null, authToken), false);
    concept.setId(null);
    concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    concept = (ConceptJpa) testService.addConcept(concept, authToken);

  }

  /**
   * Test synchronicity.
   * @throws Exception
   */
  @Test
  public void testSynchronicity() throws Exception {

    ValidationResult result = new ValidationResultJpa();

    // get the concept
    Concept c1 =
            contentService.getConcept(concept.getId(), project.getId(), authToken);

    // construct a semantic type not present on concept (here, Lipid)
    String sty = "Lipid";
    SemanticTypeComponent fullSty = new SemanticTypeComponentJpa();

    // add the sty
    result = metaEditingService.addSemanticType(project.getId(), c1.getId(),
            "activityId", c1.getLastModified().getTime(),
            sty, false, authToken);
    assertTrue(result.isValid());

    // get the concept
    c1 = contentService.getConcept(concept.getId(), null, authToken);

    sty = null;
    for (SemanticTypeComponent s : c1.getSemanticTypes()) {
      if (s.getSemanticType().equals("Lipid")) {
        fullSty = s;
      }
    }
    assertNotNull(fullSty);

    final Long cId = c1.getId();
    final Long cDate = c1.getLastModified().getTime();
    final Long styId = fullSty.getId();

    // number of repeated calls to make
    final int[] nThreads = {
            10
    };

    // runnable instanes
    final Thread[] threads = new Thread[nThreads[0]];

    // accessible validation results from within runnables
    final ValidationResult[] v = new ValidationResultJpa[nThreads[0]];

    // accessible index, success, and exception counters
    int ct[] = new int[1];
    int completeCt[] = {
            0
    };
    int successCt[] = {
            0
    };
    int exceptionCt[] = {
            0
    };

    for (int i = 0; i < nThreads[0]; i++) {
      v[i] = new ValidationResultJpa();
      ct[0] = i;
      threads[i] = new Thread(new Runnable() {
        @Override
        public void run() {
          Logger.getLogger(getClass()).info("Running new thread");

          // introduce slight delay (really for cleanliness of log)
          try {
            Thread.sleep(500);
          } catch (InterruptedException e1) {
            Logger.getLogger(getClass()).info("  Sleep command failed");
          }
          try {
            if (metaEditingService.removeSemanticType(project.getId(), cId,
                    "activityId", cDate, styId, false, authToken).isValid()) {
              Logger.getLogger(getClass()).info("  Thread returned success");
              successCt[0]++;
            } else {
              Logger.getLogger(getClass())
                      .info("  Thread returned expected failure");
            }
          } catch (Exception e) {
            Logger.getLogger(getClass())
                    .info("  Unexpected exception encountered");
            exceptionCt[0]++;
          } finally {
            completeCt[0]++;
            Logger.getLogger(getClass())
                    .info("Thread complete: " + completeCt[0] + "/" + nThreads[0]
                            + " (" + exceptionCt[0] + " exceptions, " + successCt[0]
                            + " successes)");
            // on all threads complete, expect only one valid result and no
            // exceptions
            // NOTE: Exceptions indicate a commit error (asynchronous failure)
            // NOTE: Failures due to data conditions are captured as validation
            // results (isValid check above)
            if (completeCt[0] == nThreads[0]) {
              assertTrue(exceptionCt[0] == 0);
              assertTrue(successCt[0] == 1);
              Logger.getLogger(getClass()).info("Synchronicity test complete");
              assertTrue(false);
            }
          }
        }
      });
    }

    // execute the simultaneous requests
    for (int i = 0; i < nThreads[0]; i++) {
      threads[i].start();
    }

    // Wait for all threads to finish
    for (int i = 0; i < nThreads[0]; i++) {
      threads[i].join();
    }

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @Override
  @After
  public void teardown() throws Exception {
    // Copy existing concept to avoid messing with actual database data.
    IntegrationTestClientRest testService =
            new IntegrationTestClientRest(PropertyUtility.getProperties());
    testService.removeConcept(concept.getId(), true, authToken);
    // logout
    securityService.logout(authToken);

  }

}
