/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.UserJpa;
import com.wci.umls.server.jpa.model.actions.AtomicActionListJpa;
import com.wci.umls.server.jpa.model.actions.MolecularActionListJpa;
import com.wci.umls.server.jpa.model.helpers.MaintenanceWindowJpa;
import com.wci.umls.server.jpa.model.helpers.MaintenanceWindowListJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.model.helpers.PrecedenceListJpa;
import com.wci.umls.server.jpa.model.helpers.ProjectListJpa;
import com.wci.umls.server.jpa.model.helpers.TypeKeyValueJpa;
import com.wci.umls.server.jpa.model.helpers.TypeKeyValueListJpa;
import com.wci.umls.server.jpa.model.helpers.UserListJpa;
import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParserBase;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.LogEntry;
import com.wci.umls.server.helpers.MaintenanceWindowList;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.ProjectList;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.helpers.StringList;
import com.wci.umls.server.helpers.TypeKeyValue;
import com.wci.umls.server.helpers.TypeKeyValueList;
import com.wci.umls.server.helpers.UserList;
import com.wci.umls.server.jpa.algo.maint.ReloadConfigPropertiesAlgorithm;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.ProjectServiceRest;
import com.wci.umls.server.model.actions.AtomicActionList;
import com.wci.umls.server.model.actions.MolecularActionList;
import com.wci.umls.server.model.admin.MaintenanceWindow;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST implementation for {@link ProjectServiceRest}..
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/project")
@Path("/project")
@Tag(name = "Project", description = "Project metadata, roles, validation checks, and project-user assignments.")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ProjectServiceRestImpl extends RootServiceRestImpl
    implements ProjectServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link ProjectServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ProjectServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }
  /* see superclass */
  @Override
  @RequestMapping(value = "/", method = RequestMethod.PUT)
  @PUT
  // @Path("/")
  @Operation(summary = "Add project",
      description = "Creates a new project. Requires application USER access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project created",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ProjectJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or project could not be created",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public Project addProject(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Project, e.g. newProject", required = true) @RequestBody ProjectJpa project,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): / " + project);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "add project", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      // check to see if project already exists
      for (final Project p : metadataService.getProjects().getObjects()) {
        if (p.getName().equals(project.getName())
            && p.getDescription().equals(project.getDescription())) {
          throw new LocalException(
              "A project with this name and description already exists");
        }
      }

      // Create and add precedence list
      if (project.getTerminology() == null || project.getVersion() == null) {
        throw new LocalException(
            "Project terminology and version must not be null.");
      }

      // Create a new precedence list if one isn't specified
      if (project.getPrecedenceListId() == null) {
        final PrecedenceList precList = metadataService
            .getPrecedenceList(project.getTerminology(), project.getVersion());
        if (precList != null) {
          PrecedenceList newPrecList = new PrecedenceListJpa(precList);
          newPrecList.setId(null);
          newPrecList = metadataService.addPrecedenceList(newPrecList);
          project.setPrecedenceList(newPrecList);
        }
      } else {
        final PrecedenceList precList =
            metadataService.getPrecedenceList(project.getPrecedenceListId());
        if (precList == null) {
          throw new Exception(
              "Unexpected nonexistent precedence list id specified = "
                  + project.getPrecedenceListId());
        }
        // here, do nothing, the id is properly set.
      }

      // Add project
      final Project newProject = metadataService.addProject(project);
      metadataService.addLogEntry(userName, project.getId(), project.getId(),
          null, null, "ADD project - " + project);

      return newProject;
    } catch (Exception e) {
      handleException(e, "trying to add a project");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/", method = RequestMethod.POST)
  @POST
  // @Path("/")
  @Operation(summary = "Update project",
      description = "Updates an existing project. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project updated",
              content = @Content(mediaType = MediaType.APPLICATION_JSON)),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or project could not be updated",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void updateProject(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Project, e.g. existingProject", required = true) @RequestBody ProjectJpa project,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): / " + project);

    // Create service and configure transaction scope
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String userName = authorizeProject(projectService, project.getId(),
          securityService, authToken, "update project", UserRole.AUTHOR);
      projectService.setLastModifiedBy(userName);
      // check to see if project already exists
      final Project origProject = projectService.getProject(project.getId());
      if (origProject == null) {
        throw new Exception("Project " + project.getId() + " does not exist");
      }

      // compare old and new typeKeyValue lists
      final List<TypeKeyValue> oldValidationData =
          origProject.getValidationData();
      final List<TypeKeyValue> newValidationData = project.getValidationData();

      // Find validation data to remove
      final List<TypeKeyValue> validationDataToRemove = new ArrayList<>();
      for (final TypeKeyValue tkv : oldValidationData) {
        boolean found = false;
        for (final TypeKeyValue tkv2 : newValidationData) {
          if (tkv2.getId() != null && tkv.equals(tkv2)) {
            found = true;
            break;
          }
        }
        if (!found) {
          validationDataToRemove.add(tkv);
        }
      }

      // Add new validation data
      for (final TypeKeyValue tkv : newValidationData) {
        if (tkv.getId() == null) {
          projectService.addTypeKeyValue(tkv);
          // VERIFY THAT tkv.getId() is not null at this point
          if (tkv.getId() == null) {
            throw new Exception("tkv.getId() should not be null " + tkv);
          }
        }
      }

      // Update project
      project.setUserRoleMap(origProject.getUserRoleMap());
      project.setPrecedenceList(origProject.getPrecedenceList());
      projectService.updateProject(project);

      // Remove old validation data
      for (final TypeKeyValue tkv : validationDataToRemove) {
        projectService.removeTypeKeyValue(tkv.getId());
      }

      projectService.addLogEntry(userName, project.getId(), project.getId(),
          null, null, "UPDATE project " + project);

    } catch (Exception e) {
      handleException(e, "trying to update a project");
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/{id}")
  @Operation(summary = "Remove project",
      description = "Removes the project with the specified id. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project removed",
              content = @Content(mediaType = MediaType.APPLICATION_JSON)),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or project could not be removed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void removeProject(
    @Parameter(
        description = "Project id", example = "39751",
        required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /" + id);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String userName = authorizeProject(projectService, id,
          securityService, authToken, "remove project", UserRole.AUTHOR);

      projectService.setLastModifiedBy(userName);
      // Create service and configure transaction scope
      projectService.removeProject(id);

      projectService.addLogEntry(userName, id, id, null, null,
          "REMOVE project " + id);

    } catch (Exception e) {
      handleException(e, "trying to remove a project");
    } finally {
      projectService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @GET
  @Path("/{id}")
  @Operation(summary = "Get project",
      description = "Returns the project for the specified id.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ProjectJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or project could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public Project getProject(
    @Parameter(
        description = "Project id", example = "39751",
        required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /" + id);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get the project",
          UserRole.VIEWER);
      final Project project = projectService.getProject(id);
      projectService.handleLazyInit(project);
      return project;
    } catch (Exception e) {
      handleException(e, "trying to get a project");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/assign", method = RequestMethod.GET)
  @GET
  @Path("/assign")
  @Operation(summary = "Assign user to project",
      description = "Assigns a project role to a user. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project assignment updated",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ProjectJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or assignment could not be updated",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public Project assignUserToProject(
    @Parameter(
        description = "Project id", example = "39751") @RequestParam(
            value = "projectId", required = false) Long projectId,
    @Parameter(
        description = "User name", example = "DSS") @RequestParam(
            value = "userName", required = false) String userName,
    @Parameter(
        description = "Project role to assign", example = "AUTHOR") @RequestParam(
            value = "role", required = false) UserRole role,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /assign "
        + projectId + ", " + userName + ", " + role);

    // Test preconditions
    if (projectId == null || userName == null || role == null) {
      handleException(new LocalException("Required parameter has a null value"),
          "");
    }

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String authUser =
          authorizeProject(projectService, projectId, securityService,
              authToken, "assign user to project", UserRole.AUTHOR);
      projectService.setLastModifiedBy(authUser);

      final User user = securityService.getUser(userName);
      final User userCopy = new UserJpa(user);
      final Project project = projectService.getProject(projectId);
      final Project projectCopy = new ProjectJpa(project);
      project.getUserRoleMap().put(userCopy, role);
      projectService.updateProject(project);

      user.getProjectRoleMap().put(projectCopy, role);
      securityService.updateUser(user);

      projectService.addLogEntry(authUser, projectId, projectId, null, null,
          "ASSIGN user to project - " + userName);

      return project;

    } catch (Exception e) {
      handleException(e, "trying to add user to project");
    } finally {
      projectService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/{projectId}/users", method = RequestMethod.POST)
  @POST
  @Path("/{projectId}/users")
  @Operation(summary = "Find users assigned to a project",
      description = "Returns users with assigned roles on the specified project. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Assigned users returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = UserListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public UserList findAssignedUsersForProject(
    @Parameter(
        description = "Project id", example = "39751",
        required = true) @PathVariable("projectId") Long projectId,
    @Parameter(
        description = "Optional Lucene query to filter assigned users",
        example = "userName:DSS") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters for assigned users.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /" + projectId
        + "/users, " + query + ", " + pfs);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "find users assigned to project", UserRole.AUTHOR);

      // return all users assigned to the project
      if (pfs.getQueryRestriction() == null
          || pfs.getQueryRestriction().isEmpty()) {
        pfs.setQueryRestriction("projectAnyRole:" + projectId);
      } else {
        pfs.setQueryRestriction(
            pfs.getQueryRestriction() + " AND projectAnyRole:" + projectId);

      }
      final UserList list = securityService.findUsers(query, pfs);
      // lazy initialize with blank user prefs
      for (final User user : list.getObjects()) {
        user.setUserPreferences(null);
      }
      return list;
    } catch (Exception e) {
      handleException(e, "find users for project");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/roles", method = RequestMethod.GET)
  @GET
  @Path("/roles")
  @Operation(summary = "Get project roles",
      description = "Returns the valid project role values.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project roles returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = StringList.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public StringList getProjectRoles(
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /roles");

    try {
      authorizeApp(securityService, authToken, "get roles", UserRole.VIEWER);
      final StringList list = new StringList();
      list.setTotalCount(3);
      list.getObjects().add(UserRole.AUTHOR.toString());
      list.getObjects().add(UserRole.REVIEWER.toString());
      list.getObjects().add(UserRole.ADMINISTRATOR.toString());
      return list;
    } catch (Exception e) {
      handleException(e, "trying to get roles");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/queryTypes", method = RequestMethod.GET)
  @GET
  @Path("/queryTypes")
  @Operation(summary = "Get query types",
      description = "Returns the valid query type values.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Query types returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = StringList.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public StringList getQueryTypes(
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /queryTypes");

    try {
      authorizeApp(securityService, authToken, "get query types",
          UserRole.VIEWER);
      final StringList list = new StringList();
      list.setTotalCount(3);
      list.getObjects().add(QueryType.JPQL.toString());
      list.getObjects().add(QueryType.SQL.toString());
      list.getObjects().add(QueryType.LUCENE.toString());
      list.getObjects().add(QueryType.PROGRAM.toString());
      return list;
    } catch (Exception e) {
      handleException(e, "trying to get query types");
      return null;
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/{projectId}/users/unassigned", method = RequestMethod.POST)
  @POST
  @Path("/{projectId}/users/unassigned")
  @Operation(summary = "Find users not assigned to a project",
      description = "Returns users who do not yet have assigned roles on the specified project. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Unassigned users returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = UserListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public UserList findUnassignedUsersForProject(
    @Parameter(
        description = "Project id", example = "39751",
        required = true) @PathVariable("projectId") Long projectId,
    @Parameter(
        description = "Optional Lucene query to filter candidate users",
        example = "userName:DSS") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters for unassigned users.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /users/ "
        + projectId + "/unassigned, " + query + ", " + pfs);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "find candidate users for project", UserRole.AUTHOR);
      // return all users assigned to the project
      if (pfs.getQueryRestriction() != null
          && !pfs.getQueryRestriction().isEmpty()) {
        pfs.setQueryRestriction(
            pfs.getQueryRestriction() + " AND NOT projectAnyRole:" + projectId);
      } else {
        pfs.setQueryRestriction("NOT projectAnyRole:" + projectId);
      }
      final UserList list = securityService.findUsers(query, pfs);
      // lazy initialize with blank user prefs
      for (final User user : list.getObjects()) {
        user.setUserPreferences(null);
      }

      return list;
    } catch (Exception e) {
      handleException(e, "find users for project");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/user/anyrole", method = RequestMethod.GET)
  @GET
  @Produces("text/plain")
  @Path("/user/anyrole")
  @Operation(summary = "Check whether current user has any project role",
      description = "Returns true if the authorized user has any project role on any project.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Role check returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = Boolean.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or role check could not be completed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public Boolean userHasSomeProjectRole(
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /user/anyrole");
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String user = authorizeApp(securityService, authToken,
          "check for any project role", UserRole.VIEWER);

      final StringBuilder sb = new StringBuilder();
      sb.append("(");
      sb.append("userRoleMap:" + user + UserRole.ADMINISTRATOR).append(" OR ");
      sb.append("userRoleMap:" + user + UserRole.REVIEWER).append(" OR ");
      sb.append("userRoleMap:" + user + UserRole.AUTHOR).append(")");
      final ProjectList list =
          projectService.findProjects(sb.toString(), new PfsParameterJpa());
      return list.getTotalCount() != 0;

    } catch (Exception e) {
      handleException(e, "trying to check for any project role");
    } finally {
      projectService.close();
      securityService.close();
    }
    return false;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/unassign", method = RequestMethod.GET)
  @GET
  @Path("/unassign")
  @Operation(summary = "Unassign user from project",
      description = "Removes a user's assignment from the specified project. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Project assignment updated",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ProjectJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or assignment could not be updated",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public Project unassignUserFromProject(
    @Parameter(
        description = "Project id", example = "39751") @RequestParam(
            value = "projectId", required = false) Long projectId,
    @Parameter(
        description = "User name", example = "DSS") @RequestParam(
            value = "userName", required = false) String userName,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Project): /unassign " + projectId + ", " + userName);

    // Test preconditions
    if (projectId == null || userName == null) {
      handleException(new Exception("Required parameter has a null value"), "");
    }

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      // Check if user is either an ADMIN overall or an AUTHOR on this project

      String authUser = null;
      try {
        authUser = authorizeProject(projectService, projectId, securityService,
            authToken, "unassign user from project", UserRole.AUTHOR);
      } catch (Exception e) {
        // now try to validate project role
        authUser = authorizeProject(projectService, projectId, securityService,
            authToken, "unassign user from project", UserRole.AUTHOR);
      }
      projectService.setLastModifiedBy(authUser);

      User user = securityService.getUser(userName);
      final User userCopy = new UserJpa(user);
      Project project = projectService.getProject(projectId);
      final Project projectCopy = new ProjectJpa(project);

      project.getUserRoleMap().remove(userCopy);
      projectService.updateProject(project);

      // reread to show
      project = projectService.getProject(projectId);

      user.getProjectRoleMap().remove(projectCopy);
      securityService.updateUser(user);

      user = securityService.getUser(userName);

      projectService.addLogEntry(authUser, projectId, projectId, null, null,
          "UNASSIGN user from project - " + userName);

      return project;
    } catch (Exception e) {
      handleException(e, "trying to remove user from project");
    } finally {
      projectService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/find", method = RequestMethod.POST)
  @POST
  @Path("/find")
  @Operation(summary = "Find projects",
      description = "Returns projects matching an optional Lucene query. Leave query empty to return all projects.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Projects returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ProjectListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public ProjectList findProjects(
    @Parameter(
        description = "Optional Lucene query. Leave blank to use id:[* TO *].",
        example = "id:[* TO *]") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters. Do not put this JSON in the query parameter.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Project): /find, " + pfs);
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "find projects",
          UserRole.VIEWER);

      return projectService.findProjects(query, pfs);
    } catch (Exception e) {
      handleException(e, "trying to get projects ");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @RequestMapping(value = "/log", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Path("/log")
  @Produces("text/plain")
  @Operation(summary = "Get project log entries",
      description = "Returns log entries for the specified project, optional object id, and optional message filter. Requires project AUTHOR access or better.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Log entries returned",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string"))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or log entries could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  @Override
  public String getLog(
    @Parameter(
        description = "Project id", example = "39751") @RequestParam(
            value = "projectId", required = false) Long projectId,
    @Parameter(
        description = "Optional object id", example = "39751") @RequestParam(
            value = "objectId", required = false) Long objectId,
    @Parameter(
        description = "Optional exact message filter",
        example = "ADD project") @RequestParam(value = "message",
            required = false) String message,
    @Parameter(
        description = "Maximum number of log lines", example = "25") @RequestParam(
            value = "lines", required = false, defaultValue = "0") int lines,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /log/"
        + projectId + ", " + objectId + ", " + message + ", " + lines);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeProject(projectService, projectId, securityService, authToken,
          "get log entries", UserRole.AUTHOR);

      // Precondition checking -- must have projectId and objectId set
      if (projectId == null) {
        throw new LocalException("Project id and Object id must be set");
      }

      final PfsParameter pfs = new PfsParameterJpa();
      pfs.setStartIndex(0);
      pfs.setMaxResults(lines);
      pfs.setAscending(false);
      pfs.setSortField("lastModified");

      String query = "";

      // projectId and objectId must be set
      query += "projectId:" + projectId;
      if (objectId != null) {
        query += " AND objectId:" + objectId;
      }
      if (message != null) {
        query += " AND message:\"" + QueryParserBase.escape(message) + "\"";
      }

      if (query.isEmpty()) {
        throw new Exception(
            "Must specify at least one parameter for querying log entries");
      }

      final List<LogEntry> entries = projectService.findLogEntries(query, pfs);

      final StringBuilder log = new StringBuilder();
      for (int i = entries.size() - 1; i >= 0; i--) {
        final LogEntry entry = entries.get(i);
        final StringBuilder msg = new StringBuilder();
        msg.append("[")
            .append(ConfigUtility.formatDisplayTimestamp(
                entry.getLastModified()));
        msg.append("] ");
        msg.append(entry.getLastModifiedBy()).append(" ");
        msg.append(entry.getMessage()).append("\n");
        log.append(msg);
      }

      return log.toString();

    } catch (Exception e) {
      handleException(e, "trying to get log");
    } finally {
      projectService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @RequestMapping(value = "/log/{activity}", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Path("/log/{activity}")
  @Produces("text/plain")
  @Operation(summary = "Get activity log entries",
      description = "Returns log entries for the specified terminology, version, and activity.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Log entries returned",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string"))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or log entries could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  @Override
  public String getLog(
    @Parameter(
        description = "Terminology", example = "NCI") @RequestParam(
            value = "terminology", required = false) String terminology,
    @Parameter(
        description = "Terminology version", example = "latest") @RequestParam(
            value = "version", required = false) String version,
    @Parameter(
        description = "Activity", example = "EDITING",
        required = true) @PathVariable("activity") String activity,
    @Parameter(
        description = "Maximum number of log lines", example = "25") @RequestParam(
            value = "lines", required = false, defaultValue = "0") int lines,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Terminology): /log/"
        + terminology + ", " + version + ", " + activity + ", " + lines);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get log entries",
          UserRole.VIEWER);

      // Precondition checking -- must have terminology version AND activity set
      if (terminology == null || version == null || activity == null) {
        throw new LocalException(
            "Terminology/version and activity must be set");
      }

      final PfsParameter pfs = new PfsParameterJpa();
      pfs.setStartIndex(0);
      pfs.setMaxResults(lines);
      pfs.setAscending(false);
      pfs.setSortField("lastModified");

      String query = "terminology:" + terminology + " AND version:" + version
          + " AND activity:" + activity;

      if (query.isEmpty()) {
        throw new Exception(
            "Must specify at least one parameter for querying log entries");
      }

      final List<LogEntry> entries = projectService.findLogEntries(query, pfs);

      final StringBuilder log = new StringBuilder();
      for (int i = entries.size() - 1; i >= 0; i--) {
        final LogEntry entry = entries.get(i);
        final StringBuilder message = new StringBuilder();
        message.append("[")
            .append(ConfigUtility.formatDisplayTimestamp(
                entry.getLastModified()));
        message.append("] ");
        message.append(entry.getLastModifiedBy()).append(" ");
        message.append(entry.getMessage()).append("\n");
        log.append(message);
      }

      return log.toString();

    } catch (Exception e) {
      handleException(e, "trying to get log");
    } finally {
      projectService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/actions/molecular", method = RequestMethod.POST)
  @POST
  @Path("/actions/molecular")
  @Operation(summary = "Find molecular actions",
      description = "Returns molecular actions matching the optional component, terminology, version, and query filters.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Molecular actions returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MolecularActionListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public MolecularActionList findMolecularActions(
    @Parameter(
        description = "Optional component id", example = "1") @RequestParam(
            value = "componentId", required = false) Long componentId,
    @Parameter(
        description = "Optional terminology", example = "NCI") @RequestParam(
            value = "terminology", required = false) String terminology,
    @Parameter(
        description = "Optional terminology version",
        example = "latest") @RequestParam(value = "version",
            required = false) String version,
    @Parameter(
        description = "Optional Lucene query") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters for molecular actions.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Content): /actions/molecular " + query);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken,
          "find molecular actions for a concept", UserRole.VIEWER);
      return projectService.findMolecularActions(componentId, terminology,
          version, query, pfs);

    } catch (Exception e) {
      handleException(e, "trying to find molecular actions for a concept");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/actions/atomic", method = RequestMethod.POST)
  @POST
  @Path("/actions/atomic")
  @Operation(summary = "Find atomic actions",
      description = "Returns atomic actions for a molecular action and optional query filter.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Atomic actions returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = AtomicActionListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public AtomicActionList findAtomicActions(
    @Parameter(
        description = "Molecular action id", example = "1") @RequestParam(
            value = "molecularActionId", required = false) Long molecularActionId,
    @Parameter(
        description = "Optional Lucene query") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters for atomic actions.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Content): /actions/atomic "
        + molecularActionId + ", " + query);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken,
          "find atomic actions for a molecular action", UserRole.VIEWER);

      return projectService.findAtomicActions(molecularActionId, query, pfs);

    } catch (Exception e) {
      handleException(e,
          "trying to find atomic actions for a molecular action");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checks", method = RequestMethod.GET)
  @GET
  @Path("/checks")
  @Operation(summary = "Get validation checks",
      description = "Returns configured validation check names.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Validation checks returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = KeyValuePairList.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public KeyValuePairList getValidationChecks(
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /checks ");

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get validation checks",
          UserRole.VIEWER);

      final KeyValuePairList list = projectService.getValidationCheckNames();
      return list;
    } catch (Exception e) {
      handleException(e, "trying to validate all concept");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/reload", method = RequestMethod.POST)
  @POST
  @Path("/reload")
  @Operation(summary = "Reload configuration properties",
      description = "Reloads configuration properties and clears caches. Requires application ADMINISTRATOR access.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Configuration properties reloaded"),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or reload could not be completed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void reloadConfigProperties(
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /reload ");

    final ReloadConfigPropertiesAlgorithm algo =
        new ReloadConfigPropertiesAlgorithm();
    try {
      authorizeApp(securityService, authToken, "reload config properties",
          UserRole.ADMINISTRATOR);
      algo.compute();
    } catch (Exception e) {
      handleException(e, "trying to reload config properties");
    } finally {
      algo.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/exception", method = RequestMethod.POST)
  @POST
  @Path("/exception")
  @Operation(summary = "Force an exception",
      description = "Forces an exception to test exception handling. Requires application ADMINISTRATOR access.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Exception handler completed"),
          @ApiResponse(responseCode = "500",
              description = "Forced exception or authorization failure",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void forceException(
    @Parameter(
        description = "When true, force a LocalException instead of a generic Exception.",
        example = "true") @RequestParam(value = "local",
            required = false) Boolean localFlag,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /reload ");
    try {
      authorizeApp(securityService, authToken, "force exception",
          UserRole.ADMINISTRATOR);

      if (localFlag != null && localFlag) {
        throw new LocalException("TEST LOCAL EXCEPTION");
      } else {
        throw new Exception("TEST EXCEPTION");
      }
    } catch (Exception e) {
      handleException(e, "trying to force exception");
    } finally {
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/maintenance", method = RequestMethod.PUT)
  @PUT
  @Path("/maintenance")
  @Operation(summary = "Add maintenance window",
      description = "Adds a planned maintenance window. Requires a valid application user.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Maintenance window created",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MaintenanceWindowJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or maintenance window could not be created",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public MaintenanceWindow addMaintenanceWindow(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Maintenance window", required = true) @RequestBody MaintenanceWindowJpa maintenanceWindow,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Project): /maintenance " + maintenanceWindow);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String userName = authorizeMaintenanceWindowMutation(authToken,
          "add maintenance windows");
      projectService.setLastModifiedBy(userName);
      validateMaintenanceWindow(maintenanceWindow);

      maintenanceWindow.setId(null);
      final MaintenanceWindow newWindow =
          projectService.addMaintenanceWindow(maintenanceWindow);
      projectService.addLogEntry(userName, null, newWindow.getId(), null, null,
          "ADD maintenance window - " + newWindow);
      return newWindow;
    } catch (Exception e) {
      handleException(e, "trying to add a maintenance window");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/maintenance", method = RequestMethod.POST)
  @POST
  @Path("/maintenance")
  @Operation(summary = "Update maintenance window",
      description = "Updates a planned maintenance window. Requires a valid application user.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Maintenance window updated"),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or maintenance window could not be updated",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void updateMaintenanceWindow(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Maintenance window", required = true) @RequestBody MaintenanceWindowJpa maintenanceWindow,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Project): /maintenance update " + maintenanceWindow);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String userName = authorizeMaintenanceWindowMutation(authToken,
          "update maintenance windows");
      projectService.setLastModifiedBy(userName);
      if (maintenanceWindow == null || maintenanceWindow.getId() == null) {
        throw new LocalException("Maintenance window id is required.");
      }
      validateMaintenanceWindow(maintenanceWindow);

      projectService.updateMaintenanceWindow(maintenanceWindow);
      projectService.addLogEntry(userName, null, maintenanceWindow.getId(),
          null, null, "UPDATE maintenance window - " + maintenanceWindow);
    } catch (Exception e) {
      handleException(e, "trying to update a maintenance window");
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/maintenance/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/maintenance/{id}")
  @Operation(summary = "Remove maintenance window",
      description = "Removes a planned maintenance window by id. Requires a valid application user.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Maintenance window removed"),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or maintenance window could not be removed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public void removeMaintenanceWindow(
    @Parameter(description = "Maintenance window id", example = "1",
        required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project): /maintenance remove " + id);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String userName = authorizeMaintenanceWindowMutation(authToken,
          "remove maintenance windows");
      projectService.setLastModifiedBy(userName);
      projectService.removeMaintenanceWindow(id);
      projectService.addLogEntry(userName, null, id, null, null,
          "REMOVE maintenance window - " + id);
    } catch (Exception e) {
      handleException(e, "trying to remove a maintenance window");
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/maintenance", method = RequestMethod.GET)
  @GET
  @Path("/maintenance")
  @Operation(summary = "Get upcoming maintenance windows",
      description = "Returns maintenance windows that have not yet passed.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Maintenance windows returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MaintenanceWindowListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or maintenance windows could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public MaintenanceWindowList getUpcomingMaintenanceWindows(
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /maintenance");

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get maintenance windows",
          UserRole.VIEWER);
      return projectService.getUpcomingMaintenanceWindows(new Date());
    } catch (Exception e) {
      handleException(e, "trying to get maintenance windows");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/maintenance/next", method = RequestMethod.GET)
  @GET
  @Path("/maintenance/next")
  @Operation(summary = "Get next maintenance window",
      description = "Returns the next maintenance window that has not yet passed.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Maintenance window returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = MaintenanceWindowJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or maintenance window could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public MaintenanceWindow getNextMaintenanceWindow(
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project): /maintenance/next");

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get next maintenance window",
          UserRole.VIEWER);
      return projectService.getNextMaintenanceWindow(new Date());
    } catch (Exception e) {
      handleException(e, "trying to get the next maintenance window");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  /**
   * Authorizes a maintenance window mutation.
   *
   * @param authToken the auth token
   * @param action the action
   * @return the user name
   * @throws Exception the exception
   */
  private String authorizeMaintenanceWindowMutation(String authToken,
    String action) throws Exception {
    final String userName =
        authorizeApp(securityService, authToken, action, UserRole.VIEWER);
    if ("guest".equals(userName)) {
      throw new WebApplicationException(Response.status(401).entity(
          "Guest users do not have permissions to " + action + ".").build());
    }
    return userName;
  }

  /**
   * Validates maintenance window fields.
   *
   * @param maintenanceWindow the maintenance window
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private void validateMaintenanceWindow(MaintenanceWindow maintenanceWindow)
    throws Exception {
    if (maintenanceWindow == null || maintenanceWindow.getStartDate() == null
        || maintenanceWindow.getEndDate() == null) {
      throw new LocalException(
          "Maintenance window start and end dates are required.");
    }

    if (!maintenanceWindow.getStartDate()
        .before(maintenanceWindow.getEndDate())) {
      throw new LocalException(
          "Maintenance window end date must be after the start date.");
    }

    if (maintenanceWindow.getEndDate().before(new Date())) {
      throw new LocalException(
          "Maintenance window end date must not be in the past.");
    }
  }

  @Override
  @Path("/typeKeyValue/add")
  @RequestMapping(value = "/typeKeyValue/add", method = RequestMethod.PUT)
  @PUT
  @Operation(summary = "Add type key value",
      description = "Adds a type-key-value object.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Type-key-value object created",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = TypeKeyValueJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or type-key-value object could not be created",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public TypeKeyValue addTypeKeyValue(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The type key value to add") @RequestBody TypeKeyValueJpa typeKeyValue,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project, PUT): / " + typeKeyValue);
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String username = authorizeApp(securityService, authToken,
          "add type key value", UserRole.VIEWER);
      projectService.setLastModifiedBy(username);
      return projectService.addTypeKeyValue(typeKeyValue);
    } catch (Exception e) {
      handleException(e, "trying to add type key value ");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }

  @Override
  @Path("/typeKeyValue/{id}")
  @RequestMapping(value = "/typeKeyValue/{id}", method = RequestMethod.GET)
  @GET
  @Operation(summary = "Get type key value",
      description = "Returns a type-key-value object by id.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Type-key-value object returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = TypeKeyValueJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or type-key-value object could not be retrieved",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public TypeKeyValue getTypeKeyValue(
    @Parameter(
        description = "Type-key-value id", example = "1",
        required = true) @PathVariable("id") Long id,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    {
      Logger.getLogger(getClass()).info("RESTful call (Project, Get): / " + id);
      final ProjectService projectService = new ProjectServiceJpa();
      try {
        authorizeApp(securityService, authToken, "get type key value",
            UserRole.VIEWER);
        return projectService.getTypeKeyValue(id);
      } catch (Exception e) {
        handleException(e, "trying to get type key value ");
        return null;
      } finally {
        projectService.close();
        securityService.close();
      }
    }
  }

  @Override
  @Path("/typeKeyValue/update")
  @RequestMapping(value = "/typeKeyValue/update", method = RequestMethod.POST)
  @POST
  @Operation(summary = "Update type key value",
      description = "Updates a type-key-value object.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Type-key-value object updated"),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or type-key-value object could not be updated",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })

  public void updateTypeKeyValue(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The type key value to add") @RequestBody TypeKeyValueJpa typeKeyValue,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project, TypeKeyValue): /update "
            + typeKeyValue.toString());
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String username = authorizeApp(securityService, authToken,
          "update type key value", UserRole.VIEWER);
      projectService.setLastModifiedBy(username);
      projectService.updateTypeKeyValue(typeKeyValue);
    } catch (Exception e) {
      handleException(e, "trying to update type key value ");

    } finally {
      projectService.close();
      securityService.close();
    }

  }

  @Override
  @Path("/typeKeyValue/remove/{id}")
  @RequestMapping(value = "/typeKeyValue/remove/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Operation(summary = "Remove type key value",
      description = "Removes a type-key-value object by id.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Type-key-value object removed"),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or type-key-value object could not be removed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })

  public void removeTypeKeyValue(
    @Parameter(
        description = "Type-key-value id", example = "1",
        required = true) @PathVariable("id") Long id,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project/TypeKeyValue): /remove " + id);
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      final String username = authorizeApp(securityService, authToken,
          "remove type key value", UserRole.VIEWER);
      projectService.setLastModifiedBy(username);
      projectService.removeTypeKeyValue(id);
    } catch (Exception e) {
      handleException(e, "trying to remove type key value ");

    } finally {
      projectService.close();
      securityService.close();
    }

  }

  @Override
  @Path("/typeKeyValue/find")
  @RequestMapping(value = "/typeKeyValue/find", method = RequestMethod.POST)
  @POST
  @Operation(summary = "Find type key values",
      description = "Returns type-key-value objects matching an optional Lucene query.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "Type-key-value objects returned",
              content = @Content(mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = TypeKeyValueListJpa.class))),
          @ApiResponse(responseCode = "500",
              description = "Authorization failed or the query could not be processed",
              content = @Content(mediaType = MediaType.TEXT_PLAIN,
                  schema = @Schema(type = "string")))
      })
  public TypeKeyValueList findTypeKeyValues(
    @Parameter(
        description = "Optional Lucene query") @RequestParam(value = "query",
            required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Paging/filtering/sorting parameters for type-key-value objects.",
        required = false,
        content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = PfsParameterJpa.class),
            examples = @ExampleObject(name = "First page",
                value = "{\"maxResults\":25,\"startIndex\":0}")))
    @RequestBody PfsParameterJpa pfs,
    @Parameter(
        hidden = true) @RequestHeader(value = "Authorization",
            required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Project): /find, " + query + " " + pfs);
    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken, "find type key values",
          UserRole.VIEWER);
      return projectService.findTypeKeyValuesForQuery(query, pfs);
    } catch (Exception e) {
      handleException(e, "trying to find type key values ");
      return null;
    } finally {
      projectService.close();
      securityService.close();
    }
  }
}
