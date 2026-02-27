/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.model.algo.ProcessExecution;
import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.ProcessExecutionList;
import com.wci.umls.server.jpa.model.ProcessExecutionJpa;

/**
 * JAXB enabled implementation of {@link ProcessExecutionList}.
 */
@XmlRootElement(name = "processList")
public class ProcessExecutionListJpa extends
    AbstractResultList<ProcessExecution> implements ProcessExecutionList {

  /* see superclass */
  @Override
  @XmlElement(type = ProcessExecutionJpa.class, name = "processes")
  public List<ProcessExecution> getObjects() {
    return super.getObjectsTransient();
  }

}
