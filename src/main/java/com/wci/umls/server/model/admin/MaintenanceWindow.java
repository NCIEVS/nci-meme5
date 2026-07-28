/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.model.admin;

import java.util.Date;

import com.wci.umls.server.helpers.HasLastModified;

/**
 * Represents a planned maintenance window.
 */
public interface MaintenanceWindow extends HasLastModified {

  /**
   * Returns the start date.
   *
   * @return the start date
   */
  public Date getStartDate();

  /**
   * Sets the start date.
   *
   * @param startDate the start date
   */
  public void setStartDate(Date startDate);

  /**
   * Returns the end date.
   *
   * @return the end date
   */
  public Date getEndDate();

  /**
   * Sets the end date.
   *
   * @param endDate the end date
   */
  public void setEndDate(Date endDate);
}
