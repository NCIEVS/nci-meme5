/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;
import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.algo.action.UpdateConceptMolecularAction;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.TrackingRecord;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.services.RootService;
import com.wci.umls.server.services.handlers.SearchHandler;

/**
 * Implementation of an algorithm to perform a recomputation of Metathesaurus
 * concept status based on component status and validation.
 */
public class MatrixInitializerAlgorithm extends AbstractAlgorithm {

  /** The concept ids. */
  public Set<Long> conceptIds = null;

  private Map<Long, Set<Long>> atomIdToTrackingRecordIds = new HashMap<>();

  /** Indicates whether the tracking record component id cache has been loaded. */
  private boolean atomIdToTrackingRecordIdsLoaded = false;

  /**
   * Instantiates an empty {@link MatrixInitializerAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public MatrixInitializerAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("MATRIXINIT");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    if (getProject() == null) {
      throw new Exception("Matrix initializer requires a project to be set");
    }
    // n/a - NO preconditions
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    final boolean updateMode = conceptIds != null;
    if (updateMode) {
      logInfo("  update mode = " + conceptIds.size());
    } else {
      conceptIds = getAllProjectConceptIds();
      logInfo("  full mode concept scope = " + conceptIds.size());
    }

    fireProgressEvent(0, "Starting...find publishable atoms");
    try {

      final SearchHandler handler = getSearchHandler(ConfigUtility.DEFAULT);

      // Get unpublishable concepts with publishable atoms
      final Set<Long> makePublishable =
          new HashSet<>(handler.getIdResults(getProject().getTerminology(),
              getProject().getVersion(), Branch.ROOT,
              "publishable:false AND atoms.publishable:true", null,
              ConceptJpa.class, null, new int[1], manager));
      checkCancel();
      fireProgressEvent(10, "Found concepts to make publishable");
      logInfo("  make publishable = " + makePublishable.size());

      // Get publishable concepts without any publishable atoms
      final Set<Long> publishableConcepts =
          new HashSet<>(handler.getIdResults(getProject().getTerminology(),
              getProject().getVersion(), Branch.ROOT, "publishable:true", null,
              ConceptJpa.class, null, new int[1], manager));
      checkCancel();
      final Set<Long> conceptsWithPublishableAtoms =
          new HashSet<>(handler.getIdResults(getProject().getTerminology(),
              getProject().getVersion(), Branch.ROOT, "atoms.publishable:true",
              null, ConceptJpa.class, null, new int[1], manager));
      checkCancel();

      final Set<Long> makeUnpublishable =
          Sets.difference(publishableConcepts, conceptsWithPublishableAtoms);
      fireProgressEvent(20, "Found concepts to make unpublishable");
      logInfo("  make unpublishable = " + makeUnpublishable.size());

      // Find concepts connected to needs review relationships
      final Set<String> needsReviewR = new HashSet<>();
      jakarta.persistence.Query query =
          manager.createQuery("select r from ConceptRelationshipJpa r "
              + " where terminology = :terminology and version = :version "
              + " and workflowStatus in (  :ws )");
      query.setParameter("terminology", getProject().getTerminology());
      query.setParameter("version", getProject().getVersion());
      query.setParameter("ws", WorkflowStatus.NEEDS_REVIEW);

      @SuppressWarnings("unchecked")
      final List<ConceptRelationship> rels = query.getResultList();
      for (final ConceptRelationship rel : rels) {
        needsReviewR.add("id:" + rel.getFrom().getId());
        needsReviewR.add("id:" + rel.getTo().getId());
      }
      checkCancel();
      fireProgressEvent(30, "Find concepts with NEEDS_REVIEW relationships");
      logInfo("  need review rel = " + rels.size());

      // Perform validation and collect failed concept ids
      final Set<Long> failures =
          validateConcepts(getProject(), null, conceptIds);
      checkCancel();
      fireProgressEvent(40, "Found concepts with validation failures");
      logInfo("  validation failures = " + failures.size());

      // Find NEEDS_REVIEW concepts that should be READY_FOR_PUBLICATION
      final Set<Long> makeReviewed =
          new HashSet<>(handler.getIdResults(getProject().getTerminology(),
              getProject().getVersion(), Branch.ROOT,
              "workflowStatus:NEEDS_REVIEW AND NOT atoms.workflowStatus:NEEDS_REVIEW "
                  + "AND NOT atoms.workflowStatus:DEMOTION AND NOT semanticTypes.workflowStatus:NEEDS_REVIEW "
                  + (needsReviewR.size() == 0 ? ""
                      : " AND NOT " + ConfigUtility.composeQuery("OR",
                          new ArrayList<>(needsReviewR))),
              null, ConceptJpa.class, null, new int[1], manager));
      // Remove any concept that has a validation failure
      makeReviewed.removeAll(failures);
      checkCancel();
      fireProgressEvent(50, "Found concepts to make reviewed");
      logInfo("  concepts to make reviewed = " + makeReviewed.size());

      // Find READY_FOR_PUBLICATION or PUBLISHED concepts that should be
      // NEEDS_REVIEW
      final Set<Long> makeNeedsReview =
          new HashSet<>(handler.getIdResults(getProject().getTerminology(),
              getProject().getVersion(), Branch.ROOT,
              "(workflowStatus:READY_FOR_PUBLICATION OR workflowStatus:PUBLISHED) "
                  + "AND (atoms.workflowStatus:NEEDS_REVIEW OR atoms.workflowStatus:DEMOTION "
                  + "OR semanticTypes.workflowStatus:NEEDS_REVIEW "
                  + (needsReviewR.size() == 0 ? ""
                      : " OR " + ConfigUtility.composeQuery("OR",
                          new ArrayList<>(needsReviewR)))
                  + ")",
              null, ConceptJpa.class, null, new int[1], manager));
      checkCancel();
      fireProgressEvent(60, "Found concepts to make needs review");
      logInfo("  concepts to make needs review = " + makeNeedsReview.size());

      final Set<Long> conceptsToChange = new HashSet<>();
      conceptsToChange.addAll(makePublishable);
      conceptsToChange.addAll(makeUnpublishable);
      conceptsToChange.addAll(makeReviewed);
      conceptsToChange.addAll(makeNeedsReview);
      conceptsToChange.addAll(failures);
      logInfo("  total concepts to evaluate = " + conceptsToChange.size());

      int prevProgress = 60;
      int statusChangeCt = 0;
      int publishableChangeCt = 0;
      int stepsCompleted = 0;
      int skippedConceptCt = 0;
      int foundConceptCt = 0;
      Long currentConceptId = null;
      Boolean currentPublishable = null;
      WorkflowStatus currentStatus = null;

      // Changes will be sent to a conceptUpdate molecular action
      final UpdateConceptMolecularAction action =
          new UpdateConceptMolecularAction();
      // Action will be performed in batch mode, so begin the transaction now.
      action.setTransactionPerOperation(false);
      action.beginTransaction();
      try {

        for (final Long conceptId : conceptsToChange) {
          currentConceptId = conceptId;
          currentPublishable = null;
          currentStatus = null;
          // Skip concepts outside the configured scope.
          if (conceptIds != null && !conceptIds.contains(conceptId)) {
            skippedConceptCt++;
            continue;
          }

          final Concept concept = getConcept(conceptId);

          // determine status change
          int progress = (int) (60.0
              + ((statusChangeCt * 40.0) / conceptsToChange.size()));
          if (progress != prevProgress) {
            fireProgressEvent(progress,
                "Iterate through concepts to change...");
            checkCancel();
            prevProgress = progress;
          }

          boolean found = false;
          Boolean publishable = null;
          if (makePublishable.contains(conceptId)) {
            publishable = true;
            currentPublishable = publishable;
            logInfo("  publishable change  = " + concept.getId());
            publishableChangeCt++;
            found = true;
          }

          if (makeUnpublishable.contains(conceptId)) {
            publishable = false;
            currentPublishable = publishable;
            logInfo("  unpublishable change  = " + concept.getId());
            publishableChangeCt++;
            found = true;
          }

          WorkflowStatus status = null;
          if (makeReviewed.contains(conceptId)) {
            status = WorkflowStatus.READY_FOR_PUBLICATION;
            currentStatus = status;
            logInfo("  status change  = " + concept.getId());
            statusChangeCt++;
            found = true;
            // Update tracking record
            updateTrackingRecord(concept, status);
          }

          if (makeNeedsReview.contains(conceptId)) {
            status = WorkflowStatus.NEEDS_REVIEW;
            currentStatus = status;
            statusChangeCt++;
            logInfo("  status change  = " + concept.getId());
            found = true;
            // Update tracking record
            updateTrackingRecord(concept, status);
          }

          if (failures.contains(conceptId)
              && concept.getWorkflowStatus() != WorkflowStatus.NEEDS_REVIEW) {
            status = WorkflowStatus.NEEDS_REVIEW;
            currentStatus = status;
            statusChangeCt++;
            logInfo("  status change (failure)  = " + concept.getId());
            found = true;
          }

          // If changing concept, change it
          if (found) {
            foundConceptCt++;
            // Configure the conceptUpdate molecular action

            action.setProject(getProject());
            action.setConceptId(concept.getId());
            action.setConceptId2(null);
            action.setLastModifiedBy(getLastModifiedBy());
            action.setLastModified(concept.getLastModified().getTime());
            action.setOverrideWarnings(true);
            action.setTransactionPerOperation(false);
            action.setMolecularActionFlag(true);
            action.setChangeStatusFlag(true);
            action.setActivityId(getActivityId());
            action.setWorkId(getWorkId());

            if (publishable != null) {
              action.setPublishable(publishable);
            } else {
              action.setPublishable(concept.isPublishable());
            }

            if (status != null) {
              action.setWorkflowStatus(status);
            } else {
              action.setWorkflowStatus(concept.getWorkflowStatus());
            }

            final ValidationResult result = performMolecularAction(action,
                getLastModifiedBy(), false, true);
            if (!result.isValid()) {
              throw new Exception("Invalid action - " + result);
            }

          }
          stepsCompleted++;
          if (stepsCompleted % 1000 == 0) {
            action.commitClearBegin();
          }
          logAndCommit(stepsCompleted, RootService.logCt, RootService.commitCt);
        }

        action.commitClearBegin();

      } catch (Exception e) {
        try {
          action.rollback();
        } catch (Exception rollbackException) {
          Logger.getLogger(getClass()).error(
              "Matrix initializer rollback failed after conceptId="
                  + currentConceptId,
              rollbackException);
        }
        Logger.getLogger(getClass()).error(
            "Matrix initializer failed while processing conceptId="
                + currentConceptId,
            e);
        logMatrixInitFailure("Matrix initializer failed while processing conceptId="
            + currentConceptId);
        logMatrixInitFailure("  candidate membership: makePublishable="
            + makePublishable.contains(currentConceptId)
            + ", makeUnpublishable=" + makeUnpublishable.contains(currentConceptId)
            + ", makeReviewed=" + makeReviewed.contains(currentConceptId)
            + ", makeNeedsReview=" + makeNeedsReview.contains(currentConceptId)
            + ", failure=" + failures.contains(currentConceptId));
        logMatrixInitFailure("  attempted publishable=" + currentPublishable
            + ", attempted workflowStatus=" + currentStatus);
        logMatrixInitFailure("  progress before rollback: stepsCompleted="
            + stepsCompleted + ", foundConceptCt=" + foundConceptCt
            + ", skippedConceptCt=" + skippedConceptCt
            + ", publishableChangeCt=" + publishableChangeCt
            + ", statusChangeCt=" + statusChangeCt
            + ", totalConceptsToEvaluate=" + conceptsToChange.size());
        throw e;
      } finally {
        action.close();
      }

      logInfo("  publishable changed = " + publishableChangeCt);
      logInfo("  status changed = " + statusChangeCt);
      logInfo("  concepts skipped by scope = " + skippedConceptCt);
      logInfo("  concepts with attempted changes = " + foundConceptCt);
      fireProgressEvent(100, "Finished ...");
      logInfo("Finished " + getName());

    } catch (

    Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }

  }

  /**
   * Attempt to persist Matrix Init failure details without masking the original
   * exception.
   *
   * @param message the diagnostic message
   */
  private void logMatrixInitFailure(String message) {
    try {
      logError(message);
    } catch (Exception e) {
      Logger.getLogger(getClass()).warn(
          "Unable to persist Matrix initializer failure log: " + message, e);
    }
  }

