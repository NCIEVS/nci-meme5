/**
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.MaintenanceWindowList;
import com.wci.umls.server.model.admin.MaintenanceWindow;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * JAXB enabled implementation of {@link MaintenanceWindowList}.
 */
@XmlRootElement(name = "maintenanceWindowList")
public class MaintenanceWindowListJpa
    extends AbstractResultList<MaintenanceWindow>
    implements MaintenanceWindowList {

  /* see superclass */
  @Override
  @XmlElement(type = MaintenanceWindowJpa.class, name = "maintenanceWindows")
  public List<MaintenanceWindow> getObjects() {
    return super.getObjectsTransient();
  }
}
