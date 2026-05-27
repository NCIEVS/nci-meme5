/*
 * Copyright 2020 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package com.wci.umls.server.rest.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.wci.umls.server.jpa.model.AlgorithmConfigJpa;
import com.wci.umls.server.jpa.model.ComponentInfoJpa;
import com.wci.umls.server.jpa.model.ProcessConfigJpa;
import com.wci.umls.server.jpa.model.actions.ChangeEventJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.model.helpers.SearchResultJpa;
import com.wci.umls.server.jpa.model.helpers.TrackingRecordListJpa;
import com.wci.umls.server.jpa.model.helpers.WorkflowBinListJpa;
import com.wci.umls.server.jpa.model.helpers.WorkflowConfigListJpa;
import com.wci.umls.server.jpa.model.helpers.WorkflowEpochListJpa;
import com.wci.umls.server.jpa.model.helpers.content.SearchResultListJpa;
import com.wci.umls.server.jpa.model.workflow.ChecklistJpa;
import com.wci.umls.server.jpa.model.workflow.ChecklistNoteJpa;
import com.wci.umls.server.jpa.model.workflow.ClusterTypeStatsJpa;
import com.wci.umls.server.jpa.model.workflow.TrackingRecordJpa;
import com.wci.umls.server.jpa.model.workflow.WorkflowBinDefinitionJpa;
import com.wci.umls.server.jpa.model.workflow.WorkflowBinJpa;
import com.wci.umls.server.jpa.model.workflow.WorkflowConfigJpa;
import com.wci.umls.server.jpa.model.workflow.WorkflowEpochJpa;
import com.wci.umls.server.jpa.model.workflow.WorklistJpa;
import com.wci.umls.server.jpa.model.workflow.WorklistNoteJpa;
import com.wci.umls.server.model.algo.AlgorithmConfig;
import com.wci.umls.server.model.algo.ProcessConfig;
import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.model.algo.ValidationResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.web.multipart.MultipartFile;

import com.wci.umls.server.helpers.ChecklistList;
import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.LogEntry;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.QueryStyle;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.helpers.SearchResult;
import com.wci.umls.server.helpers.SearchResultList;
import com.wci.umls.server.helpers.StringList;
import com.wci.umls.server.helpers.TrackingRecordList;
import com.wci.umls.server.helpers.WorkflowBinList;
import com.wci.umls.server.helpers.WorkflowConfigList;
import com.wci.umls.server.helpers.WorkflowEpochList;
import com.wci.umls.server.helpers.WorklistList;
import com.wci.umls.server.jpa.algo.insert.RepartitionAlgorithm;
import com.wci.umls.server.jpa.algo.maint.MatrixInitializerAlgorithm;
import com.wci.umls.server.jpa.algo.maint.StampingAlgorithm;
import com.wci.umls.server.jpa.services.ProcessServiceJpa;
import com.wci.umls.server.jpa.services.ReportServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.services.rest.WorkflowServiceRest;
import com.wci.umls.server.model.actions.ChangeEvent;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.actions.MolecularActionList;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.workflow.Checklist;
import com.wci.umls.server.model.workflow.ClusterTypeStats;
import com.wci.umls.server.model.workflow.TrackingRecord;
import com.wci.umls.server.model.workflow.WorkflowAction;
import com.wci.umls.server.model.workflow.WorkflowBin;
import com.wci.umls.server.model.workflow.WorkflowBinDefinition;
import com.wci.umls.server.model.workflow.WorkflowConfig;
import com.wci.umls.server.model.workflow.WorkflowEpoch;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.model.workflow.Worklist;
import com.wci.umls.server.services.ProcessService;
import com.wci.umls.server.services.SecurityService;
import com.wci.umls.server.services.WorkflowService;
import com.wci.umls.server.services.handlers.WorkflowActionHandler;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST implementation for {@link WorkflowServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/workflow")
@Path("/workflow")
@Tag(name = "Workflow", description = "Operations supporting workflow")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class WorkflowServiceRestImpl extends RootServiceRestImpl implements WorkflowServiceRest {

  /** The lock. */
  private static final Object LOCK = new Object();

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link WorkflowServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public WorkflowServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @RequestMapping(value = "/config", method = RequestMethod.PUT)
  @PUT
  @Path("/config")
  @Operation(summary = "Add a workflow config",
      description = "Add a workflow config")
  @Override
  public WorkflowConfig addWorkflowConfig(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow config to add", required = true) @RequestBody WorkflowConfigJpa workflowConfig,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /config/" + projectId + " "
        + workflowConfig.toString() + " " + authToken);

    final String action = "trying to add workflow config";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      // Get project and set on config
      final Project project = workflowService.getProject(projectId);
      workflowConfig.setProject(project);

      final WorkflowConfig config = workflowService.addWorkflowConfig(workflowConfig);
      workflowService.addLogEntry(userName, projectId, config.getId(), null, null,
          "ADD workflowConfig - " + config);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("AddWorkflowConfig", authToken, "BINS",
          config.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

      return config;

    } catch (Exception e) {
      handleException(e, "trying to " + action);
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @RequestMapping(value = "/config/import", method = RequestMethod.POST)
  @POST
  @Path("/config/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Import workflow config",
      description = "Imports a workflow config")
  public WorkflowConfig importWorkflowConfig(
    @Parameter(description = "Content of members file", required = true) @RequestParam("file") MultipartFile file,
    @Parameter(description = "Project id, e.g. 12345", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    try (InputStream in = file.getInputStream()) {
      return importWorkflowConfig(in, projectId, authToken);
    }
  }

  /* see superclass */
  @Override
  public WorkflowConfig importWorkflowConfig(
    FormDataContentDisposition contentDispositionHeader, InputStream in, Long projectId,
    String authToken) throws Exception {
    return importWorkflowConfig(in, projectId, authToken);
  }

  /**
   * Imports a workflow config from a stream.
   *
   * @param in the input stream
   * @param projectId the project id
   * @param authToken the auth token
   * @return the workflow config
   * @throws Exception the exception
   */
  private WorkflowConfig importWorkflowConfig(InputStream in, Long projectId, String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /config/import?projectId=" + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "adding a process config", UserRole.ADMINISTRATOR);
      workflowService.setLastModifiedBy(userName);
      // This should be atomic
      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();

      // Load project
      final Project project = workflowService.getProject(projectId);

      // Convert to a String
      final String json = IOUtils.toString(in, "UTF-8");

      // Convert to an object
      final WorkflowConfigJpa workflow =
          ConfigUtility.getGraphForJson(json, WorkflowConfigJpa.class);

      // Clean up the imported process
      workflow.setProject(project);
      // Verify that passed projectId matches ID of the processConfig's project
      verifyProject(workflow, projectId);

      // Save steps
      final List<WorkflowBinDefinition> binDefinitions =
          new ArrayList<>(workflow.getWorkflowBinDefinitions());

      // Prep workflow config
      workflow.setId(null);
      workflow.getWorkflowBinDefinitions().clear();
      final WorkflowConfigList list = workflowService.findWorkflowConfigs(projectId,
          "type:\"" + QueryParserBase.escape(workflow.getType()) + "\"", null);
      if (list.size() > 0) {
        workflow.setType(
            workflow.getType() + " - " + ConfigUtility.DATE_YYYYMMDDHHMMSS.format(new Date()));
      }
      final WorkflowConfig newWorkflowConfig = workflowService.addWorkflowConfig(workflow);

      // Add bin definitions
      for (final WorkflowBinDefinition binDefinition : binDefinitions) {
        binDefinition.setId(null);
        binDefinition.setWorkflowConfig(newWorkflowConfig);
        newWorkflowConfig.getWorkflowBinDefinitions()
            .add(workflowService.addWorkflowBinDefinition(binDefinition));
      }

      workflowService.updateWorkflowConfig(newWorkflowConfig);
      workflowService.addLogEntry(userName, projectId, newWorkflowConfig.getId(), null, null,
          "IMPORT workflow config - " + newWorkflowConfig);

      workflowService.commit();
      return newWorkflowConfig;
    } catch (Exception e) {
      handleException(e, "trying to add a process config");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @RequestMapping(value = "/config/export", method = RequestMethod.POST,
      produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = "Export workflow config",
      description = "Exports a workflow config")
  public byte[] exportWorkflowConfigResponse(
    @Parameter(description = "Project id, e.g. 12345", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "WorkflowConfig id, e.g. 23425", required = true) @RequestParam(value = "workflowId", required = false) Long workflowId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    try (InputStream in = exportWorkflowConfig(projectId, workflowId, authToken)) {
      return in == null ? new byte[0] : in.readAllBytes();
    }
  }

  /* see superclass */
  @POST
  @Override
  @Produces("application/octet-stream")
  @Path("/config/export")
  public InputStream exportWorkflowConfig(
    @Parameter(description = "Project id, e.g. 12345", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "WorkflowConfig id, e.g. 23425", required = true) @RequestParam(value = "workflowId", required = false) Long workflowId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /config/export?projectId=" + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "adding a process config", UserRole.ADMINISTRATOR);
      workflowService.setLastModifiedBy(userName);

      // Load project/process
      final WorkflowConfig workflow = workflowService.getWorkflowConfig(workflowId);
      verifyProject(workflow, projectId);

      return new ByteArrayInputStream(
          ConfigUtility.getJsonForGraph(workflow).getBytes(StandardCharsets.UTF_8));

    } catch (Exception e) {
      handleException(e, "trying to export a workflow config");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/config", method = RequestMethod.POST)
  @POST
  @Path("/config")
  @Operation(summary = "Update a workflow config",
      description = "Update a workflow config")
  public void updateWorkflowConfig(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow config to update", required = true) @RequestBody WorkflowConfigJpa config,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /config/" + projectId + " " + config.getId() + " " + authToken);

    final String action = "trying to update workflow config";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorkflowConfig oldConfig = workflowService.getWorkflowConfig(config.getId());
      verifyProject(oldConfig, projectId);

      // Workflow bin maintenance is not performed through UI - re-update here.
      config.setWorkflowBinDefinitions(oldConfig.getWorkflowBinDefinitions());

      workflowService.updateWorkflowConfig(config);
      workflowService.addLogEntry(userName, projectId, config.getId(), null, null,
          "UPDATE workflowConfig - " + config);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("UpdateWorkflowConfig", authToken, "BINS",
          config.getId(), getProjectInfo(oldConfig.getProject()));
      sendChangeEvent(userName, event);
    } catch (Exception e) {
      handleException(e, "trying to " + action);
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist", method = RequestMethod.POST)
  @POST
  @Path("/worklist")
  @Operation(summary = "Update a worklist",
      description = "Update a worklist")
  public void updateWorklist(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Worklist to update", required = true) @RequestBody WorklistJpa worklist,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + projectId + " "
        + worklist.getId() + " " + authToken);

    final String action = "trying to update a worklist";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      // reconnect tracking records before saving worklist
      // (parameter worklist will have no records on it)
      final Worklist origWorklist = workflowService.getWorklist(worklist.getId());
      verifyProject(origWorklist, projectId);

      worklist.setTrackingRecords(origWorklist.getTrackingRecords());

      workflowService.updateWorklist(worklist);
      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "UPDATE worklist - " + worklist);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("UpdateWorklist", authToken, "WORKLIST",
          worklist.getId(), getProjectInfo(origWorklist.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to " + action);
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/config/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/config/{id}")
  @Operation(summary = "Remove a workflow config",
      description = "Remove a workflow config")
  public void removeWorkflowConfig(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow config id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /config " + id + " " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove workflow config", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorkflowConfig config = workflowService.getWorkflowConfig(id);
      verifyProject(config, projectId);

      // Remove all of the attached bin definitions
      for (WorkflowBinDefinition bin : new ArrayList<>(config.getWorkflowBinDefinitions())) {
        workflowService.removeWorkflowBinDefinition(bin.getId());
      }

      // Remove the workflow config itself
      workflowService.removeWorkflowConfig(id);

      workflowService.addLogEntry(userName, projectId, id, null, null,
          "REMOVE workflowConfig - " + id);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveWorkflowConfig", authToken, "BINS",
          config.getId(), getProjectInfo(config.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a workflow config");
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/config/{id}", method = RequestMethod.GET)
  @GET
  @Path("/config/{id}")
  @Operation(summary = "Get workflow config",
      description = "Gets a workflow config")
  public WorkflowConfig getWorkflowConfig(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow config id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /config/" + id + "  " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "remove workflow config", UserRole.AUTHOR);

      final WorkflowConfig config = workflowService.getWorkflowConfig(id);
      if (config != null) {
        verifyProject(config, projectId);
        workflowService.handleLazyInit(config);
      }

      // websocket - n/a
      return config;

    } catch (Exception e) {
      handleException(e, "trying to get a workflow config");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/epoch", method = RequestMethod.GET)
  @GET
  @Path("/epoch")
  @Operation(summary = "Get current workflow epoch",
      description = "Gets a workflow epoch")
  public WorkflowEpoch getCurrentWorkflowEpoch(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /workflow/epoch" + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken, "get workflow epoch",
          UserRole.AUTHOR);

      final WorkflowEpoch epoch =
          workflowService.getCurrentWorkflowEpoch(workflowService.getProject(projectId));

      return epoch;

    } catch (Exception e) {
      handleException(e, "trying to get a workflow config");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/config/all", method = RequestMethod.GET)
  @GET
  @Path("/config/all")
  @Operation(summary = "Get workflow configs",
      description = "Gets a workflow configs")
  public WorkflowConfigList getWorkflowConfigs(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /config/all" + "  " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "remove workflow config", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);
      final List<WorkflowConfig> configs = workflowService.getWorkflowConfigs(project);
      for (WorkflowConfig config : configs) {
        verifyProject(config, projectId);
        workflowService.handleLazyInit(config);
      }
      final WorkflowConfigList list = new WorkflowConfigListJpa();
      list.setObjects(configs);
      list.setTotalCount(list.size());

      // websocket - n/a

      return list;

    } catch (Exception e) {
      handleException(e, "trying to get a workflow config");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/worklist/{id}")
  @Operation(summary = "Remove a worklist",
      description = "Remove a worklist")
  public void removeWorklist(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + id);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove worklist", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);
      // do all of this in one transaction
      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();

      final Worklist worklist = workflowService.getWorklist(id);
      verifyProject(worklist, projectId);
      final Project project = workflowService.getProject(projectId);

      // Find workflow bin name
      final List<WorkflowBin> list = workflowService.getWorkflowBins(project, null);
      for (final WorkflowBin bin : list) {
        if (bin.getName().equals(worklist.getWorkflowBinName())) {
          for (final TrackingRecord record : bin.getTrackingRecords()) {
            if (worklist.getName().equals(record.getWorklistName())) {
              record.setWorklistName(null);
              workflowService.updateTrackingRecord(record);
            }
          }
        }
      }

      workflowService.removeWorklist(id, true);

      workflowService.addLogEntry(userName, projectId, id, null, null, "REMOVE worklist - " + id);

      workflowService.commit();

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveWorklist", authToken, "WORKLIST",
          worklist.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a worklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/checklist/{id}")
  @Operation(summary = "Remove a checklist",
      description = "Remove a checklist")
  public void removeChecklist(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/" + id);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove workflow config", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final Checklist checklist = workflowService.getChecklist(id);
      verifyProject(checklist, projectId);

      workflowService.removeChecklist(id, true);
      workflowService.addLogEntry(userName, projectId, id, null, null, "REMOVE checklist - " + id);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveChecklist", authToken, "CHECKLIST",
          checklist.getId(), getProjectInfo(checklist.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a checklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition", method = RequestMethod.PUT)
  @PUT
  @Path("/definition")
  @Operation(summary = "Add a workflow bin definition",
      description = "Add a workflow bin definition")
  public WorkflowBinDefinition addWorkflowBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "New definition should be positioned after this bin definition, e.g. 1") @RequestParam(value = "positionAfterId", required = false) Long positionAfterId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow bin definition to add", required = true) @RequestBody WorkflowBinDefinitionJpa binDefinition,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /definition/" + projectId + " "
        + positionAfterId + " " + binDefinition.getName() + " " + authToken);

    final String action = "trying to add workflow bin definition";
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);
      final Project project = workflowService.getProject(projectId);

      final WorkflowConfig config =
          workflowService.getWorkflowConfig(binDefinition.getWorkflowConfig().getId());
      verifyProject(config, projectId);

      // Make sure a workflow bin definition with the same name doesn't already
      // exist
      for (WorkflowBinDefinition workflowBinDefinition : workflowService
          .getWorkflowBinDefinitions(project, config.getType())) {
        if (workflowBinDefinition.getName().equals(binDefinition.getName())) {
          throw new LocalException("Bin with this name already exists: " + binDefinition.getName());
        }
      }

      // Add to list in workflow config and save
      List<WorkflowBinDefinition> definitions = config.getWorkflowBinDefinitions();

      if (binDefinition.getAutofix() == null) {
        binDefinition.setAutofix("");
      }
      final WorkflowBinDefinition def;
      // if no position stated, add definition at the end of the list
      if (positionAfterId == null) {
        def = workflowService.addWorkflowBinDefinition(binDefinition);
        definitions.add(def);
      } else {
        // otherwise, add definition at position indicated by user
        int afterThisBinIndex = definitions.size();
        for (int i = 0; i < definitions.size(); i++) {
          if (definitions.get(i).getId().equals(positionAfterId)) {
            afterThisBinIndex = i + 1;
            break;
          }
        }
        def = workflowService.addWorkflowBinDefinition(binDefinition);
        definitions.add(afterThisBinIndex, def);
      }

      workflowService.addLogEntry(userName, projectId, def.getId(), null, null,
          "ADD workflow bin definition - " + def);

      workflowService.updateWorkflowConfig(config);

      workflowService.addLogEntry(userName, projectId, config.getId(), null, null,
          "UPDATE workflow config definition - " + def);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("AddWorkflowBinDefinition", authToken, "BINS",
          def.getId(), getProjectInfo(config.getProject()));
      sendChangeEvent(userName, event);

      return def;
    } catch (Exception e) {
      handleException(e, "trying to add workflow bin definition");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/epoch", method = RequestMethod.PUT)
  @PUT
  @Path("/epoch")
  @Operation(summary = "Add a workflow epoch",
      description = "Add a workflow epoch")
  public WorkflowEpoch addWorkflowEpoch(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow epoch to add", required = true) @RequestBody WorkflowEpochJpa epoch,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /epoch/" + projectId + " " + epoch.getName() + " " + authToken);

    final String action = "trying to add workflow bin definition";
    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      // Get project and set on config
      final Project project = workflowService.getProject(projectId);
      epoch.setProject(project);

      final WorkflowEpoch newEpoch = workflowService.addWorkflowEpoch(epoch);
      workflowService.addLogEntry(userName, projectId, newEpoch.getId(), null, null,
          "ADD workflow epoch- " + newEpoch);

      // Websocket notification - n/a

      return epoch;

    } catch (Exception e) {
      handleException(e, "trying to add workflow epoch");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/epoch/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/epoch/{id}")
  @Operation(summary = "Remove a workflow epoch",
      description = "Remove a workflow epoch")
  public void removeWorkflowEpoch(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow epoch id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /epoch " + id + " " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove workflow epoch", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorkflowEpoch epoch = workflowService.getWorkflowEpoch(id);
      verifyProject(epoch, projectId);

      workflowService.removeWorkflowEpoch(id);

      workflowService.addLogEntry(userName, projectId, id, null, null,
          "REMOVE workflow epoch - " + id);

      // Websocket notification - n/a

    } catch (Exception e) {
      handleException(e, "trying to remove a workflow epoch");
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition", method = RequestMethod.POST)
  @POST
  @Path("/definition")
  @Operation(summary = "Update a workflow bin definition",
      description = "Update a workflow bin definition")
  public void updateWorkflowBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow bin definition to update", required = true) @RequestBody WorkflowBinDefinitionJpa def,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /definition  " + projectId + " " + def.getId() + " " + authToken);

    final String action = "trying to update workflow bin definition";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      String userName = authorizeProject(workflowService, projectId, securityService, authToken,
          action, UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);
      final Project project = workflowService.getProject(projectId);
      final WorkflowBinDefinition origDef = workflowService.getWorkflowBinDefinition(def.getId());
      verifyProject(origDef.getWorkflowConfig(), projectId);

      def.setWorkflowConfig(origDef.getWorkflowConfig());

      // Lookup and update this definition's bin, if any
      for (WorkflowBin workflowBin : workflowService.getWorkflowBins(project,
          origDef.getWorkflowConfig().getType())) {
        if (workflowBin.getName().equals(origDef.getName())) {
          // Update the bin based on the updated definition
          workflowBin.setEnabled(def.isEnabled());
          workflowBin.setName(def.getName());
          workflowBin.setRequired(def.isRequired());
          workflowService.updateWorkflowBin(workflowBin);
          break;
        }
      }

      workflowService.updateWorkflowBinDefinition(def);

      workflowService.addLogEntry(userName, projectId, def.getId(), null, null,
          "UPDATE workflow bin definition - " + def);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("UpdateWorkflowBinDefinition", authToken, "BINS",
          def.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to update workflow bin definition");
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/definition/{id}")
  @Operation(summary = "Remove a workflow bin definition",
      description = "Remove a workflow bin definition")
  public void removeWorkflowBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin definition id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /definition/" + id);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove workflow bin definition", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      // load the bin definition, get its workflow config. Remove it from
      // workflow config, then remove it.
      final Project project = workflowService.getProject(projectId);
      final WorkflowBinDefinition def = workflowService.getWorkflowBinDefinition(id);
      verifyProject(def.getWorkflowConfig(), projectId);

      final WorkflowConfig workflowConfig = def.getWorkflowConfig();
      workflowConfig.getWorkflowBinDefinitions().remove(def);
      workflowService.updateWorkflowConfig(workflowConfig);
      workflowService.removeWorkflowBinDefinition(id);
      workflowService.addLogEntry(userName, projectId, id, null, null,
          "REMOVE workflow bin definition - " + id);

      // Lookup and remove this definition's bin and associated tracking
      // records, if any
      for (WorkflowBin workflowBin : workflowService.getWorkflowBins(project,
          def.getWorkflowConfig().getType())) {
        if (workflowBin.getName().equals(def.getName())) {
          workflowService.removeWorkflowBin(workflowBin.getId(), true);
          break;
        }
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveWorkflowBinDefinition", authToken, "BINS",
          def.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);
    } catch (Exception e) {
      handleException(e, "trying to remove a workflow bin definition");
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/bin/{id}")
  @Operation(summary = "Remove a workflow bin ",
      description = "Remove a workflow bin ")
  public void removeWorkflowBin(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/" + id);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove workflow bin definition", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorkflowBin bin = workflowService.getWorkflowBin(id);
      verifyProject(bin, projectId);

      workflowService.removeWorkflowBin(id, true);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveWorkflowBin", authToken, "BINS",
          bin.getId(), getProjectInfo(bin.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a workflow bin");
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition/{id}", method = RequestMethod.GET)
  @GET
  @Path("/definition/{id}")
  @Operation(summary = "Get workflow bin definition",
      description = "Gets workflow bin definition")
  public WorkflowBinDefinition getWorkflowBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin definition id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /definition/" + id + " " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "get workflow bin definition", UserRole.AUTHOR);

      final WorkflowBinDefinition definition = workflowService.getWorkflowBinDefinition(id);

      if (definition != null) {
        verifyProject(definition.getWorkflowConfig(), projectId);
        workflowService.handleLazyInit(definition);
      }
      // websocket - n/a

      return definition;

    } catch (Exception e) {
      handleException(e, "trying to get a workflow bin definition");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition", method = RequestMethod.GET)
  @GET
  @Path("/definition")
  @Operation(summary = "Get workflow bin definition",
      description = "Gets workflow bin definition by name")
  public WorkflowBinDefinition getWorkflowBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin definition name, e.g. demotions", required = true) @RequestParam(value = "name", required = false) String name,
    @Parameter(description = "Workflow bin type", required = true) @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /definition/" + name + " " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "get workflow bin definition", UserRole.AUTHOR);
      final Project project = workflowService.getProject(projectId);
      final List<WorkflowBinDefinition> definitions =
          workflowService.getWorkflowBinDefinitions(project, type);
      for (WorkflowBinDefinition definition : definitions) {
        if (definition.getName().equals(name)) {
          verifyProject(definition.getWorkflowConfig(), projectId);
          workflowService.handleLazyInit(definition);
          return definition;
        }
      }

      // websocket - n/a
      return null;

    } catch (Exception e) {
      handleException(e, "trying to get a workflow bin definition");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/clear/all", method = RequestMethod.POST)
  @POST
  @Path("/bin/clear/all")
  @Operation(summary = "Clear bins",
      description = "Clear bins")
  public void clearBins(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin type", required = true) @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/clear/all " + type);

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to clear bins", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final Project project = workflowService.getProject(projectId);
      final List<WorkflowBin> results = workflowService.getWorkflowBins(project, type);

      // remove bins and all of the tracking records in the bins
      for (final WorkflowBin workflowBin : results) {
        verifyProject(workflowBin, projectId);
        workflowService.removeWorkflowBin(workflowBin.getId(), true);
      }
      workflowService.addLogEntry(userName, projectId, null, null, null,
          "CLEAR BINS - " + projectId + ", " + type);

      // websocket - n/a
    } catch (Exception e) {
      handleException(e, "trying to clear bins");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/regenerate/all", method = RequestMethod.POST)
  @POST
  @Path("/bin/regenerate/all")
  @Operation(summary = "Regenerate bins",
      description = "Regenerate bins")
  public void regenerateBins(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin type", required = true) @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/regenerate/all " + type);

    // Only one user can regenerate bins at a time
    synchronized (LOCK) {
      // Instantiate services
      final ProcessService processService = new ProcessServiceJpa();
      final RepartitionAlgorithm algorithm = new RepartitionAlgorithm();
      try {

        // Authorize project role, get userName
        final String userName = authorizeProject(algorithm, projectId, securityService, authToken,
            "stamping worklist", UserRole.AUTHOR);
        final Project project = algorithm.getProject(projectId);

        // Lookup workflow config
        final WorkflowConfig workflowConfig = algorithm.getWorkflowConfig(project, type);

        // Set up and run the algorithm
        final Properties algoProperties = new Properties();
        algoProperties.put("type", type);
        algoProperties.put("UIRun", "true");
        algorithm.setProperties(algoProperties);
        algorithm.setLastModifiedBy(userName);

        processService.executeSingleAlgorithm(algorithm, project);

        // TODO question what would correct objectId be?
        // Websocket notification
        final ChangeEvent event = new ChangeEventJpa("RegenerateBins", authToken, "BINS",
            workflowConfig.getId(), getProjectInfo(project));
        sendChangeEvent(userName, event);

      } catch (Exception e) {
        try {
          algorithm.rollback();
        } catch (Exception e2) {
          // n/a, if this fails algo is already rolled back.
        }
        handleException(e, "trying to regenerate bins");
      } finally {
        algorithm.close();
        processService.close();
        securityService.close();
      }
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/record/assigned", method = RequestMethod.POST)
  @POST
  @Path("/record/assigned")
  @Operation(summary = "Find assigned work",
      description = "Finds tracking records assigned")
  public TrackingRecordList findAssignedWork(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "User name") @RequestParam(value = "userName", required = false) String userName,
    @Parameter(description = "User role, e.g. AUTHOR") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /record/assigned ");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find assigned work", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      // find available tracking records
      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final TrackingRecordList trackingRecords =
          handler.findAssignedWork(project, userName, role, pfs, workflowService);

      for (final TrackingRecord tr : trackingRecords.getObjects()) {
        workflowService.handleLazyInit(tr);
      }

      // websocket - n/a

      return trackingRecords;
    } catch (Exception e) {
      handleException(e, "trying to find assigned work");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/record/done", method = RequestMethod.POST)
  @POST
  @Path("/record/done")
  @Operation(summary = "Find done work",
      description = "Finds tracking records done")
  public TrackingRecordList findDoneWork(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "User name") @RequestParam(value = "userName", required = false) String userName,
    @Parameter(description = "User role, e.g. AUTHOR") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /record/done ");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find done work", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      // find available tracking records
      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final TrackingRecordList trackingRecords =
          handler.findDoneWork(project, userName, role, pfs, workflowService);

      for (final TrackingRecord tr : trackingRecords.getObjects()) {
        workflowService.handleLazyInit(tr);
      }

      // websocket - n/a

      return trackingRecords;
    } catch (Exception e) {
      handleException(e, "trying to find done work");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/record/available", method = RequestMethod.POST)
  @POST
  @Path("/record/available")
  @Operation(summary = "Find available work",
      description = "Finds tracking records available for work")
  public TrackingRecordList findAvailableWork(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "UserRole") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /record/available ");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to find available work", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      // find available tracking records
      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final TrackingRecordList trackingRecords =
          handler.findAvailableWork(project, userName, role, pfs, workflowService);
      for (final TrackingRecord tr : trackingRecords.getObjects()) {
        workflowService.handleLazyInit(tr);
      }

      // websocket - n/a

      return trackingRecords;
    } catch (Exception e) {
      handleException(e, "trying to find available work");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/{id}/records", method = RequestMethod.POST)
  @POST
  @Path("/checklist/{id}/records")
  @Operation(summary = "Find tracking records for checklist",
      description = "Finds tracking records for checklist")
  public TrackingRecordList findTrackingRecordsForChecklist(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 5") @PathVariable("id") Long id,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/" + id + "/records");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find records for checklist", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);
      final Checklist checklist = workflowService.getChecklist(id);

      // Can just search on checklist name because the only tracking
      // records with the checklist name will be attached to this checklist
      final TrackingRecordList list = workflowService.findTrackingRecords(project,
          "checklistName:\"" + checklist.getName() + "\"", pfs);
      List<TrackingRecord> recordListWithConcepts = new ArrayList<>();
      for (final TrackingRecord record : list.getObjects()) {
        TrackingRecord recordWithConcepts = workflowService.lookupTrackingRecordConcepts(record);
        recordListWithConcepts.add(recordWithConcepts);
      }

      // websocket - n/a

      list.setObjects(recordListWithConcepts);
      return list;

    } catch (Exception e) {
      handleException(e, "trying to find records for checklist ");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}/records", method = RequestMethod.POST)
  @POST
  @Path("/worklist/{id}/records")
  @Operation(summary = "Find records for worklist",
      description = "Finds tracking records for worklist")
  public TrackingRecordList findTrackingRecordsForWorklist(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 5") @PathVariable("id") Long id,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + id + "/records");

    WorkflowService workflowService = null; 
    try {
      workflowService = new WorkflowServiceJpa();
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find records for worklist", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);
      final Worklist worklist = workflowService.getWorklist(id);

      // Compose query of all of the tracking record ids
      // Can not just use worklist name because the workflow bin
      // tracking records ALSO have the worklist name set.
      final List<String> clauses = worklist.getTrackingRecords().stream()
          .map(r -> "id:" + r.getId()).collect(Collectors.toList());
      final String query = ConfigUtility.composeQuery("OR", clauses);

      if (query.isEmpty()) {
        return new TrackingRecordListJpa();
      } //"id:39527201"
      final TrackingRecordList list = workflowService.findTrackingRecords(project, query, pfs);
      for (final TrackingRecord record : list.getObjects()) {
        workflowService.lookupTrackingRecordConcepts(record);
      }

      // websocket - n/a

      return list;

    } catch (Exception e) {
      handleException(e, "trying to find records for worklist ");
    } finally {
      if (workflowService != null) {
        workflowService.close();
      }
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/{id}/records", method = RequestMethod.POST)
  @POST
  @Path("/bin/{id}/records")
  @Operation(summary = "Find records for workflow bin",
      description = "Finds tracking records for workflow bin")
  public TrackingRecordList findTrackingRecordsForWorkflowBin(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "WorkflowBin id, e.g. 5") @PathVariable("id") Long id,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/" + id + "/records");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find records for workflow bin", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);
      final WorkflowBin bin = workflowService.getWorkflowBin(id);
      // Compose query of all of the tracking record ids
      final List<String> clauses = bin.getTrackingRecords().stream().map(r -> "id:" + r.getId())
          .collect(Collectors.toList());
      final String query = ConfigUtility.composeQuery("OR", clauses);

      if (query.isEmpty()) {
        return new TrackingRecordListJpa();
      }

      final TrackingRecordList list = workflowService.findTrackingRecords(project, query, pfs);
      for (final TrackingRecord record : list.getObjects()) {
        workflowService.lookupTrackingRecordConcepts(record);
      }

      // websocket - n/a

      return list;

    } catch (Exception e) {
      handleException(e, "trying to find records for bin ");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/assigned", method = RequestMethod.POST)
  @POST
  @Path("/worklist/assigned")
  @Operation(summary = "Find assigned worklists",
      description = "Finds worklists assigned for work")
  public WorklistList findAssignedWorklists(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "User name") @RequestParam(value = "userName", required = false) String userName,
    @Parameter(description = "User role, e.g. AUTHOR") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/assigned, " + projectId
        + ", " + userName + ", " + role);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find assigned worklists", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final WorklistList list =
          handler.findAssignedWorklists(project, userName, role, pfs, workflowService);

      // websocket - n/a

      return list;
    } catch (Exception e) {
      handleException(e, "trying to find assigned worklists");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/done", method = RequestMethod.POST)
  @POST
  @Path("/worklist/done")
  @Operation(summary = "Find done worklists",
      description = "Finds worklists done for work")
  public WorklistList findDoneWorklists(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "User name") @RequestParam(value = "userName", required = false) String userName,
    @Parameter(description = "User role, e.g. AUTHOR") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /worklist/done, " + projectId + ", " + userName + ", " + role);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find done worklists", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final WorklistList list =
          handler.findDoneWorklists(project, userName, role, pfs, workflowService);

      // websocket - n/a

      return list;
    } catch (Exception e) {
      handleException(e, "trying to find done worklists");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/find", method = RequestMethod.POST)
  @POST
  @Path("/checklist/find")
  @Operation(summary = "Find checklists",
      description = "Finds checklists for query")
  public ChecklistList findChecklists(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query") @RequestParam(value = "query", required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /checklist/find " + projectId + " " + query + " " + authToken);

    final String action = "trying to find checklists";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      authorizeProject(workflowService, projectId, securityService, authToken, action,
          UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);
      ChecklistList list = workflowService.findChecklists(project, query, pfs);
      for (Checklist checklist : list.getObjects()) {
        workflowService.handleLazyInit(checklist);
      }

      // Compute "cluster" and "concept" counts
      /*
       * for (final Checklist checklist : list.getObjects()) {
       * checklist.getStats().put("clusterCt",
       * checklist.getTrackingRecords().size()); // Add up orig concepts size
       * from all tracking records checklist.getStats().put("conceptCt",
       * checklist.getTrackingRecords().stream()
       * .collect(Collectors.summingInt(w -> w.getOrigConceptIds().size()))); }
       */

      // websocket - n/a

      return list;
    } catch (Exception e) {
      handleException(e, action);
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/find", method = RequestMethod.POST)
  @POST
  @Path("/worklist/find")
  @Operation(summary = "Find worklists",
      description = "Finds worklists for query")
  public WorklistList findWorklists(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query") @RequestParam(value = "query", required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /worklist/find " + projectId + " " + query + " " + authToken);

    final String action = "trying to find worklists";
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      // authorize and get user name from the token
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, action, UserRole.AUTHOR);
      final Project project = workflowService.getProject(projectId);

      // Assume current epoch unless explicit
      final String localQuery =
          (query != null && !query.contains("epoch:")) ? ConfigUtility.composeQuery("AND", query,
              "epoch:" + workflowService.getCurrentWorkflowEpoch(project)) : query;

      // find worklists
      final WorklistList list =
          workflowService.findWorklists(workflowService.getProject(projectId), localQuery, pfs);

      // Compute "cluster" and "concept" counts and assignment availability
      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      for (final Worklist worklist : list.getObjects()) {
        worklist.getStats().put("clusterCt", worklist.getTrackingRecords().size());
        // Add up orig concepts size from all tracking records
        worklist.getStats().put("conceptCt", worklist.getTrackingRecords().stream()
            .collect(Collectors.summingInt(w -> w.getOrigConceptIds().size())));
        worklist.setAuthorAvailable(handler.isAvailable(worklist, userName, UserRole.AUTHOR));
        worklist.setReviewerAvailable(handler.isAvailable(worklist, userName, UserRole.REVIEWER));
      }

      // websocket - n/a

      return list;
    } catch (Exception e) {
      handleException(e, "trying to find worklists");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/paths", method = RequestMethod.GET)
  @GET
  @Path("/paths")
  @Operation(summary = "Get workflow paths",
      description = "Gets the supported workflow paths")
  public StringList getWorkflowPaths(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /paths");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get workflow paths", UserRole.VIEWER);

      // websocket - n/a
      return workflowService.getWorkflowPaths();

    } catch (Exception e) {
      handleException(e, "trying to get workflow paths");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/epoch/all", method = RequestMethod.GET)
  @GET
  @Path("/epoch/all")
  @Operation(summary = "Get workflow epochs",
      description = "Gets the supported workflow epochs")
  public WorkflowEpochList getWorkflowEpochs(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /epochs");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get workflow epochs", UserRole.VIEWER);

      List<WorkflowEpoch> epochs =
          workflowService.getWorkflowEpochs(workflowService.getProject(projectId));

      WorkflowEpochList list = new WorkflowEpochListJpa();
      list.setObjects(epochs);
      list.setTotalCount(epochs.size());

      return list;

    } catch (Exception e) {
      handleException(e, "trying to get workflow epochs");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/action", method = RequestMethod.GET)
  @GET
  @Path("/worklist/action")
  @Operation(summary = "Perform workflow action on a tracking record",
      description = "Performs the specified action as the specified worklist as the specified user")
  public Worklist performWorkflowAction(
    @Parameter(description = "Project id, e.g. 5", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 5") @RequestParam(value = "worklistId", required = false) Long worklistId,
    @Parameter(description = "User name, e.g. author1", required = true) @RequestParam(value = "userName", required = false) String userName,
    @Parameter(description = "User role, e.g. AUTHOR", required = true) @RequestParam(value = "userRole", required = false) UserRole userRole,
    @Parameter(description = "Workflow action, e.g. 'SAVE'", required = true) @RequestParam(value = "action", required = false) WorkflowAction action,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /action " + projectId + ", "
        + worklistId + ", " + userName + ", " + action);

    // Test preconditions
    if (projectId == null || userName == null) {
      handleException(new Exception("Required parameter has a null value"), "");
    }

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String authName = authorizeProject(workflowService, projectId, securityService,
          authToken, "perform workflow action", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(authName);

      final Worklist worklist = workflowService.getWorklist(worklistId);
      verifyProject(worklist, projectId);

      final Project project = workflowService.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException("Editing is disabled on project: " + project.getName());
      }

      // UserRole role = UserRole.valueOf(userRole);
      final Worklist returnWorklist =
          workflowService.performWorkflowAction(project, worklist, userName, userRole, action);

      workflowService.addLogEntry(userName, projectId, null, null, null,
          "PERFORM " + action + " - " + projectId + ", " + worklistId + "," + worklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("PerformWorkflowAction", authToken, "WORKLIST",
          worklist.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

      return returnWorklist;
    } catch (Exception e) {
      handleException(e, "trying to perform workflow action");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/available", method = RequestMethod.POST)
  @POST
  @Path("/worklist/available")
  @Operation(summary = "Find available  worklists",
      description = "Finds worklists available for work")
  public WorklistList findAvailableWorklists(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "UserRole") @RequestParam(value = "role", required = false) UserRole role,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/available ");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to find available worklists", UserRole.AUTHOR);

      final Project project = workflowService.getProject(projectId);

      final WorkflowActionHandler handler =
          workflowService.getWorkflowHandlerForPath(project.getWorkflowPath());
      final WorklistList list =
          handler.findAvailableWorklists(project, userName, role, pfs, workflowService);

      // websocket - n/a

      return list;
    } catch (Exception e) {
      handleException(e, "trying to find available worklists");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist", method = RequestMethod.POST)
  @POST
  @Path("/checklist")
  @Operation(summary = "Create checklist",
      description = "Create checklist")
  public Checklist createChecklist(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin id, e.g. 5") @RequestParam(value = "workflowBinId", required = false) Long workflowBinId,
    @Parameter(description = "Cluster type") @RequestParam(value = "clusterType", required = false) String clusterType,
    @Parameter(description = "Checklist name") @RequestParam(value = "name", required = false) String name,
    @Parameter(description = "Checklist description") @RequestParam(value = "description", required = false) String description,
    @Parameter(description = "Randomize, e.g. false", required = true) @RequestParam(value = "randomize", required = false) Boolean randomize,
    @Parameter(description = "Exclude on worklist, e.g. false", required = true) @RequestParam(value = "excludeOnWorklist", required = false) Boolean excludeOnWorklist,
    @Parameter(description = "Query") @RequestParam(value = "query", required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist " + projectId + ", "
        + workflowBinId + ", " + clusterType + ", " + name + ", " + randomize);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to create checklist", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final Project project = workflowService.getProject(projectId);
      final WorkflowBin workflowBin = workflowService.getWorkflowBin(workflowBinId);

      // Check that checklist name isn't already in use
      ChecklistList matchingChecklistNames =
          findChecklists(projectId, name, new PfsParameterJpa(), authToken);
      if (matchingChecklistNames.size() != 0) {
        throw new LocalException("Checklist name " + name + " is already in use.");
      }

      // Build up list of identifiers
      final List<String> clauses = workflowBin.getTrackingRecords().stream()
          // Skip records on worklists if excludeWorklist is used
          // Skip records with a clusterType if cluster type doesn't match
          // Skip records without a clusterType if cluster type is set
          .filter(record -> !(excludeOnWorklist && !ConfigUtility.isEmpty(record.getWorklistName()))
              && !(!ConfigUtility.isEmpty(clusterType)
                  && !record.getClusterType().equals(clusterType))
              && !(ConfigUtility.isEmpty(clusterType)
                  && !ConfigUtility.isEmpty(record.getClusterType())))
          .map(r -> "id:" + r.getId()).collect(Collectors.toList());
      final String idQuery = ConfigUtility.composeQuery("OR", clauses);
      final String finalQuery = ConfigUtility.composeQuery("AND", idQuery, query);

      // Handle "randomize"
      if (randomize) {
        pfs.setSortField("RANDOM");
      }
      // default to clusterId sort
      else if (ConfigUtility.isEmpty(pfs.getSortField())) {
        pfs.setSortField("clusterId");
      }
      final TrackingRecordList list = workflowService.findTrackingRecords(project, finalQuery, pfs);

      final ChecklistJpa checklist = new ChecklistJpa();
      checklist.setName(name);
      if (description != null) {
        checklist.setDescription(description);
      } else {
        checklist.setDescription(name + " description");
      }
      checklist.setProject(project);
      checklist.setTimestamp(new Date());

      final Checklist newChecklist = workflowService.addChecklist(checklist);
      Long i = 1L;
      for (final TrackingRecord record : list.getObjects()) {
        final TrackingRecord copy = new TrackingRecordJpa(record);
        copy.setId(null);
        copy.setClusterId(i++);
        copy.setChecklistName(name);
        // Clear the worklist name so it doesn't interfere with
        // getConceptIdWorklistNameMap
        copy.setWorklistName(null);
        workflowService.addTrackingRecord(copy);
        newChecklist.getTrackingRecords().add(copy);
      }
      workflowService.updateChecklist(newChecklist);
      workflowService.addLogEntry(userName, projectId, newChecklist.getId(), null, null,
          "CREATE checklist - " + newChecklist);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("ComputeChecklist", authToken, "CHECKLIST",
          newChecklist.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

      return newChecklist;
    } catch (Exception e) {
      handleException(e, "trying to create checklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist", method = RequestMethod.PUT)
  @PUT
  @Path("/worklist")
  @Operation(summary = "Create worklist",
      description = "Create worklist")
  public Worklist createWorklist(
    @Parameter(description = "Project id, e.g. 5", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin id, e.g. 5", required = true) @RequestParam(value = "workflowBinId", required = false) Long workflowBinId,
    @Parameter(description = "Cluster type") @RequestParam(value = "clusterType", required = false) String clusterType,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist ");

    // Only allow one user in here at a time.
    synchronized (LOCK) {
      final WorkflowService workflowService = new WorkflowServiceJpa();
      try {
        final String userName = authorizeProject(workflowService, projectId, securityService,
            authToken, "trying to create worklist", UserRole.AUTHOR);
        workflowService.setLastModifiedBy(userName);

        final Project project = workflowService.getProject(projectId);
        if (!project.isEditingEnabled()) {
          throw new LocalException("Editing is disabled on project: " + project.getName());
        }

        final WorkflowBin workflowBin = workflowService.getWorkflowBin(workflowBinId);
        final WorkflowEpoch currentEpoch = workflowService.getCurrentWorkflowEpoch(project);

        if (workflowBin == null) {
          throw new LocalException(
              "Attempt to create a worklist from a nonexistent bin " + workflowBinId);
        }

        if (currentEpoch == null) {
          throw new Exception("No current workflow epoch exists for this project " + projectId);
        }

        // Compose the worklist name from the current epoch, the bin name,
        // and the max worklist id+1. (e.g. wrk16a_demotions_chem_001)
        final StringBuilder worklistName = new StringBuilder();
        worklistName.append("wrk").append(currentEpoch.getName()).append("_");
        worklistName.append(workflowBin.getName()).append("_");
        // Append clusterType or "default"
        if (!ConfigUtility.isEmpty(clusterType)) {
          worklistName.append(clusterType).append("_");
        } else {
          worklistName.append("default_");
        }

        // Obtain the next worklist number for this naming scheme
        final PfsParameter worklistQueryPfs = new PfsParameterJpa();
        worklistQueryPfs.setStartIndex(0);
        worklistQueryPfs.setMaxResults(1);
        worklistQueryPfs.setSortField("name");
        worklistQueryPfs.setAscending(false);
        final StringBuilder query = new StringBuilder();
        // Must use nameSort for non-analyzed field
        if (!ConfigUtility.isEmpty(clusterType)) {
          query.append("nameSort:").append("wrk").append(
              currentEpoch.getName() + "_" + workflowBin.getName() + "_" + clusterType + "_*");
        } else {
          query.append("nameSort:").append("wrk")
              .append(currentEpoch.getName() + "_" + workflowBin.getName() + "_default_" + '*');
        }
        final WorklistList worklistList =
            workflowService.findWorklists(project, query.toString(), worklistQueryPfs);
        int nextNumber = worklistList.getObjects().size() == 0 ? 1
            : worklistList.getObjects().get(0).getNumber() + 1;
        worklistName.append(new String(Integer.toString(nextNumber + 1000)).substring(1));

        // Find records from this workflow bin that are not on a worklist and
        // not owned by a checklist. [* TO *] is converted to proper Lucene 9
        // wildcard "exists" queries by IndexUtility.applyPfsToLuceneQuery.
        final PfsParameter localPfs =
            pfs == null ? new PfsParameterJpa() : new PfsParameterJpa(pfs);
        // Always work in clusterId order, unless specified by user
        localPfs.setSortField("clusterId");
        if (pfs != null && !ConfigUtility.isEmpty(pfs.getSortField())) {
          localPfs.setSortField(pfs.getSortField());
        }
        final String recordQuery = "workflowBinName:" + workflowBin.getName()
            + " AND NOT worklistName:[* TO *]"
            + " AND NOT checklistName:[* TO *]"
            + (ConfigUtility.isEmpty(clusterType) ? " AND clusterType:\"\""
                : " AND clusterType:" + clusterType);
        final TrackingRecordList recordResultList =
            workflowService.findTrackingRecords(project, recordQuery, localPfs);

        // Bail if there are no more records to make worklists from
        if (recordResultList.size() == 0) {
          throw new LocalException("No more unassigned clusters in workflow bin");
        }

        final WorklistJpa worklist = new WorklistJpa();
        worklist.setName(worklistName.toString());
        worklist.setDescription(worklistName.toString() + " description");
        worklist.setProject(project);
        worklist.setWorkflowStatus(WorkflowStatus.NEW);
        worklist.setNumber(nextNumber);
        worklist.setProjectId(project.getId());
        worklist.setTimestamp(new Date());
        worklist.setWorkflowBinName(workflowBin.getName());
        worklist.setEpoch(workflowService.getCurrentWorkflowEpoch(project).getName());

        // Log created
        worklist.getWorkflowStateHistory().put("Created", new Date());

        final Worklist newWorklist = workflowService.addWorklist(worklist);

        long i = 1L;
        for (final TrackingRecord record : recordResultList.getObjects()) {
          // Set worklist name of bin's copy of tracking record
          record.setWorklistName(worklistName.toString());
          workflowService.updateTrackingRecord(record);
          // Reuse bins tracking record for worklist
          final TrackingRecord copy = new TrackingRecordJpa(record);
          copy.setId(null);
          copy.setClusterId(i++);
          copy.setWorklistName(worklistName.toString());
          workflowService.addTrackingRecord(copy);

          newWorklist.getTrackingRecords().add(copy);
        }
        workflowService.updateWorklist(newWorklist);

        workflowService.addLogEntry(userName, projectId, newWorklist.getId(), null, null,
            "CREATE worklist- " + newWorklist);

        // Websocket notification
        final ChangeEvent event = new ChangeEventJpa("ComputeWorklist", authToken, "WORKLIST",
            newWorklist.getId(), getProjectInfo(project));
        sendChangeEvent(userName, event);

        return newWorklist;
      } catch (Exception e) {
        handleException(e, "trying to create worklist");
      } finally {
        workflowService.close();
        securityService.close();
      }
      return null;
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/all", method = RequestMethod.GET)
  @GET
  @Path("/bin/all")
  @Operation(summary = "Get workflow bins",
      description = "Gets the workflow bins for the project and type.")
  public WorkflowBinList getWorkflowBins(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin type, e.g. MUTUALLY_EXCLUSIVE") @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/all " + type);
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to get workflow bins", UserRole.AUTHOR);
      final Project project = workflowService.getProject(projectId);
      final List<WorkflowBin> bins = workflowService.getWorkflowBins(project, type);

      // Track "unassigned" and "assigned"
      final Map<String, Integer> typeAssignedMap = new HashMap<>();
      final Map<String, Integer> typeUnassignedMap = new HashMap<>();
      for (final WorkflowBin bin : bins) {
        verifyProject(bin, projectId);
        typeAssignedMap.clear();
        typeUnassignedMap.clear();
        final List<TrackingRecord> list = bin.getTrackingRecords();

        // If no tracking records, get the raw cluster ct
        if (list.size() == 0) {
          final ClusterTypeStats stats = new ClusterTypeStatsJpa();
          stats.setClusterType("all");
          stats.getStats().put("all", bin.getClusterCt());
          bin.getStats().add(stats);

          // skip the next section in this case
          continue;
        }

        for (final TrackingRecord record : list) {
          String clusterType = record.getClusterType();
          if (clusterType.isEmpty()) {
            clusterType = "default";
          }

          // Initialize map
          if (!typeAssignedMap.containsKey(clusterType)) {
            typeAssignedMap.put(clusterType, 0);
            typeUnassignedMap.put(clusterType, 0);
          }
          // compute "all" cluster type
          if (!typeAssignedMap.containsKey("all")) {
            typeAssignedMap.put("all", 0);
            typeUnassignedMap.put("all", 0);
          }

          // Increment assigned
          if (!ConfigUtility.isEmpty(record.getWorklistName())) {
            typeAssignedMap.put(clusterType, typeAssignedMap.get(clusterType) + 1);
            typeAssignedMap.put("all", typeAssignedMap.get("all") + 1);
          }

          // Otherwise increment unassigned
          else {
            typeUnassignedMap.put(clusterType, typeUnassignedMap.get(clusterType) + 1);
            typeUnassignedMap.put("all", typeUnassignedMap.get("all") + 1);
          }

        }
        // Now extract cluster types and add statistics
        for (final String clusterType : typeAssignedMap.keySet()) {

          // N/A // Skip "all" if there is only one cluster type
          // if (typeAssignedMap.keySet().size() == 2
          // && clusterType.equals("all")) {
          // continue;
          // }

          // Add statistics
          ClusterTypeStats stats = new ClusterTypeStatsJpa();
          stats.setClusterType(clusterType);
          int unassigned = typeUnassignedMap.get(clusterType);
          int assigned = typeAssignedMap.get(clusterType);
          stats.getStats().put("all", unassigned + assigned);
          stats.getStats().put("unassigned", unassigned);
          stats.getStats().put("assigned", assigned);
          bin.getStats().add(stats);
        }
      }

      Collections.sort(bins, (o1, o2) -> o1.getRank() - o2.getRank());

      // Add "bins" for definitions that don't have bins yet.
      // Just add them at the end, there are too many situations
      // where it would be hard to find out the right order.
      // Next regenerate will fix it.
      final WorkflowConfig config = workflowService.getWorkflowConfig(project, type);
      Set<String> binNames = bins.stream().map(b -> b.getName()).collect(Collectors.toSet());
      for (final WorkflowBinDefinition def : config.getWorkflowBinDefinitions()) {
        if (!binNames.contains(def.getName())) {
          bins.add(new WorkflowBinJpa(def));
        }
      }

      final WorkflowBinList list = new WorkflowBinListJpa();
      list.setObjects(bins);
      list.setTotalCount(list.size());

      // websocket - n/a
      return list;

    } catch (Exception e) {
      handleException(e, "trying to get workflow bin stats");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}", method = RequestMethod.GET)
  @GET
  @Path("/worklist/{id}")
  @Operation(summary = "Get the worklist",
      description = "Gets the statistics for the worklist.")
  public Worklist getWorklist(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 5") @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + id);
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to get worklist stats", UserRole.AUTHOR);

      workflowService.setLastModifiedBy(userName);

      final Worklist worklist = workflowService.getWorklist(id);
      if (worklist == null) {
        return null;
      }

      verifyProject(worklist, projectId);
      Project project = workflowService.getProject(projectId);

      Set<Long> approvedByEditorIds = new HashSet<>();
      Set<Long> mergeIds = new HashSet<>();
      Set<Long> splitsIds = new HashSet<>();
      Set<Long> relsInsertedIds = new HashSet<>();
      Set<Long> stysInsertedIds = new HashSet<>();
      Set<Long> approvedIds = new HashSet<>();
      Set<Long> stampedIds = new HashSet<>();

      worklist.getStats().put("clusterCt", worklist.getTrackingRecords().size());
      // Add up orig concepts size from all tracking records
      int conceptCt = worklist.getTrackingRecords().stream()
          .collect(Collectors.summingInt(w -> w.getOrigConceptIds().size()));
      worklist.getStats().put("conceptCt", conceptCt);
      String query = "activityId:" + worklist.getName();
      MolecularActionList list = workflowService.findMolecularActions(null,
          project.getTerminology(), project.getVersion(), query, null);
      // compute the stats and add them to the stats object
      // n_actions -1 - molecular action search by concept ids on worklist
      worklist.getStats().put("actionsCt", list.size());

      for (MolecularAction action : list.getObjects()) {

        // n_approved -1 - "APPROVE_CONCEPT" molecular actions
        if (action.getName().equals("APPROVE")) {
          approvedIds.add(action.getComponentId());
        }

        // n_approved_by_editor -1 - "APPROVE_CONCEPT" molecular actions with
        // editors initial
        if (action.getName().equals("APPROVE")
            && worklist.getAuthors().contains(action.getLastModifiedBy().replace("E-", ""))) {
          approvedByEditorIds.add(action.getComponentId());
        }

        // n_stamped -1 - "APPROVE_CONCEPT" molecular actions with editors
        // stampinginitial
        if (action.getName().equals("APPROVE")
            && worklist.getAuthors().contains(action.getLastModifiedBy().replace("S-", ""))) {
          stampedIds.add(action.getComponentId());
        }

        // n_rels_inserted -1 - "ADD_RELATIONSHIP" molecular actions
        if (action.getName().equals("ADD_RELATIONSHIP")) {
          relsInsertedIds.add(action.getComponentId());
        }

        // n_stys_inserted -1 - "ADD_SEMANTIC_TYPE" molecular actions
        if (action.getName().equals("ADD_SEMANTIC_TYPE")) {
          stysInsertedIds.add(action.getComponentId());
        }

        // n_splits -1 - "SPLIT" molecular actions
        if (action.getName().equals("SPLIT")) {
          splitsIds.add(action.getComponentId());
        }

        // n_merges -1 - "MERGE" molecular actions
        if (action.getName().equals("MERGE")) {
          mergeIds.add(action.getComponentId());
        }
      }
      // n_not_stamped -1 - concepts without APPROVE_CONCEPT actions
      // all concept ids (save in set) - all concept ids for approve
      // actions in set
      worklist.getStats().put("notStampedCt", conceptCt - approvedIds.size());

      // add all stats to worklist
      worklist.getStats().put("approveCt", approvedIds.size());
      worklist.getStats().put("approveByEditorCt", approvedByEditorIds.size());
      worklist.getStats().put("stampedCt", stampedIds.size());
      worklist.getStats().put("relsInsertedCt", relsInsertedIds.size());
      worklist.getStats().put("stysInsertedCt", stysInsertedIds.size());
      worklist.getStats().put("splitsCt", splitsIds.size());
      worklist.getStats().put("mergeCt", mergeIds.size());

      // websocket - n/a

      // return the worklist
      return worklist;

    } catch (Exception e) {
      handleException(e, "trying to get worklist stats");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/{id}", method = RequestMethod.GET)
  @GET
  @Path("/checklist/{id}")
  @Operation(summary = "Get the checklist",
      description = "Gets the statistics for the checklist.")
  public Checklist getChecklist(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 5") @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/" + id);
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to get checklist stats", UserRole.AUTHOR);

      workflowService.setLastModifiedBy(userName);

      final Checklist checklist = workflowService.getChecklist(id);
      if (checklist == null) {
        return null;
      }

      verifyProject(checklist, projectId);
      Project project = workflowService.getProject(projectId);

      Set<Long> mergeIds = new HashSet<>();
      Set<Long> splitsIds = new HashSet<>();
      Set<Long> relsInsertedIds = new HashSet<>();
      Set<Long> stysInsertedIds = new HashSet<>();
      Set<Long> approvedIds = new HashSet<>();

      checklist.getStats().put("clusterCt", checklist.getTrackingRecords().size());
      // Add up orig concepts size from all tracking records
      int conceptCt = checklist.getTrackingRecords().stream()
          .collect(Collectors.summingInt(w -> w.getOrigConceptIds().size()));
      checklist.getStats().put("conceptCt", conceptCt);
      String query = "activityId:" + checklist.getName();
      MolecularActionList list = workflowService.findMolecularActions(null,
          project.getTerminology(), project.getVersion(), query, null);
      // compute the stats and add them to the stats object
      // n_actions -1 - molecular action search by concept ids on checklist
      checklist.getStats().put("actionsCt", list.size());

      for (MolecularAction action : list.getObjects()) {

        // n_approved -1 - "APPROVE_CONCEPT" molecular actions
        if (action.getName().equals("APPROVE")) {
          approvedIds.add(action.getComponentId());
        }

        // n_rels_inserted -1 - "ADD_RELATIONSHIP" molecular actions
        if (action.getName().equals("ADD_RELATIONSHIP")) {
          relsInsertedIds.add(action.getComponentId());
        }

        // n_stys_inserted -1 - "ADD_SEMANTIC_TYPE" molecular actions
        if (action.getName().equals("ADD_SEMANTIC_TYPE")) {
          stysInsertedIds.add(action.getComponentId());
        }

        // n_splits -1 - "SPLIT" molecular actions
        if (action.getName().equals("SPLIT")) {
          splitsIds.add(action.getComponentId());
        }

        // n_merges -1 - "MERGE" molecular actions
        if (action.getName().equals("MERGE")) {
          mergeIds.add(action.getComponentId());
        }
      }
      // n_not_stamped -1 - concepts without APPROVE_CONCEPT actions
      // all concept ids (save in set) - all concept ids for approve
      // actions in set
      checklist.getStats().put("notStampedCt", conceptCt - approvedIds.size());

      // add all stats to checklist
      checklist.getStats().put("approveCt", approvedIds.size());
      checklist.getStats().put("relsInsertedCt", relsInsertedIds.size());
      checklist.getStats().put("stysInsertedCt", stysInsertedIds.size());
      checklist.getStats().put("splitsCt", splitsIds.size());
      checklist.getStats().put("mergeCt", mergeIds.size());

      // websocket - n/a
      // return the checklist
      workflowService.handleLazyInit(checklist);

      // websocket - n/a

      return checklist;

    } catch (Exception e) {
      handleException(e, "trying to get checklist stats");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @RequestMapping(value = "/log", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Path("/log")
  @Produces("text/plain")
  @Operation(summary = "Get log entries",
      description = "Returns log entries for specified query parameters")
  @Override
  public String getLog(
    @Parameter(description = "Project id, e.g. 5", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 5") @RequestParam(value = "checklistId", required = false) Long checklistId,
    @Parameter(description = "Worklist id, e.g. 5") @RequestParam(value = "worklistId", required = false) Long worklistId,
    @Parameter(description = "Lines, e.g. 5", required = true) @RequestParam(value = "lines", required = false, defaultValue = "0") int lines,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /log/" + projectId + ", "
        + checklistId + ", " + worklistId + ", " + lines);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken, "get log entries",
          UserRole.AUTHOR);

      // Precondition checking -- must have projectId and objectId set
      if (projectId == null) {
        throw new LocalException("Project id must be set");
      }

      final PfsParameter pfs = new PfsParameterJpa();
      pfs.setStartIndex(0);
      pfs.setMaxResults(lines);
      pfs.setAscending(false);
      pfs.setSortField("lastModified");

      // Find actions on this object OR where the activityId is the
      // worklist/checklist name
      String idClause = null;
      String activityClause = null;
      if (checklistId != null) {
        final Checklist checklist = workflowService.getChecklist(checklistId);
        if (checklist == null) {
          throw new Exception("Checklist for id does not exist " + checklistId);
        }
        idClause = "objectId:" + checklistId;
        activityClause = "activityId:" + checklist.getName();
      }
      if (worklistId != null) {
        final Worklist worklist = workflowService.getWorklist(worklistId);
        if (worklist == null) {
          throw new Exception("Worklist for id does not exist " + worklistId);
        }
        idClause = "objectId:" + worklistId;
        activityClause = "activityId:" + worklist.getName();
      }

      // Assemble query, projectID and either id or activity matches
      final String query = ConfigUtility.composeQuery("AND", "projectId:" + projectId,
          ConfigUtility.composeQuery("OR", idClause, activityClause));

      final List<LogEntry> entries = workflowService.findLogEntries(query, pfs);
      final StringBuilder log = new StringBuilder();
      for (int i = entries.size() - 1; i >= 0; i--) {
        final LogEntry entry = entries.get(i);
        final StringBuilder message = new StringBuilder();
        message.append("[").append(ConfigUtility.DATE_FORMAT4.format(entry.getLastModified()));
        message.append("] ");
        message.append(entry.getLastModifiedBy()).append(" ");
        message.append(entry.getMessage()).append("\r\n");
        log.append(message);
      }

      // websocket - n/a
      return log.toString();

    } catch (Exception e) {
      handleException(e, "trying to get log");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/{id}/clear", method = RequestMethod.POST)
  @POST
  @Path("/bin/{id}/clear")
  @Operation(summary = "Clear bin",
      description = "Clear bin")
  public void clearBin(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/" + id + "/clear ");

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to clear bin", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorkflowBin bin = workflowService.getWorkflowBin(id);
      verifyProject(bin, projectId);

      // remove bins and all of the tracking records in the bin
      workflowService.removeWorkflowBin(id, true);
      workflowService.addLogEntry(userName, projectId, id, null, null, "CLEAR BIN - " + id);

      // websocket - n/a
    } catch (Exception e) {
      handleException(e, "trying to clear bin");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/bin/{id}/regenerate", method = RequestMethod.POST)
  @POST
  @Path("/bin/{id}/regenerate")
  @Operation(summary = "Regenerate bin",
      description = "Regenerate bin")
  public WorkflowBin regenerateBin(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin id, e.g. 5", required = true) @PathVariable("id") Long id,
    @Parameter(description = "Workflow bin type", required = true) @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /bin/" + id + "/regenerate ");

    WorkflowBin bin = null;
    
    // Only one user can regenerate a bin at a time
    synchronized (LOCK) {

      final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
      try {
        final String userName = authorizeProject(workflowService, projectId, securityService,
            authToken, "trying to regenerate a single bin", UserRole.AUTHOR);
        workflowService.setLastModifiedBy(userName);

        // Set transaction scope
        workflowService.setTransactionPerOperation(false);
        workflowService.beginTransaction();

        // Read relevant workflow objects
        bin = workflowService.getWorkflowBin(id);
        verifyProject(bin, projectId);
        final Project project = workflowService.getProject(projectId);
        if (!project.isEditingEnabled()) {
          throw new LocalException("Editing is disabled on project: " + project.getName());
        }
        
        // start progress monitoring
        workflowService.startProcess(projectId, bin.getName());

        // Remove the workflow bin
        workflowService.removeWorkflowBin(id, true);

        // Get the bin definitions
        final List<WorkflowBinDefinition> definitions =
            workflowService.getWorkflowBinDefinitions(project, type);
        WorkflowBin newBin = null;
        for (final WorkflowBinDefinition definition : definitions) {
          if (definition.getName().equals(bin.getName())) {
            newBin = workflowService.regenerateBinHelper(project, definition, bin.getRank(),
                new HashSet<>(), workflowService.getConceptIdWorklistNameMap(project));
            break;
          }
        }

        workflowService.addLogEntry(userName, projectId, id, null, null,
            "REGENERATE BIN - " + id + ", " + bin.getName());
        workflowService.commit();

        // websocket - n/a
        
        // finish progress monitoring
        workflowService.finishProcess(projectId, bin.getName());

        return newBin;

      } catch (Exception e) {
        try {
          workflowService.rollback();
        } catch (Exception e2) {
          // n/a - if this fails, it's already rolled back
        }
        handleException(e, "trying to regenerate a single bin");
      } finally {
        if (bin != null) {
          workflowService.finishProcess(projectId, bin.getName());
        }
        workflowService.close();
        securityService.close();
      }
      return null;
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definition/regenerate", method = RequestMethod.POST)
  @POST
  @Path("/definition/regenerate")
  @Operation(summary = "Regenerate bin from definition",
      description = "Regenerate bin from definition.  Used for a defintion that does not yet have a bin")
  public WorkflowBin regenerateBinDefinition(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Workflow bin definition name, e.g. 'demotions'", required = true) @RequestParam(value = "name", required = false) String name,
    @Parameter(description = "Workflow bin type", required = true) @RequestParam(value = "type", required = false) String type,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /definition/regenerate " + name);

    // Only one user can regenerate a bin at a time
    synchronized (LOCK) {

      final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
      try {
        final String userName = authorizeProject(workflowService, projectId, securityService,
            authToken, "trying to regenerate a single bin", UserRole.AUTHOR);
        workflowService.setLastModifiedBy(userName);
        final Project project = workflowService.getProject(projectId);
        if (!project.isEditingEnabled()) {
          throw new LocalException("Editing is disabled on project: " + project.getName());
        }

        // Set transaction scope
        workflowService.setTransactionPerOperation(false);
        workflowService.beginTransaction();

        // Remove the bin by name if it exists (assume rank - if never created)
        int rank = 0;
        for (final WorkflowBin bin : workflowService.getWorkflowBins(project, type)) {
          if (bin.getName().equals(name)) {
            rank = bin.getRank();
            workflowService.removeWorkflowBin(bin.getId(), true);
          }
        }

        // Get the bin definitions
        final List<WorkflowBinDefinition> definitions =
            workflowService.getWorkflowBinDefinitions(project, type);
        WorkflowBin newBin = null;
        for (final WorkflowBinDefinition definition : definitions) {
          if (definition.getName().equals(name)) {
            newBin = workflowService.regenerateBinHelper(project, definition, rank, new HashSet<>(),
                workflowService.getConceptIdWorklistNameMap(project));
            break;
          }
        }
        if (newBin == null) {
          throw new LocalException("Unable to regenerate missing bin definition "
              + name + ", " + type);
        }

        workflowService.addLogEntry(userName, projectId, newBin.getId(), null, null,
            "REGENERATE BIN DEFINITION - " + name + ", " + type);
        workflowService.commit();

        // websocket - n/a

        return newBin;

      } catch (Exception e) {
        try {
          workflowService.rollback();
        } catch (Exception e2) {
          // n/a - if this fails, it's already rolled back
        }
        handleException(e, "trying to regenerate a single bin");
      } finally {
        workflowService.close();
        securityService.close();
      }
      return null;
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}/report/generate", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/worklist/{id}/report/generate")
  @Operation(summary = "Generate concept reports for worklist",
      description = "Generate concept reports for the specified worklist")
  public String generateConceptReport(
    @Parameter(description = "Project id, e.g. 5", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 5", required = true) @PathVariable("id") Long id,
    @Parameter(description = "Delay") @RequestParam(value = "delay", required = false) Long delay,
    @Parameter(description = "Send email, e.g. false") @RequestParam(value = "sendEmail", required = false) Boolean sendEmail,
    @Parameter(description = "Concept report type") @RequestParam(value = "conceptReportType", required = false) String conceptReportType,
    @Parameter(description = "Relationship count") @RequestParam(value = "relationshipCt", required = false) Integer relationshipCt,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /report/" + id + "/report/generate ");

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    final ReportServiceJpa reportService = new ReportServiceJpa();
    StringBuilder conceptReport = new StringBuilder();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to generate concept report", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);
      final Project project = workflowService.getProject(projectId);

      // Read vars
      final Worklist worklist = workflowService.getWorklist(id);
      final List<TrackingRecord> recordList = worklist.getTrackingRecords();

      // Construct filename
      final String fileName = worklist.getName() + "_rpt.txt";
      final String uploadDir = ConfigUtility.getUploadDir();
      final File reportsDir = new File(uploadDir + "/" + projectId + "/reports");
      final File file = new File(reportsDir, fileName);
      if (file.exists()) {
        throw new Exception("Worklist report file already exists - " + file.getAbsolutePath());
      }

      // Make dirs
      if (!reportsDir.exists()) {
        reportsDir.mkdirs();
      }

      // Handle delay
      if (delay != null) {
        Thread.sleep(delay);
      }

      // Generate the report
      for (final TrackingRecord record : recordList) {
        for (final Long conceptId : record.getOrigConceptIds()) {
          final Concept concept = reportService.getConcept(conceptId);

          final PrecedenceList list =
              sortAtoms(securityService, reportService, userName, concept, project);
          conceptReport.append(reportService.getConceptReport(project, concept, list, false));
          conceptReport.append("\r\n---------------------------------------------\r\n\r\n");
        }
      }

      try (final BufferedWriter out = Files.newBufferedWriter(file.toPath(),
          StandardCharsets.UTF_8)) {
        out.write(conceptReport.toString());
        out.flush();
      }

      // If sendEmail, handle sending email - to the email for the user who
      // requested the build
      if (sendEmail) {
        final User user = securityService.getUser(userName);
        final Properties config = PropertyUtility.getProperties();
        String from;
        if (config.containsKey("mail.smtp.from")) {
          from = config.getProperty("mail.smtp.from");
        } else {
          from = config.getProperty("mail.smtp.user");
        }
        ConfigUtility.sendEmail("[Terminology Server] Worklist Concept Report " + fileName, from,
            user.getEmail(),
            "The worklist concept report " + fileName + " has been successfully generated.",
            config);
      }

      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "GENERATE REPORT for worklist - " + worklist.getId() + ", " + worklist.getName());

      // websocket - n/a

      return fileName;
    } catch (Exception e) {
      handleException(e, "trying to generate concept report");
    } finally {
      reportService.close();
      workflowService.close();
      securityService.close();
    }
    return "";
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/report", method = RequestMethod.POST)
  @POST
  @Path("/report")
  @Operation(summary = "Find concept reports",
      description = "Find generated concept reports")
  public StringList findGeneratedConceptReports(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query") @RequestParam(value = "query", required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /report " + projectId + ", " + query);

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    StringList stringList = new StringList();
    List<String> matchingFiles = new ArrayList<>();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to find concept report", UserRole.AUTHOR);
      final String uploadDir = ConfigUtility.getUploadDir();
      final String filePath = uploadDir + "/" + projectId + "/reports";
      final File dir = new File(filePath);
      if (!dir.exists()) {
        Logger.getLogger(getClass()).info("  create path = " + filePath);
        dir.mkdirs();
      }
      int i = 0;
      final String[] files = dir.list();
      if (files != null) {
        for (final String file : files) {
          i++;
          if (ConfigUtility.isEmpty(query) || file.contains(query)) {
            matchingFiles.add(file);
          }
        }
      }
      Collections.sort(matchingFiles);
      if (pfs == null || pfs.getStartIndex() == -1) {
        stringList.setObjects(matchingFiles);
      } else {
        // Or get a substring
        stringList.setObjects(matchingFiles.subList(pfs.getStartIndex(),
            Math.min((pfs.getStartIndex() + pfs.getMaxResults()), matchingFiles.size())));
      }
      stringList.setTotalCount(i);

      // websocket - n/a

      return stringList;

    } catch (Exception e) {
      handleException(e, e.getMessage() + ". Trying to find generated concept reports.");
    } finally {

      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/report/{fileName}", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/report/{fileName}")
  @Operation(summary = "Get generated concept report",
      description = "Get generated concept report")
  public String getGeneratedConceptReport(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "File name") @PathVariable("fileName") String fileName,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /report/" + fileName);

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to get generated concept report", UserRole.AUTHOR);
      final String uploadDir = ConfigUtility.getUploadDir();
      final String filePath = uploadDir + "/" + projectId + "/reports/" + fileName;
      final File file = new File(filePath);
      if (!file.exists()) {
        throw new LocalException("No report exists for path " + filePath);
      }
      // Return file contents

      // websocket - n/a
      return FileUtils.readFileToString(file, "UTF-8");

    } catch (Exception e) {
      handleException(e, e.getMessage() + ". Trying to find generated concept report.");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/report/{fileName}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/report/{fileName}")
  @Operation(summary = "Get generated concept report",
      description = "Get generated concept report")
  public void removeGeneratedConceptReport(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "File name") @PathVariable("fileName") String fileName,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /report/" + fileName);
    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();

    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "trying to remove generated concept report", UserRole.AUTHOR);
      final String uploadDir = ConfigUtility.getUploadDir();
      final String filePath = uploadDir + "/" + projectId + "/reports/" + fileName;
      FileUtils.forceDelete(new File(filePath));
      workflowService.addLogEntry(userName, projectId, null, null, null,
          "REMOVE REPORT - " + fileName);

      // websocket - n/a
    } catch (Exception e) {
      handleException(e, e.getMessage() + ". Trying to remove generated concept report.");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/query/test", method = RequestMethod.GET)
  @GET
  @Path("/query/test")
  @Operation(summary = "Test query.",
      description = "Test workflow bin definition query.")
  public SearchResultList testQuery(
    @Parameter(description = "Project id, e.g. 5") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query, e.g. NOT workflowStatus:NEEDS_REVIEW", required = true) @RequestParam(value = "query", required = false) String query,
    @Parameter(description = "Query type, e.g. LUCENE", required = true) @RequestParam(value = "queryType", required = false) QueryType queryType,
    @Parameter(description = "Query style, e.g. CLUSTER", required = true) @RequestParam(value = "queryStyle", required = false) QueryStyle queryStyle,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /query/test ");

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "trying to test query", UserRole.AUTHOR);

	  // start progress monitoring
      workflowService.startProcess(projectId, "test-query-" + authToken);
      
      final Project project = workflowService.getProject(projectId);
      if (query == null) {
        throw new LocalException("Unexpected null query");
      }
      if (query.endsWith(";")) {
        throw new LocalException("Remove semi-colon character from end of query");
      }

      List<Long[]> results = new ArrayList<>();
      int ct = 0;
      if (queryStyle == QueryStyle.CLUSTER) {
        results = workflowService.executeClusteredConceptQuery(query, queryType,
            workflowService.getDefaultQueryParams(project), true);
        ct = workflowService.executeClusteredConceptQueryCt(query, queryType,
            workflowService.getDefaultQueryParams(project));
      }

      else if (queryStyle == QueryStyle.REPORT) {
        workflowService.executeReportQuery(query, queryType,
            workflowService.getDefaultQueryParams(project), true);
      }

      else if (queryStyle == QueryStyle.OTHER) {
        workflowService.executeQuery(query, queryType,
            workflowService.getDefaultQueryParams(project), true);
      } else {
        throw new LocalException("Unexpected query style = " + queryStyle);
      }

      SearchResultList searchResultList = new SearchResultListJpa();
      List<SearchResult> srl = new ArrayList<>();
      for (Long[] result : results) {
        SearchResult sr = new SearchResultJpa();
        if (result.length > 1) {
          sr.setValue(result[0].toString() + ", " + result[1].toString());
        } else {
          sr.setValue(result[0].toString());
        }
        srl.add(sr);
      }
      searchResultList.setObjects(srl);
      searchResultList.setTotalCount(ct);
      
      // finish progress monitoring
      workflowService.finishProcess(projectId, "test-query-" + authToken);
      
      return searchResultList;

      // websocket - n/a
    } catch (Exception e) {
      handleException(e, "trying to test query");
    } finally {
      workflowService.finishProcess(projectId, "test-query-" + authToken);
      
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/{id}/note", method = RequestMethod.PUT)
  @PUT
  @Path("/checklist/{id}/note")
  @Consumes("text/plain")
  @Operation(summary = "Add checklist note",
      description = "Adds a checklist note")
  public Note addChecklistNote(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 3", required = true) @PathVariable("id") Long checklistId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The note, e.g. \"this is a sample note\"", required = true) @RequestBody String note,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /checklist/" + checklistId + "/note " + note);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "adding checklist note", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final Checklist checklist = workflowService.getChecklist(checklistId);
      verifyProject(checklist, projectId);

      final Note checklistNote = new ChecklistNoteJpa();
      checklistNote.setLastModifiedBy(userName);
      checklistNote.setNote(note);
      ((ChecklistNoteJpa) checklistNote).setChecklist(checklist);

      // Add and return the note
      final Note newNote = workflowService.addNote(checklistNote);
      workflowService.addLogEntry(userName, projectId, checklist.getId(), null, null,
          "ADD checklist note - " + checklist.getId() + ", " + checklist.getName() + ", " + note);

      // For indexing
      checklist.getNotes().add(newNote);
      workflowService.updateChecklist(checklist);
      workflowService.addLogEntry(userName, projectId, checklist.getId(), null, null,
          "UPDATE checklist - " + checklist.getId() + ", " + checklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("AddChecklistNote", authToken, "CHECKLIST",
          checklist.getId(), getProjectInfo(checklist.getProject()));
      sendChangeEvent(userName, event);

      return newNote;
    } catch (Exception e) {
      handleException(e, "trying to add note");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}/note", method = RequestMethod.PUT)
  @PUT
  @Path("/worklist/{id}/note")
  @Consumes("text/plain")
  @Operation(summary = "Add worklist note",
      description = "Adds a worklist note")
  public Note addWorklistNote(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 3", required = true) @PathVariable("id") Long worklistId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The note, e.g. \"this is a sample note\"", required = true) @RequestBody String note,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /worklist/" + worklistId + "/note " + note);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "adding worklist note", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final Worklist worklist = workflowService.getWorklist(worklistId);
      if (worklist == null) {
        throw new Exception("Invalid worklist id " + worklistId);
      }

      final Note worklistNote = new WorklistNoteJpa();
      worklistNote.setLastModifiedBy(userName);
      worklistNote.setNote(note);
      ((WorklistNoteJpa) worklistNote).setWorklist(worklist);

      // Add and return the note
      final Note newNote = workflowService.addNote(worklistNote);
      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "UPDATE worklist - " + worklist.getId() + ", " + worklist.getName());

      // For indexing
      worklist.getNotes().add(newNote);
      workflowService.updateWorklist(worklist);
      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "UPDATE worklist - " + worklist.getId() + ", " + worklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("AddWorklistNote", authToken, "WORKLIST",
          worklist.getId(), getProjectInfo(worklist.getProject()));
      sendChangeEvent(userName, event);

      return newNote;
    } catch (Exception e) {
      handleException(e, "trying to add note");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/note/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/checklist/note/{id}")
  @Operation(summary = "Remove checklist note",
      description = "Removes the specified checklist note")
  public void removeChecklistNote(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Note id, e.g. 3", required = true) @PathVariable("id") Long noteId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/note/" + noteId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove checklist note", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final ChecklistNoteJpa note =
          (ChecklistNoteJpa) workflowService.getNote(noteId, ChecklistNoteJpa.class);
      final Checklist checklist = note.getChecklist();
      verifyProject(checklist, projectId);

      if (!checklist.getProject().getId().equals(projectId)) {
        throw new Exception("Attempt to remove a note from a different project.");
      }

      // remove note
      workflowService.removeNote(noteId, ChecklistNoteJpa.class);
      workflowService.addLogEntry(userName, projectId, checklist.getId(), null, null,
          "REMOVE checklist note - " + checklist.getId() + ", " + checklist.getName() + ", "
              + note.getNote());

      // For indexing
      checklist.getNotes().remove(note);
      workflowService.updateChecklist(checklist);
      workflowService.addLogEntry(userName, projectId, checklist.getId(), null, null,
          "UPDATE checklist - " + checklist.getId() + ", " + checklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveChecklistNote", authToken, "CHECKLIST",
          checklist.getId(), getProjectInfo(checklist.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a checklist note");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/note/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/worklist/note/{id}")
  @Operation(summary = "Remove worklist note",
      description = "Removes the specified worklist note")
  public void removeWorklistNote(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Note id, e.g. 3", required = true) @PathVariable("id") Long noteId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/note/" + noteId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "remove worklist note", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      final WorklistNoteJpa note =
          (WorklistNoteJpa) workflowService.getNote(noteId, WorklistNoteJpa.class);
      final Worklist worklist = note.getWorklist();
      verifyProject(worklist, projectId);

      if (!worklist.getProject().getId().equals(projectId)) {
        throw new Exception("Attempt to remove a note from a different project.");
      }

      // remove note
      workflowService.removeNote(noteId, WorklistNoteJpa.class);
      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "REMOVE worklist note - " + worklist.getId() + ", " + worklist.getName() + ", "
              + note.getNote());
      // For indexing
      worklist.getNotes().remove(note);
      workflowService.updateWorklist(worklist);
      workflowService.addLogEntry(userName, projectId, worklist.getId(), null, null,
          "UPDATE worklist - " + worklist.getId() + ", " + worklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("RemoveWorklistNote", authToken, "WORKLIST",
          worklist.getId(), getProjectInfo(worklist.getProject()));
      sendChangeEvent(userName, event);

    } catch (Exception e) {
      handleException(e, "trying to remove a worklist note");
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /**
   * Returns the project info.
   *
   * @param project the project
   * @return the project info
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private ComponentInfo getProjectInfo(Project project) throws Exception {
    return new ComponentInfoJpa(project.getId(), project.getTerminology(), null, null,
        project.getName(), IdType.PROJECT);
  }

  /* see superclass */
  @RequestMapping(value = "/checklist/import", method = RequestMethod.POST)
  @POST
  @Path("/checklist/import")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Import checklist",
      description = "Imports a checklist in the standard format")
  public Checklist importChecklist(
    @Parameter(description = "Content of members file", required = true) @RequestParam("file") MultipartFile file,
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist name, e.g. chk_test") @RequestParam(value = "name", required = false) String name,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    try (InputStream in = file.getInputStream()) {
      return importChecklist(in, projectId, name, authToken);
    }
  }

  /* see superclass */
  @Override
  public Checklist importChecklist(FormDataContentDisposition contentDispositionHeader,
    InputStream in, Long projectId, String name, String authToken) throws Exception {
    return importChecklist(in, projectId, name, authToken);
  }

  /**
   * Imports a checklist from a stream.
   *
   * @param in the input stream
   * @param projectId the project id
   * @param name the checklist name
   * @param authToken the auth token
   * @return the checklist
   * @throws Exception the exception
   */
  private Checklist importChecklist(InputStream in, Long projectId, String name, String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Workflow): /checklist/import " + projectId + ", " + name);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    workflowService.setTransactionPerOperation(false);
    workflowService.beginTransaction();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "import checklist", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);

      // Read input stream
      final BufferedReader reader = new BufferedReader(
          new InputStreamReader(in, StandardCharsets.UTF_8));
      String line;
      final Map<Long, List<String>> entries = new HashMap<>();
      while ((line = reader.readLine()) != null) {
        // Verify format
        final String[] tokens = FieldedStringTokenizer.split(line, "\t");
        // skip header
        if (tokens[0].toLowerCase().contains("cluster")) {
          continue;
        }
        if (tokens.length != 2 && tokens.length != 3) {
          throw new LocalException("Imported checklist has wrong number of fields: " + line);
        }

        if (!tokens[0].matches("[0-9]*")) {
          throw new LocalException("Imported checklist has bad clusterId: " + line);
        }
        if (!tokens[1].matches("[0-9]*")) {
          throw new LocalException("Imported checklist has bad conceptId: " + line);
        }

        final Long clusterId = Long.valueOf(tokens[0]);
        if (!entries.containsKey(clusterId)) {
          entries.put(clusterId, new ArrayList<>(3));
        }
        entries.get(clusterId).add(line);
      }

      final Project project = workflowService.getProject(projectId);

      // Add checklist
      final Checklist checklist = new ChecklistJpa();
      checklist.setName(name);
      checklist.setDescription(name + " description");
      checklist.setProject(project);
      checklist.setTimestamp(new Date());

      // Add tracking records
      long i = 1L;
      for (final Long clusterId : entries.keySet()) {
        final TrackingRecord record = new TrackingRecordJpa();
        record.setChecklistName(name);
        // recluster from 1
        record.setClusterId(i++);
        record.setClusterType("");
        record.setProject(project);
        record.setTerminology(project.getTerminology());
        record.setTimestamp(new Date());
        record.setVersion(workflowService.getLatestVersion(project.getTerminology()));
        final StringBuilder sb = new StringBuilder();
        for (final String entry : entries.get(clusterId)) {
          final String[] tokens = FieldedStringTokenizer.split(entry, "\t");
          final Concept concept = workflowService.getConcept(Long.valueOf(tokens[1]));
          record.getComponentIds()
              .addAll(concept.getAtoms().stream().map(a -> a.getId()).collect(Collectors.toSet()));
          record.getOrigConceptIds().add(concept.getId());
          sb.append(concept.getName()).append(" ");
        }
        record.setIndexedData(sb.toString());
        workflowService.computeTrackingRecordStatus(record, true);
        final TrackingRecord newRecord = workflowService.addTrackingRecord(record);
        // Add the record to the checklist.
        checklist.getTrackingRecords().add(newRecord);
      }

      // Add the checklist
      final Checklist newChecklist = workflowService.addChecklist(checklist);

      // End transaction
      workflowService.addLogEntry(userName, projectId, checklist.getId(), null, null,
          "IMPORT checklist - " + checklist.getId() + ", " + checklist.getName());

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("ImportChecklist", authToken, "CHECKLIST",
          newChecklist.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

      workflowService.commit();

      return newChecklist;
    } catch (Exception e) {
      handleException(e, "trying to import checklist");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @RequestMapping(value = "/checklist/compute", method = RequestMethod.POST)
  @POST
  @Override
  @Path("/checklist/compute")
  @Operation(summary = "Compute checklist",
      description = "Computes a checklist from a query")
  public Checklist computeChecklist(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query, e.g. NOT workflowStatus:NEEDS_REVIEW", required = true) @RequestParam(value = "query", required = false) String query,
    @Parameter(description = "Query type, e.g. LUCENE", required = true) @RequestParam(value = "queryType", required = false) QueryType queryType,
    @Parameter(description = "Checklist name, e.g. chk_test") @RequestParam(value = "name", required = false) String name,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Workflow): /checklist/compute " + projectId + ", " + name + ", " + query);

    final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
    try {
      final String userName = authorizeProject(workflowService, projectId, securityService,
          authToken, "compute checklist", UserRole.AUTHOR);
      workflowService.setLastModifiedBy(userName);
      final Project project = workflowService.getProject(projectId);

      workflowService.setTransactionPerOperation(false);
      workflowService.beginTransaction();

      Checklist newChecklist =
          workflowService.computeChecklist(project, query, queryType, name, pfs, false);

      workflowService.commit();

      workflowService.addLogEntry(userName, projectId, newChecklist.getId(), null, null,
          "COMPUTE checklist - " + newChecklist.getId() + ", " + newChecklist.getName() + ", "
              + query);

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("ComputeChecklist", authToken, "CHECKLIST",
          newChecklist.getId(), getProjectInfo(project));
      sendChangeEvent(userName, event);

      return newChecklist;
    } catch (Exception e) {
      handleException(e, "trying to compute checklist");
      return null;
    } finally {
      workflowService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @RequestMapping(value = "/checklist/{id}/export", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = "Export checklist",
      description = "Exports the checklist")
  public byte[] exportChecklistResponse(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    try (InputStream in = exportChecklist(projectId, id, authToken)) {
      return in == null ? new byte[0] : in.readAllBytes();
    }
  }

  /* see superclass */
  @GET
  @Override
  @Produces("application/octet-stream")
  @Path("/checklist/{id}/export")
  public InputStream exportChecklist(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/" + id + "/export");

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken, "export checklist",
          UserRole.AUTHOR);

      final Checklist checklist = workflowService.getChecklist(id);
      verifyProject(checklist, projectId);

      // websocket - n/a

      return exportList(checklist.getTrackingRecords(), workflowService);

    } catch (Exception e) {
      handleException(e, "trying to export checklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @RequestMapping(value = "/worklist/{id}/export", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @Operation(summary = "Export worklist",
      description = "Exports the worklist")
  public byte[] exportWorklistResponse(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    try (InputStream in = exportWorklist(projectId, id, authToken)) {
      return in == null ? new byte[0] : in.readAllBytes();
    }
  }

  /* see superclass */
  @GET
  @Override
  @Produces("application/octet-stream")
  @Path("/worklist/{id}/export")
  public InputStream exportWorklist(
    @Parameter(description = "Project id, e.g. 3", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + id + "/export");
    // identical to prior method but for worklists.
    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken, "export worklist",
          UserRole.AUTHOR);

      final Worklist worklist = workflowService.getWorklist(id);
      verifyProject(worklist, projectId);

      // websocket - n/a

      return exportList(worklist.getTrackingRecords(), workflowService);

    } catch (Exception e) {
      handleException(e, "trying to export worklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /**
   * Export list.
   *
   * @param records the records
   * @param workflowService the workflow service
   * @return the input stream
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private InputStream exportList(List<TrackingRecord> records, WorkflowService workflowService)
    throws Exception {
    // Write a header
    // Obtain members for refset,
    // Write RF2 simple refset pattern to a StringBuilder
    // wrap and return the string for that as an input stream
    StringBuilder sb = new StringBuilder();
    sb.append("clusterId").append("\t");
    sb.append("conceptId").append("\t");
    sb.append("conceptName").append("\r\n");

    for (final TrackingRecord record : records) {
      workflowService.lookupTrackingRecordConcepts(record);
      for (final Concept concept : record.getConcepts()) {
        sb.append(record.getClusterId()).append("\t");
        sb.append(concept.getId()).append("\t");
        sb.append(concept.getName()).append("\r\n");
      }
    }

    return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}/stamp", method = RequestMethod.POST)
  @POST
  @Path("/worklist/{id}/stamp")
  @Operation(summary = "Stamp worklist",
      description = "Approve all concepts on worklist")
  public void stampWorklist(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Worklist id, e.g. 2", required = true) @PathVariable("id") Long id,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Approve") @RequestParam(value = "approve", required = false, defaultValue = "false") boolean approve,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /worklist/" + id + "/stamp "
        + projectId + ", " + activityId + ", " + approve);

    // Instantiate services
    final ProcessService processService = new ProcessServiceJpa();
    final StampingAlgorithm algorithm = new StampingAlgorithm();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(algorithm, projectId, securityService, authToken,
          "stamping worklist", UserRole.AUTHOR);
      final Project project = algorithm.getProject(projectId);

      // Set up and run the algorithm
      algorithm.setActivityId(activityId);
      algorithm.setLastModifiedBy("S-" + userName);
      algorithm.setWorklistId(id);
      algorithm.setApprove(approve);

      processService.executeSingleAlgorithm(algorithm, project);

    } catch (Exception e) {
      try {
        algorithm.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "stamping worklist");
    } finally {
      algorithm.close();
      processService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/checklist/{id}/stamp", method = RequestMethod.POST)
  @POST
  @Path("/checklist/{id}/stamp")
  @Operation(summary = "Stamp checklist",
      description = "Approve all concepts on checklist")
  public void stampChecklist(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Checklist id, e.g. 2", required = true) @PathVariable("id") Long id,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Approve") @RequestParam(value = "approve", required = false, defaultValue = "false") boolean approve,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /checklist/" + id + "/stamp "
        + projectId + ", " + activityId + ", " + approve);

    // Instantiate services
    final ProcessService processService = new ProcessServiceJpa();
    final StampingAlgorithm algorithm = new StampingAlgorithm();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(algorithm, projectId, securityService, authToken,
          "stamping checklist", UserRole.AUTHOR);
      final Project project = algorithm.getProject(projectId);

      // Set up and run the algorithm
      algorithm.setActivityId(activityId);
      algorithm.setLastModifiedBy("S-" + userName);
      algorithm.setChecklistId(id);
      algorithm.setApprove(approve);

      processService.executeSingleAlgorithm(algorithm, project);

    } catch (Exception e) {
      try {
        algorithm.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "stamping checklist");
    } finally {
      algorithm.close();
      processService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/status/compute", method = RequestMethod.POST)
  @POST
  @Path("/status/compute")
  @Operation(summary = "Recompute concept status",
      description = "Recompute concept status")
  public void recomputeConceptStatus(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Activity id, e.g. MATRIXINIT", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Update flag, e.g. false") @RequestParam(value = "update", required = false) Boolean updateFlag,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Workflow): /status/compute " + projectId + ", "
        + activityId + ", " + updateFlag);

    // Instantiate services
    final ProcessService processService = new ProcessServiceJpa();
    final MatrixInitializerAlgorithm algorithm = new MatrixInitializerAlgorithm();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(algorithm, projectId, securityService, authToken,
          "compute concept status", UserRole.AUTHOR);
      final Project project = algorithm.getProject(projectId);

      // Set up and run the algorithm
      algorithm.setActivityId(activityId);
      algorithm.setLastModifiedBy(userName);

      if (updateFlag != null && updateFlag) {
        final PfsParameter pfs = new PfsParameterJpa();
        pfs.setSortField("lastModified");
        pfs.setAscending(false);
        pfs.setStartIndex(0);
        pfs.setMaxResults(1);
        final List<LogEntry> list =
            algorithm.findLogEntries("message:\"Finished Matrix Initializer Algorithm\"", pfs);
        if (list.size() > 0) {
          final Date lastMatrixinit = list.get(0).getLastModified();
          // find project concepts touched since then\
          final jakarta.persistence.Query query = algorithm.getEntityManager()
              .createQuery("select c.id from ConceptJpa c " + "where terminology = :terminology "
                  + "  and version = :version and lastModified > :date");
          query.setParameter("terminology", project.getTerminology());
          query.setParameter("version", project.getVersion());
          query.setParameter("date", lastMatrixinit);
          final List<?> results = query.getResultList();
          final Set<Long> conceptIds =
              results.stream().map(o -> Long.valueOf(o.toString())).collect(Collectors.toSet());
          if (conceptIds.size() == 0) {
            // bail, no algorithm
            throw new LocalException(
                "Update mode used and no concepts have changed since last run");
          }
          algorithm.setConceptIds(conceptIds);
        }
      }

      processService.executeSingleAlgorithm(algorithm, project);

    } catch (

    Exception e) {
      try {
        algorithm.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "compute concept status");
    } finally {
      processService.close();
      algorithm.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "runautofix", method = RequestMethod.POST)
  @POST
  @Path("runautofix")
  @Operation(summary = "Autofix bin",
      description = "Autofix bin")
  public void runAutofix(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Workflow bin to run autofix on", required = true) @RequestBody WorkflowBinJpa workflowBin,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Workflow): /runautofix ");

    // Only one user can autofix a bin at a time
    synchronized (LOCK) {

      final WorkflowServiceJpa workflowService = new WorkflowServiceJpa();
      final ProcessServiceJpa processService = new ProcessServiceJpa();
      processService.setLastModifiedBy(authToken);

      try {
        final String userName = authorizeProject(workflowService, projectId, securityService,
            authToken, "trying to autofix a bin", UserRole.AUTHOR);
        workflowService.setLastModifiedBy(userName);

        // Read relevant workflow objects
        final Project project = workflowService.getProject(projectId);

        if (!project.isEditingEnabled()) {
          throw new LocalException("Editing is disabled on project: " + project.getName());
        }

        // Create autofix process
        ProcessConfig processConfig = new ProcessConfigJpa();
        processConfig.setDescription("Autofix Process for '" + workflowBin.getName() + "' - "
            + ConfigUtility.DATE_YYYYMMDDHHMMSS.format(new Date()));
        processConfig.setFeedbackEmail(null);
        processConfig.setName("Autofix Process for '" + workflowBin.getName() + "' - "
            + ConfigUtility.DATE_YYYYMMDDHHMMSS.format(new Date()));
        processConfig.setProject(project);
        processConfig.setTerminology("");
        processConfig.setVersion("");
        processConfig.setTimestamp(new Date());
        processConfig.setType("Autofix");
        processConfig.setInputPath("");
        processConfig = processService.addProcessConfig(processConfig);

        // Create autofix algorithm
        AlgorithmConfig algoConfig = new AlgorithmConfigJpa();
        algoConfig.setAlgorithmKey(workflowBin.getAutofix());
        algoConfig.setDescription("Autofix Algorithm: " + workflowBin.getAutofix());
        algoConfig.setEnabled(true);
        algoConfig.setName("Autofix Algorithm: " + workflowBin.getAutofix());
        algoConfig.setProcess(processConfig);
        algoConfig.setProject(project);
        algoConfig.setTimestamp(new Date());
        // Add algorithm and insert as step into process
        algoConfig = processService.addAlgorithmConfig(algoConfig);

        processConfig.getSteps().add(algoConfig);
        processService.updateProcessConfig(processConfig);

        // TODO Execute algorithm? Or go to process page so they can execute
        // algorithm themselves?

        workflowService.addLogEntry(userName, projectId, null, null, null,
            "AUTOFIX BIN - " + workflowBin.getName());

        // websocket - n/a

        return;

      } catch (Exception e) {
        try {
          workflowService.rollback();
        } catch (Exception e2) {
          // n/a - if this fails, it's already rolled back
        }
        handleException(e, "trying to autofix a bin");
      } finally {
        workflowService.close();
        processService.close();
        securityService.close();
      }
      return;
    }
  }
  
  /* see superclass */
  @Override
  @RequestMapping(value = "/lookup/progress", method = RequestMethod.POST)
  @POST
  @Path("/lookup/progress")
  @Operation(summary = "Lookup progress through process",
      description = "Returns whether the process is still in progress")
  public Boolean getProcessProgress(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Project id", required = true) @RequestBody Long projectId,
    @Parameter(description = "Process, e.g. BETA", required = true) @RequestParam(value = "process", required = false) String process,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call GET (Workflow): /lookup/process " + projectId + ", "
            + process);

    final WorkflowService releaseService = new WorkflowServiceJpa();

    Boolean processStillInProgress = false;

    try {
      if (releaseService.getProcessProgressStatus(projectId, process)) {
        processStillInProgress = true;
      }

      return processStillInProgress;
    } catch (Exception e) {
      handleException(e, "trying to find the process status");
    } finally {
      releaseService.close();
      securityService.close();
    }
    return null;
  }
  
  /* see superclass */
  @Override
  @RequestMapping(value = "/process/results/{projectId}", method = RequestMethod.GET)
  @GET
  @Path("/process/results/{projectId}")
  @Operation(summary = "Get process results",
      description = "Returns the validation result of a completed process")
  public ValidationResult getProcessResults(
    @Parameter(description = "Project id, e.g. 2", required = true) @PathVariable("projectId") Long projectId,
    @Parameter(description = "Bulk Process, e.g. BETA", required = true) @RequestParam(value = "process", required = false) String process,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call GET (Refset): /process/results/" + projectId + ", "
            + process);

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      ValidationResult validationResult =
          workflowService.getProcessValidationResult(projectId, process);

      if (validationResult == null) {
        throw new LocalException("No validation result found for project="
            + projectId + ", process=" + process);
      }

      // Now that we've gotten the result, clear it out so a future process run
      // can use the same key
      workflowService.removeProcessValidationResult(projectId, process);

      return validationResult;
    } catch (Exception e) {
      handleException(e,
          "trying to find the validation results for a completed process");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }
  
  /* see superclass */
  @Override
  @RequestMapping(value = "/lookup/progress/bulk", method = RequestMethod.POST)
  @POST
  @Path("/lookup/progress/bulk")
  @Operation(summary = "Lookup progress through bulk process",
      description = "Returns the refsetIds that are still in progress for specified bulk process")
  public StringList getBulkProcessProgress(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of workflow bins", required = true) @RequestBody String[] binNames,
    @Parameter(description = "Project id", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call POST (Workflow): /lookup/process/bulk "
            + binNames.toString() + ", " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();

    StringList binsStillInProgress = new StringList();

    try {
      for (String binName : binNames) {
        if (workflowService.getProcessProgressStatus(projectId, binName)) {
          binsStillInProgress.getObjects().add(binName);
        }
      }

		binsStillInProgress.setTotalCount(
				binsStillInProgress.getObjects() != null ? binsStillInProgress.getObjects().size() : 0);
		return binsStillInProgress;
    } catch (Exception e) {
      handleException(e, "trying to find the bulk process status for workflow bins");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/process/results/bulk/{projectId}", method = RequestMethod.GET)
  @GET
  @Path("/process/results/bulk/{projectId}")
  @Operation(summary = "Get bulk process results",
      description = "Returns the validation results of a completed bulk process")
  public ValidationResult getBulkProcessResults(
    @Parameter(description = "Project id, e.g. 2", required = true) @PathVariable("projectId") Long projectId,
    @Parameter(description = "Bulk Process, e.g. BETA", required = true) @RequestParam(value = "process", required = false) String process,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call GET (Refset): /process/results/bulk/" + projectId
            + ", " + process);

    final WorkflowService workflowService = new WorkflowServiceJpa();

    try {
      ValidationResult validationResult =
          workflowService.getProcessValidationResult(projectId, process);

      if (validationResult == null) {
        throw new LocalException("No validation result found for project="
            + projectId + ", process=" + process);
      }

      // Now that we've gotten the result, clear it out so a future process run
      // can use the same key
      workflowService.removeProcessValidationResult(projectId, process);

      return validationResult;
    } catch (Exception e) {
      handleException(e,
          "trying to find the validation results for a completed bulk process");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }
}
