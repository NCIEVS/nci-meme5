/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.model.content;

import java.util.List;

/**
 * Represents a classification of atoms within a terminology, ontology, or
 * coding scheme. For example Metathesaurus CUIs, SNOMED CT source concepts, or
 * RXCUIs within RXNORM.
 */
public interface AtomClass extends ComponentHasAttributes {

  /**
   * Returns the atoms.
   * 
   * @return the atoms
   */
  public List<Atom> getAtoms();

  /**
   * Sets the atoms.
   * 
   * @param atoms the atoms
   */
  public void setAtoms(List<Atom> atoms);

  /**
   * Adds the atom.
   * 
   * @param atom the atom
   */
  public void addAtom(Atom atom);

  /**
   * Removes the atom.
   * 
   * @param atom the atom
   */
  public void removeAtom(Atom atom);

  /**
   * Returns the preferred atom name.
   * @return the preferred atom name
   */
  public String getDefaultPreferredName();

  /**
   * Sets the default preferred name.
   *
   * @param defaultPreferredName the default preferred name
   */
  public void setDefaultPreferredName(String defaultPreferredName);

  /**
   * Returns the workflow status.
   *
   * @return the workflow status
   */
  public String getWorkflowStatus();
  
  /**
   * Sets the workflow status.
   *
   * @param workflowStatus the workflow status
   */
  public void setWorkflowStatus(String workflowStatus);

  /**
   * Returns the branched to.
   *
   * @return the branched to
   */
  public String getBranchedTo();
  
  /**
   * Sets the branched to.
   *
   * @param branchedTo the branched to
   */
  public void setBranchedTo(String branchedTo);
  
  /**
   * Adds the branched to.
   *
   * @param newBranch the new branch
   */
  public void addBranchedTo(String newBranch);
  
  /**
   * Removes the branched to.
   *
   * @param closedBranch the closed branch
   */
  public void removeBranchedTo(String closedBranch);
}
