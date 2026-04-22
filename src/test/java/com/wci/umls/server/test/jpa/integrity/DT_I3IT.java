/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.integrity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.algo.action.AddDemotionMolecularAction;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.validation.DT_I3;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration testing for {@link DT_I3}.
 */
public class DT_I3IT extends IntegrationUnitSupport {

  /** The project. */
  private Project project;

  /** The service. */
  protected ContentServiceJpa contentService;

  /** The concept demotions. */
  private Concept conceptDemotions = null;

  /** The concept no demotions. */
  private Concept conceptNoDemotions = null;

  /**
   * Setup class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    // do nothing
  }

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    project = null;
    conceptDemotions = null;
    conceptNoDemotions = null;

    // instantiate service
    contentService = new ContentServiceJpa();

    // make a copy of the validationTest project
    ProjectList projects = contentService.getProjects();
    assertTrue(projects.size() > 0);
    project = new ProjectJpa(projects.getObjects().get(0));

    // Reset the project's validation check list, so only this integrity check
    // will run.
    project.setValidationChecks(new ArrayList<>(Arrays.asList("DT_I3")));

    // Get two concepts, one with DEMOTION relationships, and one without.
    // C0040247 has DEMOTION atomRels in SAMPLE_UMLS; C0004611 does not.
    conceptDemotions =
        contentService.getConcept("C0040247", "MTH", "latest", Branch.ROOT);
    conceptNoDemotions =
        contentService.getConcept("C0004611", "MTH", "latest", Branch.ROOT);
    ensureDemotionRelationship(conceptDemotions,
        contentService.getConcept("C0118168", "MTH", "latest", Branch.ROOT));

  }

  /**
   * Ensure the supplied concept has at least one DEMOTION atom relationship.
   *
   * @param fromConcept the source concept
   * @param toConcept the target concept
   * @throws Exception the exception
   */
  private void ensureDemotionRelationship(Concept fromConcept, Concept toConcept)
    throws Exception {
    for (final Atom atom : fromConcept.getAtoms()) {
      for (final AtomRelationship atomRel : atom.getRelationships()) {
        if (atomRel.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
          return;
        }
      }
    }

    final AddDemotionMolecularAction action = new AddDemotionMolecularAction();
    try {
      action.setProject(project);
      action.setConceptId(fromConcept.getId());
      action.setConceptId2(toConcept.getId());
      action.setLastModifiedBy("admin");
      action.setLastModified(fromConcept.getLastModified().getTime());
      action.setOverrideWarnings(false);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);
      action.setTerminology("MTH");
      action.setVersion("latest");
      action.setAtomId(fromConcept.getAtoms().get(0).getId());
      action.setAtomId2(toConcept.getAtoms().get(0).getId());
      final ValidationResult validationResult =
          action.performMolecularAction(action, "admin", true, false);
      assertTrue(validationResult.getErrors().isEmpty());
    } finally {
      action.close();
    }

    contentService.close();
    contentService = new ContentServiceJpa();
    conceptDemotions =
        contentService.getConcept("C0040247", "MTH", "latest", Branch.ROOT);
  }

  /**
   * Test merge normal use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUse() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    //
    // Test violation of DT_I3
    // Concept contains Demotion relationships
    //
    // Reload concept within a transaction so Hibernate 6 can lazily initialize
    // atom.getRelationships() (2nd-level lazy collection).
    {
      ContentServiceJpa txService = new ContentServiceJpa();
      txService.setTransactionPerOperation(false);
      txService.beginTransaction();
      Concept fresh = txService.getConcept("C0040247", "MTH", "latest", Branch.ROOT);
      final ValidationResult validationResult =
          txService.validateConcept(project.getValidationChecks(), fresh);
      txService.rollback();
      txService.close();

      // Verify that it returned a validation error
      assertFalse(validationResult.isValid());
    }

    //
    // Test non-violation of DT_I3
    // Concept contains no Demotion relationships
    //

    // Check whether the action violates the validation check
    final ValidationResult validationResult2 =
        contentService.validateConcept(project.getValidationChecks(), conceptNoDemotions);

    // Verify that it returned no validation errors
    assertTrue(validationResult2.isValid());

  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // do nothing
  }

  /**
   * Teardown class.
   */
  @AfterClass
  public static void teardownClass() {
    // do nothing
  }

}
