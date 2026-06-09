/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;

import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.UserJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;
import com.wci.umls.server.test.helpers.IntegrationUnitSupport;

/**
 * Integration testing for project deletion behavior.
 */
public class ProjectDeletionJpaIT extends IntegrationUnitSupport {

  /**
   * Test removing a project that is still referenced by a user's project-role
   * map.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRemoveProjectClearsAssignedUserProjectRoles()
    throws Exception {
    final ProjectService projectService = new ProjectServiceJpa();
    final SecurityService securityService = new SecurityServiceJpa();
    Project addedProject = null;

    try {
      projectService.setLastModifiedBy("admin");
      securityService.setLastModifiedBy("admin");

      Project project = new ProjectJpa();
      project.setName(uniqueTestName("Assigned project delete"));
      project.setDescription("Assigned project delete");
      project.setLastModifiedBy("admin");
      project.setLastModified(new Date());
      project.setTimestamp(new Date());
      project.setTerminology("MTH");
      project.setVersion("latest");
      project.setLanguage("ENG");
      project.setWorkflowPath("DEFAULT");

      addedProject = projectService.addProject(project);
      final Long projectId = addedProject.getId();

      User admin = securityService.getUser("admin");
      addedProject.getUserRoleMap().put(new UserJpa(admin),
          UserRole.ADMINISTRATOR);
      projectService.updateProject(addedProject);

      admin = securityService.getUser("admin");
      admin.getProjectRoleMap().put(new ProjectJpa(addedProject),
          UserRole.ADMINISTRATOR);
      securityService.updateUser(admin);

      projectService.removeProject(projectId);

      assertNull(projectService.getProject(projectId));

      final SecurityService validationService = new SecurityServiceJpa();
      try {
        final User updatedAdmin = validationService.getUser("admin");
        assertFalse(updatedAdmin.getProjectRoleMap().keySet().stream()
            .anyMatch(userProject -> projectId.equals(userProject.getId())));
      } finally {
        validationService.close();
      }
      addedProject = null;
    } finally {
      if (addedProject != null && addedProject.getId() != null
          && projectService.getProject(addedProject.getId()) != null) {
        projectService.removeProject(addedProject.getId());
      }
      projectService.close();
      securityService.close();
    }
  }
}
