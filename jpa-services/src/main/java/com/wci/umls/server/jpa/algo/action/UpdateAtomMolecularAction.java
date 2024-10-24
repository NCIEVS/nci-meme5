/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.action;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.services.helper.IndexUtility;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * A molecular action for adding an atom.
 */
public class UpdateAtomMolecularAction extends AbstractMolecularAction {

  /** The atom. */
  private Atom atom;

  /**
   * Instantiates an empty {@link UpdateAtomMolecularAction}.
   *
   * @throws Exception the exception
   */
  public UpdateAtomMolecularAction() throws Exception {
    super();
    // n/a
  }

  /**
   * Returns the atom.
   *
   * @return the atom
   */
  public Atom getAtom() {
    return atom;
  }

  /**
   * Sets the atom.
   *
   * @param atom the atom
   */
  public void setAtom(Atom atom) {
    this.atom = atom;
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResult validationResult = new ValidationResultJpa();
    // Perform "adding an atom" specific validation - n/a

    // Metadata referential integrity checking
    if (getTermType(atom.getTermType(), getConcept().getTerminology(),
        getConcept().getVersion()) == null) {
      rollback();
      throw new LocalException(
          "Cannot update atom with invalid term type - " + atom.getTermType());
    }
    if (getLanguage(atom.getLanguage(), getConcept().getTerminology(),
        getConcept().getVersion()) == null) {
      rollback();
      throw new LocalException(
          "Cannot update atom with invalid language - " + atom.getLanguage());
    }
    if (getTerminology(atom.getTerminology(), atom.getVersion()) == null) {
      rollback();
      throw new LocalException("Cannot update atom with invalid terminology - "
          + atom.getTerminology() + ", version: " + atom.getVersion());
    }

    // Cannot change any field that would affect the identity of the atom:
    // codeId, conceptId, descriptorId, stringClassId, termType, terminology,
    // terminologyId
    final Atom oldAtom = getAtom(atom.getId());

    // The only fields that should be getting updated through here is
    // "suppressible", "obsolete", "publishable", or "workflowStauts"
    // If any other field is changing, error out.
    List<String> changeAllowedGetMethods = Arrays.asList("isSuppressible",
        "isObsolete", "isPublishable", "getWorkflowStatus");

    List<Method> allGetMethods =
        IndexUtility.getAllColumnGetMethods(AtomJpa.class);

    for (Method method : allGetMethods) {
      if (!changeAllowedGetMethods.contains(method.getName())) {
        final Object origValue = method.invoke(oldAtom);
        final Object newValue = method.invoke(getAtom());
        final String origValueStr =
            origValue != null ? origValue.toString() : "";
        final String newValueStr = newValue != null ? newValue.toString() : "";
        if (!origValueStr.toString().equals(newValueStr.toString())) {
          final String fieldName =
              method.toString().substring(3, 4).toLowerCase()
                  + method.toString().substring(4);
          throw new Exception("Error: change detected in unexpected field "
              + fieldName + " for atom " + atom.getName());
        }
      }
    }

    // Check preconditions
    validationResult.merge(super.checkPreconditions());
    validationResult.merge(
        super.validateAtom(getProject().getValidationChecks(), getAtom()));
    return validationResult;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    //
    // Perform the "adding an atom" (contentService will create atomic "adding
    // an atom"s for CRUD operations)
    //

    // Let the update choose the new workflow status for the atom, unless null
    // then set to needs review
    if (atom.getWorkflowStatus() == null) {
      atom.setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
    }

    // Update the atom
    updateAtom(atom);

    // Change status of the concept
    if (getChangeStatusFlag()) {
      getConcept().setWorkflowStatus(WorkflowStatus.NEEDS_REVIEW);
    }

    // update the concept
    updateConcept(getConcept());
  }

  /* see superclass */
  @Override
  public void logAction() throws Exception {

    // log the REST call
    addLogEntry(getLastModifiedBy(), getProject().getId(), getConcept().getId(),
        getActivityId(), getWorkId(), getName() + " " + atom);

    // Log for the molecular action report
    addLogEntry(getLastModifiedBy(), getProject().getId(),
        getMolecularAction().getId(), getActivityId(), getWorkId(),
        "\nACTION  " + getName() + "\n  concept = " + getConcept().getId() + " "
            + getConcept().getName() + "\n  atom = " + atom.getName() + ", "
            + atom.getTerminology() + "/" + atom.getTermType() + ","
            + atom.getCodeId() + ", " + atom.isPublishable() + ", "
            + atom.isSuppressible() + ", " + atom.getWorkflowStatus());

  }

}
