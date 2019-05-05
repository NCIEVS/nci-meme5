/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.algo.RemoveTerminologyAlgorithm;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.ConceptSubsetMember;
import com.wci.umls.server.model.content.ConceptTreePosition;
import com.wci.umls.server.model.content.Definition;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.model.workflow.WorkflowEpoch;
import com.wci.umls.server.services.WorkflowService;

/**
 * Implementation of an algorithm to run prod mid cleanup
 */
public class ProdMidCleanupAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link ProdMidCleanupAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public ProdMidCleanupAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("PRODMIDCLEANUP");
    setLastModifiedBy("admin");
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("ProdMid Cleanup requires a project to be set");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    try {

      logInfo(
          "[ProdMid Cleanup] Removing contents for non-current terminologies.");
      commitClearBegin();

      // Find all non-current terminologies
      List<Terminology> nonCurrentTerminologies = new ArrayList<>();
      // RAW 20190504 - this was returning hundreds of terminologies that had been
      // cleaned out from previous runs, causing the process to take an extremely long time.
      // Changed to only grab nonCurrent terminologies that actually contained content
//      for (final Terminology terminology : getTerminologies().getObjects()) {
//        if (!terminology.isCurrent()) {
//          nonCurrentTerminologies.add(terminology);
//        }
//      }

      Query query = getEntityManager().createNativeQuery(
          "select distinct terminology, version from ( "+
            "select distinct a.terminology, a.version from atoms a, terminologies t where t.terminology=a.terminology and t.version=a.version and t.current = false "+
            "UNION ALL "+
            "select distinct a.terminology, a.version from concepts a, terminologies t where t.terminology=a.terminology and t.version=a.version and t.current = false "+
            "UNION ALL "+
            "select distinct a.terminology, a.version from descriptors a, terminologies t where t.terminology=a.terminology and t.version=a.version and t.current = false "+
            "UNION ALL "+
            "select distinct a.terminology, a.version from codes a, terminologies t where t.terminology=a.terminology and t.version=a.version and t.current = false) terminologies;"
          );
          
      List<Object[]> objects = query.getResultList();
      for (final Object[] entry : objects) {
        final String terminology = entry[0].toString();
        final String version = entry[1].toString();
        Terminology nonCurrentterminology = getTerminology(terminology, version);
        nonCurrentTerminologies.add(nonCurrentterminology);
      }      
      
      
      int removals = 0;

      WorkflowService workflowService = new WorkflowServiceJpa();
      workflowService.setLastModifiedBy("admin");

      WorkflowEpoch currentEpoch =
          workflowService.getCurrentWorkflowEpoch(getProject());

      Set<Long> worklistIdsToRemove = new HashSet<>();
      Set<Long> checklistIdsToRemove = new HashSet<>();
      Set<Long> conceptsWithoutAtoms = new HashSet<>();

      // Get worklists
       query =
          getEntityManager().createQuery("select a.id from " + "WorklistJpa a");

      List<Object> list = query.getResultList();
      for (final Object entry : list) {
        final Long id = Long.valueOf(entry.toString());
        worklistIdsToRemove.add(id);
      }

      // Get checklists
      query = getEntityManager()
          .createQuery("select a.id from " + "ChecklistJpa a");

      list = query.getResultList();
      for (final Object entry : list) {
        final Long id = Long.valueOf(entry.toString());
        checklistIdsToRemove.add(id);
      }

      // Get concepts without atoms
      query = getEntityManager().createQuery("select c1.id from "
          + "ConceptJpa c1 where c1.terminology = :terminology and c1.id NOT IN (select c2.id from ConceptJpa c2 JOIN c2.atoms)");
      query.setParameter("terminology", "NCIMTH");

      list = query.getResultList();
      for (final Object entry : list) {
        final Long id = Long.valueOf(entry.toString());
        Concept concept = getConcept(id);
        conceptsWithoutAtoms.add(id);
      }

      logInfo("[ProdMid Cleanup] " + checklistIdsToRemove.size()
          + " checklists to be removed");
      logInfo("[ProdMid Cleanup] " + worklistIdsToRemove.size()
          + " worklists to be removed");
      logInfo("[ProdMid Cleanup] " + conceptsWithoutAtoms.size()
          + " concepts to be removed");

      setSteps(nonCurrentTerminologies.size() + checklistIdsToRemove.size()
          + worklistIdsToRemove.size() + conceptsWithoutAtoms.size());

      // Remove checklists
      for (Long id : checklistIdsToRemove) {
        logInfo("[ProdMid Cleanup] " + id + " checklist to be removed");
        workflowService.removeChecklist(id, true);
        updateProgress();
        removals++;
      }

      // Remove worklists
      for (Long id : worklistIdsToRemove) {
        logInfo("[ProdMid Cleanup] " + id + " worklist to be removed");
        workflowService.removeWorklist(id, true);
        updateProgress();
        removals++;
      }

      logInfo("[ProdMid Cleanup] " + removals + " lists successfully removed.");

      // For each non-current terminology, run removeTerminologies on it (keep
      // the terminology itself for tracking purposes).
      for (final Terminology nonCurrentTerminology : nonCurrentTerminologies) {
        final RemoveTerminologyAlgorithm algo =
            new RemoveTerminologyAlgorithm();
        try {
          logInfo("  Removing content associated with "
              + nonCurrentTerminology.getTerminology() + "/"
              + nonCurrentTerminology.getVersion());

          algo.setLastModifiedBy(getLastModifiedBy());
          algo.setTerminology(nonCurrentTerminology.getTerminology());
          algo.setVersion(nonCurrentTerminology.getVersion());
          algo.setKeepTerminology(true);
          algo.setWorkId(getWorkId());
          algo.setActivityId(getActivityId());
          algo.setProject(getProject());
          algo.compute();
          algo.close();

        } catch (Exception e) {
          logError("Unexpected problem - " + e.getMessage());
          algo.rollback();
          algo.close();
          throw e;
        }

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      // Consider truncating action tables, log entries, etc.

      logInfo("[ProdMid Cleanup] Removed content for " + getSteps()
          + " non-current terminologies.");

      // Mark unpublished concepts without atoms and their components
      int markedConcepts = 0;
      for (final Long id : conceptsWithoutAtoms) {

        //final Long id = Long.valueOf(entry.toString());
        Concept concept = getConcept(id);
        if (concept == null) {
          continue;
        }
        concept.setPublishable(false);
        for (Definition def : concept.getDefinitions()) {
          def.setPublishable(false);
          updateDefinition(def, concept);
        }
        for (Attribute att : concept.getAttributes()) {
          att.setPublishable(false);
          updateAttribute(att, concept);
        }
        for (ConceptRelationship rel : concept.getInverseRelationships()) {
          rel.setPublishable(false);
          updateRelationship(rel);
        }
        for (ConceptRelationship rel : concept.getRelationships()) {
          rel.setPublishable(false);
          updateRelationship(rel);
        }
        for (SemanticTypeComponent sty : concept.getSemanticTypes()) {
          sty.setPublishable(false);
          updateSemanticTypeComponent(sty, concept);
        }
        for (ConceptSubsetMember member : concept.getMembers()) {
          member.setPublishable(false);
          updateSubsetMember(member);
        }
        for (ConceptTreePosition treePos : concept.getTreePositions()) {
          treePos.setPublishable(false);
          updateTreePosition(treePos);
        }
        concept.setNotes(null);
        updateConcept(concept);

        updateProgress();
        markedConcepts++;
      }

      logInfo(
          "[ProdMid Cleanup] Marked unpublished content for concepts without atoms."
              + markedConcepts);

      commitClearBegin();

      logInfo("  project = " + getProject().getId());
      logInfo("  workId = " + getWorkId());
      logInfo("  activityId = " + getActivityId());
      logInfo("  user  = " + getLastModifiedBy());
      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - No reset
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Removes contents for non-current terminologies";
  }

}