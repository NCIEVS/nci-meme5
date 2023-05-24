/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.content.AtomRelationshipJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.jpa.content.ConceptNoteJpa;
import com.wci.umls.server.jpa.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.content.SemanticTypeComponentJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * A molecular action for merging two concepts.
 */
public class MergeMolecularAction extends AbstractMolecularAction {

  /**
   * Instantiates an empty {@link MergeMolecularAction}.
   *
   * @throws Exception the exception
   */
  public MergeMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the from concept.
   *
   * @return the from concept
   */
  public Concept getFromConcept() {
    return getConcept();
  }

  /**
   * Returns the to concept.
   *
   * @return the to concept
   */
  public Concept getToConcept() {
    return getConcept2();
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResult validationResult = new ValidationResultJpa();
    // Perform action specific validation - n/a

    // Metadata referential integrity checking

    // Check to make sure concepts are different
    if (getFromConcept() == getToConcept()) {
      throw new LocalException(
          "Cannot merge concept " + getFromConcept().getId() + " into concept "
              + getToConcept().getId() + " - identical concept.");
    }

    // Merging concepts must be from the same terminology
    if (!(getFromConcept().getTerminology().toString()
        .equals(getToConcept().getTerminology().toString()))) {
      throw new LocalException(
          "Two concepts must be from the same terminology to be merged, but concept "
              + getFromConcept().getId() + " has terminology "
              + getFromConcept().getTerminology() + ", and Concept "
              + getToConcept().getId() + " has terminology "
              + getToConcept().getTerminology());
    }

    // Check superclass validation
    validationResult.merge(super.checkPreconditions());
    return validationResult;

  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("rawtypes")
  /* see superclass */
  @Override
  public void compute() throws Exception {
    //
    // Perform the action (contentService will create atomic actions for CRUD
    // operations)
    //

    // Get all "inverse" relationships for the "to" and "from" concepts (e.g.
    // where toId is the id)
    final Map<Long, ConceptRelationship> inverseFromRelsMap = new HashMap<>();
    final Map<Long, ConceptRelationship> inverseToRelsMap = new HashMap<>();
    for (final Relationship rel : findConceptRelationships(null,
        getTerminology(), getVersion(), Branch.ROOT,
        "toId:" + getFromConcept().getId(), false, null).getObjects()) {
      final ConceptRelationship crel =
          new ConceptRelationshipJpa((ConceptRelationship) rel, false);
      if (inverseFromRelsMap.containsKey(crel.getFrom().getId())) {
        throw new Exception("Multiple concept level relationships from "
            + crel.getFrom().getId());
      }
      inverseFromRelsMap.put(crel.getFrom().getId(), crel);
    }
    for (final Relationship rel : findConceptRelationships(null,
        getTerminology(), getVersion(), Branch.ROOT,
        "toId:" + getToConcept().getId(), false, null).getObjects()) {
      final ConceptRelationship crel =
          new ConceptRelationshipJpa((ConceptRelationship) rel, false);
      if (inverseToRelsMap.containsKey(crel.getFrom().getId())) {
        throw new Exception("Multiple concept level relationships from "
            + crel.getFrom().getId());
      }
      inverseToRelsMap.put(crel.getTo().getId(), crel);
    }

    // Copy atoms in "from" concept.
    // Also, if any atom has a demotion to the "to" concept, remove it, and
    // create a copy for later deletion.
    // Keep track of each remaining demoted atom id pairs for later
    final List<Atom> fromAtomsCopies = new ArrayList<>();
    final Map<Long, Long> demotionAtomIdPairs = new HashMap<>();
    final List<AtomRelationship> demotionCopies = new ArrayList<>();
    for (final Atom atom : getFromConcept().getAtoms()) {
      Atom atomCopy = new AtomJpa(atom, true);
      fromAtomsCopies.add(atomCopy);
      for (final AtomRelationship atomRel : new ArrayList<AtomRelationship>(
          atom.getRelationships())) {
        if (atomRel.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
          if (getToConcept().getAtoms().contains(atomRel.getTo())) {
            if (atomCopy.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
              atomCopy.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
            }
            atomCopy.getRelationships().remove(atomRel);
            updateAtom(atomCopy);
            demotionCopies.add(new AtomRelationshipJpa(atomRel, false));
          } else {
            demotionAtomIdPairs.put(atomRel.getFrom().getId(),
                atomRel.getTo().getId());
          }
        }
      }
    }

    // If any atom in the toConcept has a demotion to the "from" concept, remove
    // it, update the atom, and create a copy of the demotion for later
    // deletion.
    // Keep track of each remaining demoted atom id pairs for later
    for (final Atom atom : getToConcept().getAtoms()) {
      for (final AtomRelationship atomRel : new ArrayList<AtomRelationship>(
          atom.getRelationships())) {
        if (atomRel.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
          if (getFromConcept().getAtoms().contains(atomRel.getTo())) {
            Atom atomCopy = new AtomJpa(atom, true);
            if (atomCopy.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
              atomCopy.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
            }
            atomCopy.getRelationships().remove(atomRel);
            updateAtom(atomCopy);
            demotionCopies.add(new AtomRelationshipJpa(atomRel, false));
          } else {
            demotionAtomIdPairs.put(atomRel.getFrom().getId(),
                atomRel.getTo().getId());
          }
        }
      }
    }

    // Copy stys in "from" concept
    final List<SemanticTypeComponent> fromStysCopies = new ArrayList<>();
    for (final SemanticTypeComponent sty : getFromConcept()
        .getSemanticTypes()) {
      fromStysCopies.add(new SemanticTypeComponentJpa(sty));
    }

    // Copy rels in "from" concept
    final List<ConceptRelationship> fromRelCopies = new ArrayList<>();
    for (final ConceptRelationship rel : getFromConcept().getRelationships()) {
      fromRelCopies.add(new ConceptRelationshipJpa(rel, true));
    }

    // Copy notes in "from" concept
    final List<Note> fromNotesCopies = new ArrayList<>();
    for (final Note note : getFromConcept().getNotes()) {
      fromNotesCopies.add(new ConceptNoteJpa((ConceptNoteJpa) note));
    }

    // Prep a list of concepts to update after object removal
    final Set<Concept> conceptsChanged = new HashSet<>();
    conceptsChanged.add(getFromConcept());
    conceptsChanged.add(getToConcept());

    //
    // Remove all objects from the fromConcept
    //
    getFromConcept().getAtoms().clear();
    getFromConcept().getSemanticTypes().clear();
    getFromConcept().getRelationships().clear();
    getFromConcept().getNotes().clear();

    // Remove the inverse "from" relationships
    for (final ConceptRelationship rel : inverseFromRelsMap.values()) {
      // If this is a rel between "from" and "to" remove it
      if (rel.getFrom().getId().equals(getToConcept().getId())) {
        removeById(getToConcept().getRelationships(), rel.getId());
      }
      // Otherwise remove it from the concept on the other end of the
      // relationship
      else {
        final Concept inverseConcept = new ConceptJpa(rel.getFrom(), true);
        removeById(inverseConcept.getRelationships(), rel.getId());
        conceptsChanged.add(inverseConcept);
      }
    }

    //
    // Update all the concepts changed by the above section
    //
    for (final Concept inverseConcept : conceptsChanged) {
      updateConcept(inverseConcept);
    }

    //
    // Remove objects
    //

    // Remove the "from" semantic types
    for (final SemanticTypeComponent sty : fromStysCopies) {
      removeSemanticTypeComponent(sty.getId());
    }
    // Remove the "from" relationships
    for (final ConceptRelationship rel : fromRelCopies) {
      removeRelationship(rel.getId(), rel.getClass());
    }
    // Remove the inverses of the "from" relationships
    for (final ConceptRelationship rel : inverseFromRelsMap.values()) {
      removeRelationship(rel.getId(), rel.getClass());
    }
    // Remove demotions between atoms in the "to" and "from" concepts
    for (final AtomRelationship rel : demotionCopies) {
      removeRelationship(rel.getId(), rel.getClass());
    }
    // Remove the "from" notes
    for (final Note note : fromNotesCopies) {
      removeNote(note.getId(), ConceptNoteJpa.class);
    }

    // Note: don't remove atoms - we just move them instead

    //
    // If both "from" and "to" have relationships to the same third concept,
    // and the changeStatus flag is set, mark the "to" relationship as needs
    // review
    // and as per above the "from" relationship will be removed.
    //
    if (getChangeStatusFlag()) {
      for (final ConceptRelationship toRel : getToConcept()
          .getRelationships()) {
        if (inverseFromRelsMap.containsKey(toRel.getTo().getId())) {
          toRel.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
          updateRelationship(toRel);
        }
      }

      for (final ConceptRelationship inverseToRel : inverseToRelsMap.values()) {
        if (inverseFromRelsMap.containsKey(inverseToRel.getFrom().getId())) {
          inverseToRel.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
          updateRelationship(inverseToRel);
        }
      }
    }

    //
    // Update and add objects
    //
    conceptsChanged.clear();
    conceptsChanged.add(getToConcept());

    // Set workflow status of "from" atoms and add to the "to" concept.
    for (final Atom atom : fromAtomsCopies) {
      if (getChangeStatusFlag()) {
        atom.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
        updateAtom(atom);
      }
      getToConcept().getAtoms().add(atom);
    }

    // Add new semantic types and wire them to "to" concept (unless they match)
    final Set<String> toConceptStys = getToConcept().getSemanticTypes().stream()
        .map(sty -> sty.getSemanticType()).collect(Collectors.toSet());
    for (SemanticTypeComponent sty : fromStysCopies) {
      // Only create semantic type if it already exists in toConcept
      if (!toConceptStys.contains(sty.getSemanticType())) {
        sty.setId(null);
        if (getChangeStatusFlag()) {
          sty.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
        }
        final SemanticTypeComponent newSty =
            addSemanticTypeComponent(sty, getToConcept());
        getToConcept().getSemanticTypes().add(newSty);
      }
    }

    // update "from" concept relationships that do not match "to" concept
    // relationships.
    for (final ConceptRelationship rel : fromRelCopies) {

      // Only copy over relationship if
      // It's NOT between from and to concept, and
      // it won't overwrite an existing relationship.
      if (!rel.getTo().getId().equals(getToConcept().getId())
          && inverseToRelsMap.containsKey(rel.getTo().getId())) {
        rel.setId(null);
        rel.setFrom(getToConcept());
        if (getChangeStatusFlag()) {
          rel.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
        }
        addRelationship(rel);
        getToConcept().getRelationships().add(rel);
      }
    }

    // Corresponding logic for inverse rels
    for (final ConceptRelationship rel : inverseFromRelsMap.values()) {
      // Only copy over relationship if
      // It's NOT between from and to concept, and
      // it won't overwrite an existing relationship.
      if (!rel.getFrom().getId().equals(getToConcept().getId())
          && inverseToRelsMap.containsKey(rel.getFrom().getId())) {
        rel.setId(null);
        rel.setTo(getToConcept());
        if (getChangeStatusFlag()) {
          rel.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
        }
        addRelationship(rel);
        final Concept concept = new ConceptJpa(rel.getFrom(), true);
        concept.getRelationships().add(rel);
        conceptsChanged.add(concept);
      }
    }

    // If new combined concept has both a concept relationship and a demotion to
    // another concept, remove the demotion
    if (!demotionAtomIdPairs.isEmpty()) {
      // Get all of the atoms contained by related concepts
      final Set<Long> relatedConceptsAtomIds = new HashSet<>();
      for (ConceptRelationship conceptRelationship : getToConcept()
          .getRelationships()) {
        for (Atom atom : conceptRelationship.getTo().getAtoms()) {
          relatedConceptsAtomIds.add(atom.getId());
        }
      }

      // For each atom demotion relationship, if the destination atom is
      // in a concept that has a concept-level relationship with this one,
      // remove the demotion
      for (Long fromAtomId : demotionAtomIdPairs.keySet()) {
        Long toAtomId = demotionAtomIdPairs.get(fromAtomId);
        if (relatedConceptsAtomIds.contains(toAtomId)) {
          final Atom fromAtom = getAtom(fromAtomId);
          final Atom toAtom = getAtom(toAtomId);
          AtomRelationship fromDemotionRelationship = null;
          AtomRelationship toDemotionRelationship = null;

          for (AtomRelationship atomRelationship : fromAtom
              .getRelationships()) {
            if (atomRelationship.getTo().getId().equals(toAtomId)) {
              fromDemotionRelationship = atomRelationship;
              break;
            }
          }
          for (AtomRelationship atomRelationship : toAtom.getRelationships()) {
            if (atomRelationship.getTo().getId().equals(fromAtomId)) {
              toDemotionRelationship = atomRelationship;
              break;
            }
          }

          if(fromDemotionRelationship == null || toDemotionRelationship == null){
            logWarn("Unexpected null demotion relationship between atoms " + fromAtomId + " and " + toAtomId);
            continue;
          }
          
          if (fromAtom.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
            fromAtom.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
          }
          fromAtom.getRelationships().remove(fromDemotionRelationship);
          updateAtom(fromAtom);
          removeRelationship(fromDemotionRelationship.getId(),
              fromDemotionRelationship.getClass());

          if (toAtom.getWorkflowStatus().equals(WorkflowStatus.DEMOTION)) {
            toAtom.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
          }
          toAtom.getRelationships().remove(toDemotionRelationship);
          updateAtom(toAtom);
          removeRelationship(toDemotionRelationship.getId(),
              toDemotionRelationship.getClass());
        }
      }
    }

    // Add new notes and wire them to "to" concept
    for (Note note : fromNotesCopies) {
      note.setId(null);
      final Note newNote = addNote(note);
      final ConceptNoteJpa conceptNote = (ConceptNoteJpa) newNote;
      conceptNote.setConcept(getToConcept());
      getToConcept().getNotes().add(conceptNote);
    }

    //
    // Change status of the concept
    //
    if (getChangeStatusFlag()) {
      getToConcept().setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
    }

    //
    // update the to and from Concepts, and all concepts a relationship has been
    // added to
    //
    for (final Concept concept : conceptsChanged) {
      updateConcept(concept);
    }

    //
    // Delete the from concept
    //
    removeConcept(getFromConcept().getId());
  }

  /* see superclass */
  @Override
  public void logAction() throws Exception {

    // log the REST calls
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        getFromConcept().getId(), getActivityId(), getWorkId(),
        getName() + " concept " + getFromConcept().getId() + " into concept "
            + getToConcept().getId());
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        getToConcept().getId(), getActivityId(), getWorkId(),
        getName() + " concept " + getToConcept().getId() + " from concept "
            + getFromConcept().getId());

    // Log for the molecular action report
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        getMolecularAction().getId(), getActivityId(), getWorkId(),
        "\nACTION  " + getName() + "\n  concept (from) = "
            + getFromConcept().getId() + " " + getFromConcept().getName()
            + (getToConcept() != null ? "\n  concept (to) = "
                + getToConcept().getId() + " " + getToConcept().getName()
                : ""));

  }

  /* see superclass */
  @Override
  public boolean lockRelatedConcepts() {
    return true;
  }

}
