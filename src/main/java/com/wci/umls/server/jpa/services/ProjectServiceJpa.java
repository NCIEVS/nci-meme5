/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.NoResultException;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.MaintenanceWindowList;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.helpers.content.ConceptList;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.UserJpa;
import com.wci.umls.server.jpa.model.helpers.MaintenanceWindowJpa;
import com.wci.umls.server.jpa.model.helpers.MaintenanceWindowListJpa;
import com.wci.umls.server.jpa.model.helpers.ProjectListJpa;
import com.wci.umls.server.model.admin.MaintenanceWindow;
import com.wci.umls.server.services.ProjectService;

/**
 * JPA and JAXB enabled implementation of {@link ProjectService}.
 */
public class ProjectServiceJpa extends RootServiceJpa
    implements ProjectService {

  /** The config properties. */
  protected static Properties config = null;

  /**
   * Instantiates an empty {@link ProjectServiceJpa}.
   *
   * @throws Exception the exception
   */
  public ProjectServiceJpa() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public ConceptList findConceptsInScope(Project project, PfsParameter pfs)
    throws Exception {
    Logger.getLogger(getClass())
        .info("Project Service - get project scope - " + project);

    return null;
  }

  /* see superclass */
  @Override
  public Project getProject(Long id) {
    Logger.getLogger(getClass()).debug("Project Service - get project " + id);
    final Project project = manager.find(ProjectJpa.class, id);
    handleLazyInit(project);
    return project;
  }

  /* see superclass */
  @Override
  @SuppressWarnings("unchecked")
  public ProjectList getProjects() {
    Logger.getLogger(getClass()).debug("Project Service - get projects");
    jakarta.persistence.Query query =
        manager.createQuery("select a from ProjectJpa a");
    try {
      final List<Project> projects = query.getResultList();
      final ProjectList projectList = new ProjectListJpa();
      projectList.setObjects(projects);
      for (final Project project : projectList.getObjects()) {
        handleLazyInit(project);
      }
      return projectList;
    } catch (NoResultException e) {
      return null;
    }
  }

  /* see superclass */
  @Override
  public UserRole getUserRoleForProject(String username, Long projectId)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - get user role for project - " + username
            + ", " + projectId);
    final Project project = getProject(projectId);
    if (project == null) {
      throw new Exception("No project found for " + projectId);
    }

    // check admin
    for (final Map.Entry<User, UserRole> entry : project.getUserRoleMap()
        .entrySet()) {
      if (username.equals(entry.getKey().getName())
          && entry.getValue().equals(UserRole.ADMINISTRATOR)) {
        return UserRole.ADMINISTRATOR;
      }
    }

    // check viewer
    for (final Map.Entry<User, UserRole> entry : project.getUserRoleMap()
        .entrySet()) {
      if (username.equals(entry.getKey().getName())
          && entry.getValue().equals(UserRole.VIEWER)) {
        return UserRole.VIEWER;
      }
    }

    // check reviewer
    for (final Map.Entry<User, UserRole> entry : project.getUserRoleMap()
        .entrySet()) {
      if (username.equals(entry.getKey().getName())
          && entry.getValue().equals(UserRole.REVIEWER)) {
        return UserRole.REVIEWER;
      }
    }

    // check user
    for (final Map.Entry<User, UserRole> entry : project.getUserRoleMap()
        .entrySet()) {
      if (username.equals(entry.getKey().getName())
          && entry.getValue().equals(UserRole.USER)) {
        return UserRole.USER;
      }
    }

    // check author
    for (final Map.Entry<User, UserRole> entry : project.getUserRoleMap()
        .entrySet()) {
      if (username.equals(entry.getKey().getName())
          && entry.getValue().equals(UserRole.AUTHOR)) {
        return UserRole.AUTHOR;
      }
    }

    return null;
  }

  /* see superclass */
  @Override
  public Project addProject(Project project) throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - add project - " + project);
    return addHasLastModified(project);
  }

  /* see superclass */
  @Override
  public void updateProject(Project project) throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - update project - " + project);
    updateHasLastModified(project);
  }

  /* see superclass */
  @Override
  public void removeProject(Long id) throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - remove project " + id);

    try {
      tx = manager.getTransaction();
      final ProjectJpa project = manager.find(ProjectJpa.class, id);

      // Entity not found - nothing to remove, treat as no-op
      if (project == null) {
        return;
      }

      if (isLastModifiedFlag()) {
        if (getLastModifiedBy() == null) {
          throw new Exception(
              "Service cannot remove object, name of modifying user required");
        }
        project.setLastModifiedBy(getLastModifiedBy());
        project.setLastModified(new Date());
      }

      if (getTransactionPerOperation()) {
        tx.begin();
      }

      removeProjectRolesFromUsers(id);
      project.getUserRoleMap().clear();

      if (manager.contains(project)) {
        manager.remove(project);
      } else {
        manager.remove(manager.merge(project));
      }

      if (getTransactionPerOperation()) {
        tx.commit();
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /* see superclass */
  @Override
  public MaintenanceWindow addMaintenanceWindow(MaintenanceWindow window)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - add maintenance window - " + window);
    return addHasLastModified(window);
  }

  /* see superclass */
  @Override
  public void updateMaintenanceWindow(MaintenanceWindow window)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - update maintenance window - " + window);

    if (window == null || window.getId() == null) {
      throw new LocalException("Maintenance window id is required.");
    }

    final MaintenanceWindow existing =
        getHasLastModified(window.getId(), MaintenanceWindowJpa.class);
    if (existing == null) {
      throw new LocalException(
          "Maintenance window does not exist: " + window.getId());
    }

    existing.setStartDate(window.getStartDate());
    existing.setEndDate(window.getEndDate());
    updateHasLastModified(existing);
  }

  /* see superclass */
  @Override
  public void removeMaintenanceWindow(Long windowId) throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - remove maintenance window - " + windowId);
    removeHasLastModified(windowId, MaintenanceWindowJpa.class);
  }

  /* see superclass */
  @Override
  public MaintenanceWindowList getUpcomingMaintenanceWindows(Date now)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - get upcoming maintenance windows");

    final Date effectiveNow = now == null ? new Date() : now;
    final List<MaintenanceWindowJpa> queryResults = manager
        .createQuery(
            "select a from MaintenanceWindowJpa a "
                + "where a.endDate >= :now order by a.startDate",
            MaintenanceWindowJpa.class)
        .setParameter("now", effectiveNow).getResultList();

    final List<MaintenanceWindow> windows = new ArrayList<>();
    windows.addAll(queryResults);

    final MaintenanceWindowList list = new MaintenanceWindowListJpa();
    list.setObjects(windows);
    list.setTotalCount(windows.size());
    return list;
  }

  /* see superclass */
  @Override
  public MaintenanceWindow getNextMaintenanceWindow(Date now) throws Exception {
    Logger.getLogger(getClass())
        .debug("Project Service - get next maintenance window");

    final Date effectiveNow = now == null ? new Date() : now;
    final List<MaintenanceWindowJpa> windows = manager
        .createQuery(
            "select a from MaintenanceWindowJpa a "
                + "where a.endDate >= :now order by a.startDate",
            MaintenanceWindowJpa.class)
        .setParameter("now", effectiveNow).setMaxResults(1).getResultList();

    return windows.isEmpty() ? null : windows.get(0);
  }

  /**
   * Removes references to the specified project from every user's project-role
   * map before the project row is deleted.
   *
   * @param projectId the project id
   */
  private void removeProjectRolesFromUsers(final Long projectId) {
    final List<UserJpa> users = manager
        .createQuery("select u from UserJpa u", UserJpa.class)
        .getResultList();

    for (final UserJpa user : users) {
      final List<Project> projectKeysToRemove = new ArrayList<>();
      for (final Project project : user.getProjectRoleMap().keySet()) {
        if (projectId.equals(project.getId())) {
          projectKeysToRemove.add(project);
        }
      }

      for (final Project project : projectKeysToRemove) {
        user.getProjectRoleMap().remove(project);
      }
    }
  }

  /**
   * Handle lazy initialization.
   *
   * @param project the project
   */
  @Override
  public void handleLazyInit(Project project) {
    if (project == null) {
      return;
    }
    ConfigUtility.initializeLazy(project.getUserRoleMap());
    ConfigUtility.initializeLazy(project.getValidationChecks());
    ConfigUtility.initializeLazy(project.getSemanticTypeCategoryMap());
    if (project.getPrecedenceList() != null) {
      ConfigUtility.initializeLazy(project.getPrecedenceList());
      ConfigUtility.initializeLazy(project.getPrecedenceList().getPrecedence());
      ConfigUtility.initializeLazy(
          project.getPrecedenceList().getPrecedence().getKeyValuePairs());
    }
    ConfigUtility.initializeLazy(project.getValidCategories());
    ConfigUtility.initializeLazy(project.getValidationData());
  }

  /**
   * Returns the pfs comparator.
   *
   * @param <T> the
   * @param clazz the clazz
   * @param pfs the pfs
   * @return the pfs comparator
   * @throws Exception the exception
   */
  @Override
  protected <T> Comparator<T> getPfsComparator(Class<T> clazz, PfsParameter pfs)
    throws Exception {
    if (pfs != null
        && (pfs.getSortField() != null && !pfs.getSortField().isEmpty())) {
      // check that specified sort field exists on Concept and is
      // a string
      final Field sortField = clazz.getField(pfs.getSortField());

      // allow the field to access the Concept values
      sortField.setAccessible(true);

      if (pfs.isAscending()) {
        // make comparator
        return new Comparator<T>() {
          @Override
          public int compare(T o1, T o2) {
            try {
              // handle dates explicitly
              if (o2 instanceof Date) {
                return ((Date) sortField.get(o1))
                    .compareTo((Date) sortField.get(o2));
              } else {
                // otherwise, sort based on conversion to string
                return (sortField.get(o1).toString())
                    .compareTo(sortField.get(o2).toString());
              }
            } catch (IllegalAccessException e) {
              // on exception, return equality
              return 0;
            }
          }
        };
      } else {
        // make comparator
        return new Comparator<T>() {

          @Override
          public int compare(T o2, T o1) {
            try {
              // handle dates explicitly
              if (o2 instanceof Date) {
                return ((Date) sortField.get(o1))
                    .compareTo((Date) sortField.get(o2));
              } else {
                // otherwise, sort based on conversion to string
                return (sortField.get(o1).toString())
                    .compareTo(sortField.get(o2).toString());
              }
            } catch (IllegalAccessException e) {
              // on exception, return equality
              return 0;
            }
          }

        };
      }

    } else {
      return null;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public ProjectList findProjects(String query, PfsParameter pfs)
    throws Exception {
    Logger.getLogger(getClass())
        .info("Project Service - find projects " + "/" + query);

    int[] totalCt = new int[1];
    List<Project> list = (List<Project>) getQueryResults(
        query == null || query.isEmpty() ? "id:[* TO *]" : query,
        ProjectJpa.class, pfs, totalCt);
    final ProjectList result = new ProjectListJpa();
    result.setTotalCount(totalCt[0]);
    result.setObjects(list);
    for (final Project project : result.getObjects()) {
      handleLazyInit(project);
    }
    return result;
  }

}
