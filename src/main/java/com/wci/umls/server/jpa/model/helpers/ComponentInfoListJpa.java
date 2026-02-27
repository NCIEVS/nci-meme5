/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.helpers.ComponentInfoList;
import com.wci.umls.server.jpa.model.ComponentInfoJpa;

/**
 * JAXB enabled implementation of {@link ComponentInfoList}.
 */
@XmlRootElement(name = "userList")
public class ComponentInfoListJpa extends AbstractResultList<ComponentInfo>
    implements ComponentInfoList {

  /* see superclass */
  @Override
  @XmlElement(type = ComponentInfoJpa.class, name = "userFavorites")
  public List<ComponentInfo> getObjects() {
    return super.getObjectsTransient();
  }

}
