/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.validation;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.wci.umls.server.Project;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.algo.action.MolecularActionAlgorithm;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.action.AbstractMolecularAction;
import com.wci.umls.server.jpa.algo.action.MergeMolecularAction;
import com.wci.umls.server.jpa.algo.action.MoveMolecularAction;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.services.ContentService;

/**
 * Validates merges between two {@link Concept}s where both contain publishable
 * current version <code>MSH</code> {@link Atom}s.
 *
 */
public class MGV_C extends AbstractValidationCheck {

  /* see superclass */
  @Override
  public void setProperties(Properties p) {
    // n/a
  }

  /* see superclass */
  @SuppressWarnings("unused")
  @Override
  public ValidationResult validateAction(MolecularActionAlgorithm action) {
    ValidationResult result = new ValidationResultJpa();

    // Only run this check on merge and move actions
    if (!(action instanceof MergeMolecularAction || action instanceof MoveMolecularAction)){
      return result;
    }
    
    final Project project = action.getProject();
    final ContentService service = (AbstractMolecularAction) action;
    final Concept source = (action instanceof MergeMolecularAction
        ? action.getConcept2() : action.getConcept());
    final Concept target = (action instanceof MergeMolecularAction
        ? action.getConcept() : action.getConcept2());
    final List<Atom> source_atoms = (action instanceof MoveMolecularAction
        ? ((MoveMolecularAction)action).getMoveAtoms() : source.getAtoms());

    //
    // Get target MSH atoms
    //
    List<Atom> target_atoms =
        target.getAtoms().stream().filter(a -> a.getTerminology().equals("MSH"))
            .collect(Collectors.toList());

    List<Atom> l_source_atoms =
        source_atoms.stream().filter(a -> a.getTerminology().equals("MSH"))
            .collect(Collectors.toList());

    //
    // Look for cases where both source and target contain
    // publishable current-version MSH atoms.
    //
    for (Atom sourceAtom : l_source_atoms) {
      if (sourceAtom.isPublishable()) {
        for (Atom targetAtom : target_atoms) {
          if (targetAtom.isPublishable()) {
            result.getErrors().add(
                getName() + ": Source and target concepts both contain publishable MSH atoms.");
            return result;
          }
        }
      }
    }
    return result;
  }

  /* see superclass */
  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

}
