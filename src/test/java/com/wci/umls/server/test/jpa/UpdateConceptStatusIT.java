/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.algo.action.UpdateConceptMolecularAction;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.actions.MolecularActionList;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Unit testing for {@link UpdateConceptMolecularAction}.
 */
public class UpdateConceptStatusIT extends IntegrationUnitSupport {

  /** The service. */
  protected ContentServiceJpa contentService;

  /** The properties. */
  protected static Properties properties;

  /** The update concept status action/service */
  protected static UpdateConceptMolecularAction action;

  /** The concept. */
  private Concept concept;

  /** The project. */
  private Project project;

  /** The umls terminology. */
  private String umlsTerminology = "MTH";

  /** The umls version. */
  private String umlsVersion = "latest";

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // instantiate properties
    properties = PropertyUtility.getProperties();

  }

  /**
   * Setup.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {

    contentService = new ContentServiceJpa();

    // ensure there is a concept associated with the project
    ProjectList projects = contentService.getProjects();
    assertTrue(projects.size() > 0);
    project = projects.getObjects().get(0);

    // verify terminology has expected value
    assertTrue(project.getTerminology().equals(umlsTerminology));

    // instantiate required service objects
    contentService = new ContentServiceJpa();
    contentService.setLastModifiedBy("admin");
    contentService.setMolecularActionFlag(false);

    // Copy existing concept to avoid messing with actual database data.
    ConceptJpa conceptJpa = new ConceptJpa(contentService.getConcept("C0000294",
        umlsTerminology, umlsVersion, null), false);
    conceptJpa.setId(null);
    conceptJpa.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);
    conceptJpa = (ConceptJpa) contentService.addConcept(conceptJpa);

    // Re-instantiate service so it can pickup the changed concept.
    contentService = new ContentServiceJpa();

    concept = contentService.getConcept(conceptJpa.getId());

  }

  /**
   * /** Test normal use of the helper object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testUpdateConceptStatusNormalUse() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());

    //
    // Prepare the test and check prerequisites
    //
    // Due to MySQL truncating to the second, we must also truncate our comparison
    // startDate (using truncate, not round, to avoid rounding up past the stored value).
    Date startDate = DateUtils.truncate(new Date(), Calendar.SECOND);

    // Update the WorkflowStatus of the concept from READY_FOR_PUBLICATION to
    // NEEDS_REVIEW
    final UpdateConceptMolecularAction action =
        new UpdateConceptMolecularAction();
    ValidationResult validationResult = null;
    try {

      // Configure the action
      action.setProject(project);
      action.setConceptId(concept.getId());
      action.setConceptId2(null);
      action.setLastModifiedBy("admin");
      action.setLastModified(concept.getLastModified().getTime());
      action.setOverrideWarnings(false);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
      // Preserve original publishable value to avoid spurious atomic action
      action.setPublishable(concept.isPublishable());

      // Perform the action
      validationResult = action.performMolecularAction(action, "admin", true, false);

    } catch (Exception e) {
      action.rollback();
      throw e;
    } finally {
      action.close();
    }

    assertNotNull(validationResult);
    assertTrue(validationResult.getErrors().isEmpty());

    // Re-instantiate service so it can pickup the changed concept.
    contentService = new ContentServiceJpa();

    // Verify the concept's workflow Status has updated
    concept = contentService.getConcept(concept.getId());
    assertEquals(WorkflowStatus.NEEDS_REVIEW, concept.getWorkflowStatus());

    // verify the molecular action exists
    PfsParameterJpa pfs = new PfsParameterJpa();
    pfs.setSortField("lastModified");
    pfs.setAscending(false);
    MolecularActionList list = contentService.findMolecularActions(
        concept.getId(), umlsTerminology, umlsVersion, null, pfs);
    assertTrue(list.size() > 0);
    MolecularAction ma = list.getObjects().get(0);
    assertNotNull(ma);
    assertEquals(concept.getId(), ma.getComponentId());
    assertTrue(ma.getLastModified().compareTo(startDate) >= 0);
    assertNotNull(ma.getAtomicActions());

    // Verify that one atomic actions exists for updating concept workflow
    // status
    pfs.setSortField(null);

    List<AtomicAction> atomicActions =
        contentService.findAtomicActions(ma.getId(), null, pfs).getObjects();
    Collections.sort(atomicActions,
        (a1, a2) -> a1.getId().compareTo(a2.getId()));
    assertEquals(1, atomicActions.size());
    assertEquals("CONCEPT", atomicActions.get(0).getIdType().toString());
    assertNotNull(atomicActions.get(0).getOldValue());
    assertNotNull(atomicActions.get(0).getNewValue());
    assertEquals("workflowStatus", atomicActions.get(0).getField());

  }

  /*
   * Test degenerate use of the helper object.
   * 
   * @throws Exception the exception
   */
  /**
   * Test helper degenerate use.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHelperDegenerateUse() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    // n/a
  }

  /**
   * Test edge cases of the helper object.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHelperEdgeCases() throws Exception {
    Logger.getLogger(getClass()).info("TEST " + name.getMethodName());
    // n/a
  }

  /**
   * Teardown.
   * @throws Exception
   */
  @After
  public void teardown() throws Exception {
    // Delete copies of concepts created during this test
    contentService = new ContentServiceJpa();
    contentService.setLastModifiedBy("admin");
    contentService.setMolecularActionFlag(false);

    if (concept != null && contentService.getConcept(concept.getId()) != null) {
      contentService.removeConcept(concept.getId());
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
