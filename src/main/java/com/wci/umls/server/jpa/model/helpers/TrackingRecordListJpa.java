/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.helpers.AbstractResultList;
import com.wci.umls.server.helpers.TrackingRecordList;
import com.wci.umls.server.jpa.model.workflow.TrackingRecordJpa;
import com.wci.umls.server.model.workflow.TrackingRecord;

/**
 * JAXB enabled implementation of {@link TrackingRecordList}.
 */
@XmlRootElement(name = "worklistList")
public class TrackingRecordListJpa extends AbstractResultList<TrackingRecord>
    implements TrackingRecordList {

  /* see superclass */
  @Override
  @XmlElement(type = TrackingRecordJpa.class, name = "records")
  public List<TrackingRecord> getObjects() {
    return super.getObjectsTransient();
  }

}
