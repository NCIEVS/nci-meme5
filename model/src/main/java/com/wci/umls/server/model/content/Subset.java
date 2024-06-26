/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.model.content;

import com.wci.umls.server.helpers.HasAlternateTerminologyIds;

/**
 * Represents a subset of content asserted by a terminology.
 */
public interface Subset
    extends ComponentHasAttributes, HasAlternateTerminologyIds {

  /**
   * Returns the description.
   * 
   * @return the description
   */
  public String getDescription();

  /**
   * Sets the description.
   * 
   * @param description the description
   */
  public void setDescription(String description);

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
   * Clear members.
   */
  public void clearMembers();
}
