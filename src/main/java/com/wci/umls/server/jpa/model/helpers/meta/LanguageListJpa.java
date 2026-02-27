/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers.meta;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.meta.LanguageList;
import com.wci.umls.server.jpa.model.meta.LanguageJpa;
import com.wci.umls.server.model.meta.Language;

/**
 * JAXB enabled implementation of {@link LanguageList}.
 */
@XmlRootElement(name = "languageList")
public class LanguageListJpa extends AbstractResultList<Language> implements
    LanguageList {

  /* see superclass */
  @Override
  @XmlElement(type = LanguageJpa.class, name = "types")
  public List<Language> getObjects() {
    return super.getObjectsTransient();
  }

}
