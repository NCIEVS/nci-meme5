/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers.content;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.content.SubsetList;
import com.wci.umls.server.jpa.content.AbstractSubset;
import com.wci.umls.server.model.content.Subset;

/**
 * JAXB enabled implementation of {@link SubsetList}.
 */
@XmlRootElement(name = "subsetList")
public class SubsetListJpa extends AbstractResultList<Subset> implements
    SubsetList {

  /* see superclass */
  @Override
  @XmlElement(type = AbstractSubset.class, name = "subsets")
  public List<Subset> getObjects() {
    return super.getObjectsTransient();
  }

}
