/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.HasId;
import com.wci.umls.server.helpers.HasLastModified;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;

/**
 * A molecular action for redoing a previously undone action.
 */
public class RedoMolecularAction extends AbstractMolecularAction {

  /** The molecular action id. */
  private Long molecularActionId;

  /** Whether to force the redo, regardless of current object state. */
  private Boolean force = false;
  
  /** The redo molecular action. */
  private MolecularAction redoMolecularAction = null;

  /**
   * Instantiates an empty {@link RedoMolecularAction}.
   *
   * @throws Exception the exception
   */
  public RedoMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the molecular action id.
   *
   * @return the molecular action id
   */
  public Long getMolecularActionId() {
    return molecularActionId;
  }

  /**
   * Sets the molecular action id.
   *
   * @param molecularActionId the molecular action id
   */
  public void setMolecularActionId(Long molecularActionId) {
    this.molecularActionId = molecularActionId;
  }

  /**
   * Sets the force.
   *
   * @param force the force
   */
  public void setForce(Boolean force) {
    this.force = force;
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
    final ValidationResult validationResult = new ValidationResultJpa();
    // Perform action specific validation - n/a

    // Metadata referential integrity checking

    // Check to make sure action has already been undone
    if (!getMolecularAction(molecularActionId).isUndoneFlag()) {
      throw new LocalException("Cannot redo Molecular action "
          + molecularActionId + " - it has not been undone.");
    }

    // Check preconditions
    validationResult.merge(super.checkPreconditions());
    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  /* see superclass */
  @Override
  public void compute() throws Exception {

    // Call up the molecular Action we're redoing
    redoMolecularAction =
        getMolecularAction(molecularActionId);

    // Perform the opposite action for each of the molecular action's atomic
    // actions
    final List<AtomicAction> atomicActions =
        redoMolecularAction.getAtomicActions();

    // Sort actions by id (order inserted into DB)
    Collections.sort(atomicActions,
        (a1, a2) -> a1.getId().compareTo(a2.getId()));

    // Iterate through atomic actions IN ORDER
    for (final AtomicAction a : atomicActions) {

      //
      // Redo add (null old value with "id" field)
      //
      if (isAddAction(a)) {

        // Get the class of the object we're looking for, so we can pass it into
        // the Hibernate query

        final AuditReader reader = AuditReaderFactory.get(manager);
        final AuditQuery query =
            reader.createQuery()
                // last updated revision
                .forRevisionsOfEntity(Class.forName(a.getClassName()), true,
                    true)
                .addProjection(AuditEntity.revisionNumber().max())
                // add id and owner as constraints
                .add(AuditEntity.property("id").eq(a.getObjectId()));
        final Number revision = (Number) query.getSingleResult();
        HasLastModified returnedObject =
            (HasLastModified) reader.find(Class.forName(a.getClassName()),
                a.getClassName(), a.getObjectId(), revision, true);

        // Recover the object here (id is set already so this works better than
        // "add")
        // TODO - hack alert. If the object returned is a concept, it will have
        // all some of its component objects intact, and and they need to be
        // stripped out before it can be re-added
        //
        // In addition, created concepts are coming back with TerminologyId,
        // even though they were set to "" before being deleted. Reset to "" so
        // the following modify action field can complete.

        if (returnedObject instanceof ConceptJpa) {
          returnedObject =
              new ConceptJpa((ConceptJpa) returnedObject,
                  false);
          if (((ConceptJpa) returnedObject).getTerminologyId()
              .equals(((ConceptJpa) returnedObject).getId().toString())) {
            ((ConceptJpa) returnedObject).setTerminologyId("");
          }
        }
        updateHasLastModified(returnedObject);
      }

      //
      // Redo remove
      //
      else if (isRemoveAction(a)) {

        // Get the object that was added, and make sure it still exists
        final Object referencedObject = getReferencedObject(a);
        removeObject(referencedObject);

      }

      //
      // Redo a collections action
      //
      else if (isCollectionsAction(a)) {

        // Obtain the object with a collection (based on the class name)
        final HasLastModified containerObject = getReferencedObject(a);
        // Obtain the referenced object (based on collection class name and the
        // old/new value)
        final HasLastModified referencedObject =
            getReferencedCollectionObject(a);
        // Based on invoking the collection method based on the field name
        final Collection collection = getCollection(a, containerObject);

        // If the action was to add to the collection, add it
        if (a.getOldValue() == null && a.getNewValue() != null) {
          collection.add(referencedObject);
        }

        // If the action was to remove from the collection, remove it
        else if (a.getNewValue() == null && a.getOldValue() != null) {
          // In case there are multiple hash-identical objects contained in the
          // same object, remove by index.
          Predicate<HasId> predicate =
              p -> p.getId().toString().equals(a.getOldValue());
          collection.removeIf(predicate);
        }

        // otherwise fail
        else {
          throw new Exception("Unexpected combination of old/new values - "
              + a.getOldValue() + ", " + a.getNewValue());
        }

        // Update the container object
        updateHasLastModified(containerObject);

      }

      //
      // Redo a field change
      //
      else if (isChangeAction(a)) {

        // Get the object that was modified, and make sure it still exists
        final HasLastModified referencedObject = getReferencedObject(a);

        // Get the get/set methods for the field
        final Method getMethod = getColumnGetMethod(a);
        final Method setMethod = getColumnSetMethod(a);

        if (getMethod == null) {
          throw new Exception(
              "Unable to find get method for field " + a.getField());
        }
        if (setMethod == null) {
          throw new Exception(
              "Unable to find set method for field " + a.getField());
        }

        // If force is not set, check to make sure the field still has the old
        // state
        final Object origValue = getMethod.invoke(referencedObject);
        if (!force
            && !origValue.toString().equals(a.getOldValue().toString())) {
          throw new Exception("Error: field " + a.getField() + " in "
              + referencedObject + " no longer has value: " + a.getOldValue());
        }

        // If all is well, set the field to the new value
        final Object value =
            getObjectForValue(getMethod.getReturnType(), a.getNewValue());
        setMethod.invoke(referencedObject, value);
        updateHasLastModified(referencedObject);

      }

    }

    // Unset the molecular action flag
    redoMolecularAction.setUndoneFlag(false);
    this.updateMolecularAction(redoMolecularAction);
  }


  /* see superclass */
  @Override
  public void logAction() throws Exception {

    // log the REST call
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        redoMolecularAction.getComponentId(), getActivityId(), getWorkId(),
        getName() + " " + redoMolecularAction.getName() + ", "
            + molecularActionId);

    if (redoMolecularAction.getComponentId2() != null) {
      addLogEntry(getLastModifiedBy(), getProject().getId(),
          redoMolecularAction.getComponentId2(), getActivityId(), getWorkId(),
          getName() + " " + redoMolecularAction.getName() + ", "
              + molecularActionId);
    }

  }  
  
}
