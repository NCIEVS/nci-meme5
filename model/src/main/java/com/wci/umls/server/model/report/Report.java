/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package com.wci.umls.server.model.report;

import java.util.List;

import com.wci.umls.server.Project;
import com.wci.umls.server.helpers.HasLastModified;
import com.wci.umls.server.helpers.QueryType;

/**
 * Generically represents a report.
 */
public interface Report extends HasLastModified {

  /**
   * Gets the auto generated.
   * 
   * @return the auto generated
   */
  public Boolean getAutoGenerated();

  /**
   * Sets the auto generated.
   * 
   * @param autoGenerated the new auto generated
   */
  public void setAutoGenerated(boolean autoGenerated);

  /**
   * Gets the query.
   * 
   * @return the query
   */
  public String getQuery();

  /**
   * Sets the query.
   * 
   * @param query the new query
   */
  public void setQuery(String query);

  /**
   * Gets the query type.
   * 
   * @return the query type
   */
  public QueryType getQueryType();

  /**
   * Sets the query type.
   * 
   * @param queryType the new query type
   */
  public void setQueryType(QueryType queryType);

  /**
   * Gets the results.
   * 
   * @return the results
   */
  public List<ReportResult> getResults();

  /**
   * Sets the results.
   * 
   * @param results the new results
   */
  public void setResults(List<ReportResult> results);

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Sets the name.
   * 
   * @param name the new name
   */
  public void setName(String name);

  /**
   * Gets the result type.
   * 
   * @return the result type
   */
  public String getResultType();

  /**
   * Sets the result type.
   * 
   * @param resultType the new result type
   */
  public void setResultType(String resultType);

  /**
   * Gets the report1 id.
   * 
   * @return the report1 id
   */
  public Long getReport1Id();

  /**
   * Sets the report1 id.
   * 
   * @param report the new report1 id
   */
  public void setReport1Id(Long report);

  /**
   * Gets the report2 id.
   * 
   * @return the report2 id
   */
  public Long getReport2Id();

  /**
   * Sets the report2 id.
   * 
   * @param report the new report2 id
   */
  public void setReport2Id(Long report);

  /**
   * Checks if is diff report.
   *
   * @return true, if is diff report
   */
  public boolean isDiffReport();

  /**
   * Sets the diff report.
   *
   * @param isDiffReport the new diff report
   */
  public void setDiffReport(boolean isDiffReport);

  /**
   * Returns the project.
   *
   * @return the project
   */
  public Project getProject();

  /**
   * Sets the project.
   *
   * @param project the project
   */
  public void setProject(Project project);
}