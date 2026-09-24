/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa.integrity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.algo.action.AddDemotionMolecularAction;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.validation.DT_I3B;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration testing for {@link DT_I3B}.
 */
public class DT_I3BIT extends IntegrationUnitSupport {

  /** The project. */
  private Project project;

  /** The service. */
  protected ContentServiceJpa contentService;

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

    // instantiate service
    contentService = new ContentServiceJpa();

    // make a copy of the validationTest project
    ProjectList projects = contentService.getProjects();
    assertTrue(projects.size() > 0);
    project = new ProjectJpa(projects.getObjects().get(0));

    // Reset the project's validation check list, so only this integrity check
    // will run.
    project.setValidationChecks(new ArrayList<>(Arrays.asList("DT_I3B")));

    // Ensure the violation case has at least one DEMOTION relationship.
    final Concept conceptDemotionsNoCorresponding =
        contentService.getConcept("C0040247", "MTH", "latest", Branch.ROOT);
    ensureDemotionRelationship(conceptDemotionsNoCorresponding,
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
  }

  /**
   * Add a matching concept relationship for every DEMOTION target.
   *
   * @param service the content service
   * @param source the source concept
   * @throws Exception the exception
   */
  private void addMatchingConceptRelationships(ContentServiceJpa service,
    Concept source) throws Exception {
    final Set<Long> matchedConceptIds = new HashSet<>();

    for (final Atom atom : source.getAtoms()) {
      for (final AtomRelationship atomRel : atom.getRelationships()) {
        if (!WorkflowStatus.DEMOTION.equals(atomRel.getWorkflowStatus())) {
          continue;
        }

        final List<ConceptJpa> targetConcepts = service.getEntityManager()
            .createQuery("select distinct c from ConceptJpa c join c.atoms a "
                + "where c.terminology = :terminology and c.version = :version "
                + "and c.branch = :branch and a.id = :atomId", ConceptJpa.class)
            .setParameter("terminology", "MTH")
            .setParameter("version", "latest")
            .setParameter("branch", Branch.ROOT)
            .setParameter("atomId", atomRel.getTo().getId()).getResultList();
        assertEquals("Expected one MTH concept for demoted atom "
            + atomRel.getTo().getId(), 1, targetConcepts.size());

        final ConceptJpa targetConcept = targetConcepts.get(0);
        if (matchedConceptIds.add(targetConcept.getId())) {
          final ConceptRelationship matchingRel =
              new ConceptRelationshipJpa();
          matchingRel.setFrom(source);
          matchingRel.setTo(targetConcept);
          matchingRel.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
          matchingRel.setPublishable(true);
          source.getRelationships().add(matchingRel);
        }
      }
    }

    assertFalse("Expected at least one DEMOTION relationship",
        matchedConceptIds.isEmpty());
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
    // Test violation of DT_I3B
    // Concept contains Demotion relationships, but no corresponding
    // ConceptRelationship
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
      assertFalse(validationResult.isValid());
    }

    //
    // Test non-violation of DT_I3B
    // Concept contains Demotion relationships, and corresponding
    // ConceptRelationships
    //
    {
      ContentServiceJpa txService = new ContentServiceJpa();
      txService.setTransactionPerOperation(false);
      txService.beginTransaction();
      Concept fresh = txService.getConcept("C0040247", "MTH", "latest", Branch.ROOT);
      addMatchingConceptRelationships(txService, fresh);
      final ValidationResult validationResult2 =
          txService.validateConcept(project.getValidationChecks(), fresh);
      txService.rollback();
      txService.close();
      assertTrue(validationResult2.isValid());
    }

    //
    // Test non-violation of DT_I3B
    // Concept contains no Demotion relationships
    //
    {
      ContentServiceJpa txService = new ContentServiceJpa();
      txService.setTransactionPerOperation(false);
      txService.beginTransaction();
      Concept fresh = txService.getConcept("C0004611", "MTH", "latest", Branch.ROOT);
      final ValidationResult validationResult3 =
          txService.validateConcept(project.getValidationChecks(), fresh);
      txService.rollback();
      txService.close();
      assertTrue(validationResult3.isValid());
    }
  }

  /**
   * Test batch mode.
   *
   * @throws Exception the exception
   */
  @Test
  public void testBatchMode() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    // 1. Read all concepts
    Logger.getLogger(getClass()).info("  Read all concept ids ");
    final List<Long> conceptIds =
        contentService.getAllConceptIds("MTH", "latest", Branch.ROOT);

    // 2. Perform the batch test
    Logger.getLogger(getClass()).info("  Validate check");
    final DT_I3B check = new DT_I3B();
    final Set<Long> failures = check.validateConcepts(new HashSet<>(conceptIds),
        "MTH", "latest", contentService);
    Logger.getLogger(getClass()).info("    count = " + failures.size());
    for (final Long id : failures) {
      Logger.getLogger(getClass())
          .info("     fail = " + contentService.getConcept(id));
    }
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
