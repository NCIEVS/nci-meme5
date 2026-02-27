/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.WorklistList;
import com.wci.umls.server.jpa.model.workflow.WorklistJpa;
import com.wci.umls.server.model.workflow.Worklist;


/**
 * JAXB enabled implementation of {@link WorklistList}.
 */
@XmlRootElement(name = "worklistList")
public class WorklistListJpa extends AbstractResultList<Worklist>
    implements WorklistList {

  /* see superclass */
  @Override
  @XmlElement(type = WorklistJpa.class, name = "worklists")
  public List<Worklist> getObjects() {
    return super.getObjectsTransient();
  }

}
