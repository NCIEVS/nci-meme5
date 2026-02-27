/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.jpa.model.ProjectJpa;

/**
 * JAXB enabled implementation of {@link ProjectList}.
 */
@XmlRootElement(name = "projectList")
public class ProjectListJpa extends AbstractResultList<Project> implements
    ProjectList {

  /* see superclass */
  @Override
  @XmlElement(type = ProjectJpa.class, name = "projects")
  public List<Project> getObjects() {
    return super.getObjectsTransient();
  }

}
