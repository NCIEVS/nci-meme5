/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers.meta;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.meta.TermTypeList;
import com.wci.umls.server.jpa.model.meta.TermTypeJpa;
import com.wci.umls.server.model.meta.TermType;

/**
 * JAXB enabled implementation of {@link TermTypeList}.
 */
@XmlRootElement(name = "termTypeList")
public class TermTypeListJpa extends AbstractResultList<TermType> implements
    TermTypeList {

  /* see superclass */
  @Override
  @XmlElement(type = TermTypeJpa.class, name = "types")
  public List<TermType> getObjects() {
    return super.getObjectsTransient();
  }

}
