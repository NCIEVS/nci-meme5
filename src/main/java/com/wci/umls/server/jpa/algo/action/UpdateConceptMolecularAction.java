/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * A molecular action for updating a concept workflow status.
 */
public class UpdateConceptMolecularAction extends AbstractMolecularAction {

  /** The workflow status. */
  private WorkflowStatus workflowStatus;

  /** The publishable. */
  private boolean publishable;

  /** Indicates whether to cascade unpublishable status to concept rels. */
  private boolean cascadeUnpublishableRelationships = false;

  /**
   * Instantiates an empty {@link UpdateConceptMolecularAction}.
   *
   * @throws Exception the exception
   */
  public UpdateConceptMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Sets the workflow status.
   *
   * @param workflowStatus the workflow status
   */
  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;
  }

  /**
   * Sets the publishable.
   *
   * @param publishable the publishable
   */
  public void setPublishable(boolean publishable) {
    this.publishable = publishable;
  }

  /**
   * Sets whether unpublishable concept status should cascade to relationships.
   *
   * @param cascadeUnpublishableRelationships the cascade flag
   */
  public void setCascadeUnpublishableRelationships(
    boolean cascadeUnpublishableRelationships) {
    this.cascadeUnpublishableRelationships = cascadeUnpublishableRelationships;
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    // Perform action specific validation - n/a

    // Metadata referential integrity checking

    // Check preconditions
    return super.checkPreconditions();
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void compute() throws Exception {
    //
    // Perform the action (contentService will create atomic actions for CRUD
    // operations)
    //

    // Make a copy of the concept
    Concept updateConcept = new ConceptJpa(getConcept(), true);

    //
    // Change the concept in the limited ways supported
    //
    updateConcept.setWorkflowStatus(this.workflowStatus);
    updateConcept.setPublishable(publishable);

    //
    // update the Concept
    //
    updateConcept(updateConcept);

    if (cascadeUnpublishableRelationships && !publishable) {
      makeConceptRelationshipsUnpublishable(getConcept());
    }

  }

  /**
   * Makes all concept relationships connected to the concept unpublishable.
   *
   * @param concept the concept
   * @throws Exception the exception
   */
  private void makeConceptRelationshipsUnpublishable(Concept concept)
    throws Exception {

    final Set<Long> processedRelationshipIds = new HashSet<>();
    makeConceptRelationshipsUnpublishable(concept.getRelationships(),
        processedRelationshipIds);
    makeConceptRelationshipsUnpublishable(concept.getInverseRelationships(),
        processedRelationshipIds);
  }

  /**
   * Makes the specified concept relationships unpublishable.
   *
   * @param relationships the relationships
   * @param processedRelationshipIds the processed relationship ids
   * @throws Exception the exception
   */
  private void makeConceptRelationshipsUnpublishable(
    List<ConceptRelationship> relationships, Set<Long> processedRelationshipIds)
    throws Exception {

    for (final ConceptRelationship relationship : relationships) {
      if (!processedRelationshipIds.add(relationship.getId())) {
        continue;
      }
      for (final Attribute attribute : relationship.getAttributes()) {
        if (attribute.isPublishable()) {
          attribute.setPublishable(false);
          updateAttribute(attribute, relationship);
        }
      }
      if (relationship.isPublishable()) {
        relationship.setPublishable(false);
        updateRelationship(relationship);
      }
    }
  }

  /* see superclass */
  @Override
  public void logAction() throws Exception {

    // log the REST calls
    addLogEntry(getLastModifiedBy(), getProject().getId(), getConcept().getId(),
        getActivityId(), getWorkId(), getName() + " " + getConcept());

    // Log for the molecular action report
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        getMolecularAction().getId(), getActivityId(), getWorkId(),
        "\nACTION  " + getName() + "\n  concept = " + getConcept().getId() + " "
            + getConcept().getName() + ", " + getConcept().isPublishable()
            + ", " + getConcept().isSuppressible() + ", "
            + getConcept().getWorkflowStatus());
  }

}
