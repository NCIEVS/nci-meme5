/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers.content;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.content.MappingList;
import com.wci.umls.server.jpa.model.content.MappingJpa;
import com.wci.umls.server.model.content.Mapping;

/**
 * JAXB enabled implementation of {@link MappingList}.
 */
@XmlRootElement(name = "mappingList")
public class MappingListJpa extends AbstractResultList<Mapping> implements
    MappingList {

  /* see superclass */
  @Override
  @XmlElement(type = MappingJpa.class, name = "mappings")
  public List<Mapping> getObjects() {
    return super.getObjectsTransient();
  }

}