  /**
   * Returns all project concept ids from the database.
   *
   * Matrix Init uses this set as the full-mode processing scope and validation
   * input. Do not use Lucene here: the default Lucene fetch path caps unpaged
   * results at 500000, which can silently exclude valid candidate concepts.
   *
   * @return all concept ids in project scope
   * @throws Exception if the query fails
   */
  private Set<Long> getAllProjectConceptIds() throws Exception {
    final jakarta.persistence.Query query =
        getEntityManager().createQuery("select c.id from ConceptJpa c "
            + "where c.version = :version and c.terminology = :terminology "
            + "and (c.branch = :branch or c.branchedTo not like :branchMatch)");
    query.setParameter("terminology", getProject().getTerminology());
    query.setParameter("version", getProject().getVersion());
    query.setParameter("branch", Branch.ROOT);
    query.setParameter("branchMatch", "%" + Branch.ROOT + Branch.SEPARATOR + "%");

    final Set<Long> ids = new HashSet<>();
    for (final Object id : query.getResultList()) {
      ids.add(((Number) id).longValue());
    }
    return ids;
  }

  @SuppressWarnings("unchecked")
  private void updateTrackingRecord(Concept concept, WorkflowStatus status)
    throws Exception {

    if (!atomIdToTrackingRecordIdsLoaded) {
      // Cache all atomId->trackingRecordIds
      logInfo("  loading tracking record component id cache");
      jakarta.persistence.Query query =
          getEntityManager().createNativeQuery("select * from component_ids");
      final List<Object[]> list = query.getResultList();
      logInfo("  tracking record component id rows = " + list.size());
      for (final Object[] entry : list) {
        Long atomId = ((Number) entry[1]).longValue();
        Long trackingRecordId = ((Number) entry[0]).longValue();
        if (!atomIdToTrackingRecordIds.containsKey(atomId)) {
          final Set<Long> trackingRecordIds = new HashSet<>();
          atomIdToTrackingRecordIds.put(atomId, trackingRecordIds);
        }
        atomIdToTrackingRecordIds.get(atomId).add(trackingRecordId);
      }
      atomIdToTrackingRecordIdsLoaded = true;
      logInfo("  tracking record atom id cache size = "
          + atomIdToTrackingRecordIds.size());
    }

    // Get the concept's atom Ids, and find all tracking records for those atom
    // Ids
    final Set<Long> trackingRecordIds = new HashSet<>();
    final List<Long> atomIds = concept.getAtoms().stream().map(a -> a.getId())
        .collect(Collectors.toList());
    for (final Long atomId : atomIds) {
      if (atomIdToTrackingRecordIds.containsKey(atomId)) {
        trackingRecordIds.addAll(atomIdToTrackingRecordIds.get(atomId));
      }
    }

    // Any tracking record that references atoms contained in this concept may
    // potentially be updated.
    // Set trackingRecord to the passed in status
    for (final Long trackingRecordId : trackingRecordIds) {
      final TrackingRecord trackingRecord = getTrackingRecord(trackingRecordId);
      if (!trackingRecord.isFinished()
          && trackingRecord.getWorkflowStatus() != status) {
        trackingRecord.setWorkflowStatus(status);
        updateTrackingRecord(trackingRecord);
      }
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

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    return super.getParameters();
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Recompute concept status";
  }

  /**
   * Sets the concept ids.
   *
   * @param conceptIds the concept ids
   */
  public void setConceptIds(Set<Long> conceptIds) {
    this.conceptIds = conceptIds;
  }
}
