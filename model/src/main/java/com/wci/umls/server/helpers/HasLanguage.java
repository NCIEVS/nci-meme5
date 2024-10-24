/**
 * Copyright 2016 West Coast Informatics, LLC
 */
/*************************************************************
 * HasLanguage: HasLanguage.java
 * Last Updated: Feb 27, 2009
 *************************************************************/
package com.wci.umls.server.helpers;


/**
 * Represents a thing that has a language.
 */
public interface HasLanguage {

  /**
   * Returns the language.
   * 
   * @return the language
   */
  public String getLanguage();

  /**
   * Sets the language
   * 
   * @param language the language
   */
  public void setLanguage(String language);

}
