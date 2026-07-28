/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.services;

import java.util.Date;

import com.wci.umls.server.helpers.MaintenanceWindowList;
import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.helpers.content.ConceptList;
import com.wci.umls.server.model.admin.MaintenanceWindow;

/**
 * Represents a service for accessing {@link Project} information.
 */
public interface ProjectService extends RootService {

  /**
   * Returns the concepts in scope.
   *
   * @param project the project
   * @param pfs the pfs
   * @return the concepts in scope
   * @throws Exception the exception
   */
  public ConceptList findConceptsInScope(Project project, PfsParameter pfs)
    throws Exception;

  /**
   * Returns the project.
   *
   * @param id the id
   * @return the project
   */
  public Project getProject(Long id);

  /**
   * Adds the project.
   *
   * @param project the project
   * @return the project
   * @throws Exception the exception
   */
  public Project addProject(Project project) throws Exception;

  /**
   * Update project.
   *
   * @param project the project
   * @throws Exception the exception
   */
  public void updateProject(Project project) throws Exception;

  /**
   * Removes the project.
   *
   * @param projectId the project id
   * @throws Exception the exception
   */
  public void removeProject(Long projectId) throws Exception;

  /**
   * Adds the maintenance window.
   *
   * @param window the maintenance window
   * @return the maintenance window
   * @throws Exception the exception
   */
  public MaintenanceWindow addMaintenanceWindow(MaintenanceWindow window)
    throws Exception;

  /**
   * Updates the maintenance window.
   *
   * @param window the maintenance window
   * @throws Exception the exception
   */
  public void updateMaintenanceWindow(MaintenanceWindow window)
    throws Exception;

  /**
   * Removes the maintenance window.
   *
   * @param windowId the maintenance window id
   * @throws Exception the exception
   */
  public void removeMaintenanceWindow(Long windowId) throws Exception;

  /**
   * Returns upcoming maintenance windows.
   *
   * @param now the current date
   * @return the maintenance window list
   * @throws Exception the exception
   */
  public MaintenanceWindowList getUpcomingMaintenanceWindows(Date now)
    throws Exception;

  /**
   * Returns the next maintenance window.
   *
   * @param now the current date
   * @return the next maintenance window
   * @throws Exception the exception
   */
  public MaintenanceWindow getNextMaintenanceWindow(Date now) throws Exception;

  /**
   * Returns the projects.
   *
   * @return the projects
   */
  public ProjectList getProjects();

  /**
   * Returns the user role for project.
   *
   * @param username the username
   * @param projectId the project id
   * @return the user role for project
   * @throws Exception the exception
   */
  public UserRole getUserRoleForProject(String username, Long projectId)
    throws Exception;

  /**
   * Find projects for query.
   *
   * @param query the query
   * @param pfs the pfs
   * @return the project list
   * @throws Exception the exception
   */
  public ProjectList findProjects(String query, PfsParameter pfs)
    throws Exception;

  /**
   * Handle lazy init.
   *
   * @param project the project
   */
  public void handleLazyInit(Project project);

}
