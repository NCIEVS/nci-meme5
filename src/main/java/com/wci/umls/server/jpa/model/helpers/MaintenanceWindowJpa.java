/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Date;

import com.wci.umls.server.jpa.model.meta.AbstractHasLastModified;
import com.wci.umls.server.model.admin.MaintenanceWindow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * JPA and JAXB enabled implementation of {@link MaintenanceWindow}.
 */
@Entity
@Table(name = "maintenance_windows")
@XmlRootElement(name = "maintenanceWindow")
public class MaintenanceWindowJpa extends AbstractHasLastModified
    implements MaintenanceWindow {

  /** The start date. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDate;

  /** The end date. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDate;

  /**
   * Instantiates an empty {@link MaintenanceWindowJpa}.
   */
  public MaintenanceWindowJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link MaintenanceWindowJpa} from the specified parameters.
   *
   * @param window the maintenance window
   */
  public MaintenanceWindowJpa(MaintenanceWindow window) {
    super(window);
    startDate = window.getStartDate();
    endDate = window.getEndDate();
  }

  /* see superclass */
  @Override
  public Date getStartDate() {
    return startDate;
  }

  /* see superclass */
  @Override
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /* see superclass */
  @Override
  public Date getEndDate() {
    return endDate;
  }

  /* see superclass */
  @Override
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "MaintenanceWindowJpa [id=" + getId() + ", startDate="
        + startDate + ", endDate=" + endDate + "]";
  }
}
