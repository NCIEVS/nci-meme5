/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.model.meta;

import com.wci.umls.server.helpers.HasId;
import com.wci.umls.server.helpers.HasName;
import com.wci.umls.server.helpers.Identity;

/**
 * Represents atom identity for Metathesaurus editing.
 */
public interface StringClassIdentity extends HasId, HasName, Identity {

  /**
   * Returns the language.
   *
   * @return the language
   */
  public String getLanguage();

  /**
   * Sets the language.
   *
   * @param language the language
   */
  public void setLanguage(String language);

}
