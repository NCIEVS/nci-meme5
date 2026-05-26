/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.QueryStyle;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.helpers.WorkflowBinDefinitionList;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.model.helpers.WorkflowBinDefinitionListJpa;
import com.wci.umls.server.jpa.services.ReportServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.services.rest.ReportServiceRest;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.Descriptor;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.report.Report;
import com.wci.umls.server.model.report.ReportList;
import com.wci.umls.server.model.workflow.WorkflowBinDefinition;
import com.wci.umls.server.model.workflow.WorkflowConfig;
import com.wci.umls.server.services.ReportService;
import com.wci.umls.server.services.SecurityService;
import com.wci.umls.server.services.WorkflowService;

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
 * REST implementation for {@link ReportServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/report")
@Path("/report")
@Tag(name = "Report", description = "Operations for reporting.")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ReportServiceRestImpl extends RootServiceRestImpl
    implements ReportServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link ReportServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ReportServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/{id}", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/concept/{id}")
  @Operation(summary = "Get concept report",
      description = "Gets a concept report")
  public String getConceptReport(
    @Parameter(description = "Project id, e.g. UMLS") @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. UMLS", required = true) @PathVariable("id") Long conceptId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Report): /report " + projectId);

    final ReportService reportService = new ReportServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "get concept report", UserRole.VIEWER);

      final Concept concept = reportService.getConcept(conceptId);
      final Project project =
          projectId == null ? null : reportService.getProject(projectId);

      // Sort atoms
      if (concept != null) {
//        reportService.getGraphResolutionHandler(concept.getTerminology())
//            .resolve(concept);
        final PrecedenceList list = sortAtoms(securityService, reportService,
            userName, concept, project);
        return reportService.getConceptReport(project, concept, list, true);
      }
      return "MISSING CONCEPT";

    } catch (Exception e) {
      handleException(e, "trying to get concept report");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/descriptor/{id}", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/descriptor/{id}")
  @Operation(summary = "Get descriptor report",
      description = "Gets a descriptor report")
  public String getDescriptorReport(
    @Parameter(description = "Project id, e.g. UMLS", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Descriptor id, e.g. UMLS", required = true) @PathVariable("id") Long descriptorId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Report): /report");

    final ReportService reportService = new ReportServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "get descriptor report", UserRole.VIEWER);

      final Descriptor descriptor = reportService.getDescriptor(descriptorId);
      final Project project =
          projectId == null ? null : reportService.getProject(projectId);

      // Sort atoms
      if (descriptor != null) {
        reportService.getGraphResolutionHandler(descriptor.getTerminology())
            .resolve(descriptor);
        final PrecedenceList list = sortAtoms(securityService, reportService,
            userName, descriptor, project);
        return reportService.getDescriptorReport(project, descriptor, list,
            true);
      }
      return "MISSING DESCRIPTOR";

    } catch (Exception e) {
      handleException(e, "trying to get descriptor report");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/code/{id}", method = RequestMethod.GET,
      produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/code/{id}")
  @Operation(summary = "Get code report",
      description = "Gets a code report")
  public String getCodeReport(
    @Parameter(description = "Project id, e.g. UMLS", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Code id, e.g. UMLS", required = true) @PathVariable("id") Long codeId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Report): /report");

    final ReportService reportService = new ReportServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "get code report", UserRole.VIEWER);

      final Code code = reportService.getCode(codeId);
      final Project project =
          projectId == null ? null : reportService.getProject(projectId);

      // Sort atoms
      if (code != null) {
        reportService.getGraphResolutionHandler(code.getTerminology())
            .resolve(code);
        final PrecedenceList list =
            sortAtoms(securityService, reportService, userName, code, project);
        return reportService.getCodeReport(project, code, list, true);
      }
      return "MISSING CODE";

    } catch (Exception e) {
      handleException(e, "trying to get code report");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/definitions", method = RequestMethod.GET)
  @GET
  @Path("/definitions")
  @Operation(summary = "Find report definitions",
      description = "Find report definitions")
  public WorkflowBinDefinitionList findReportDefinitions(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Report): /definitions" + " " + projectId);

    final WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeProject(workflowService, projectId, securityService, authToken,
          "get report definitions", UserRole.AUTHOR);

      Project project = workflowService.getProject(projectId);
      final List<WorkflowConfig> configs =
          workflowService.getWorkflowConfigs(project);

      List<WorkflowBinDefinition> reportDefinitions = new ArrayList<>();
      for (WorkflowConfig config : configs) {
        if (config.getQueryStyle() == QueryStyle.REPORT) {
          reportDefinitions.addAll(config.getWorkflowBinDefinitions());
        }
      }

      for (WorkflowBinDefinition definition : reportDefinitions) {
        if (definition != null) {
          verifyProject(definition.getWorkflowConfig(), projectId);
          workflowService.handleLazyInit(definition);
        }
      }
      // websocket - n/a

      WorkflowBinDefinitionList list = new WorkflowBinDefinitionListJpa();
      list.setObjects(reportDefinitions);
      list.setTotalCount(reportDefinitions.size());
      return list;

    } catch (Exception e) {
      handleException(e, "trying to get a report definition");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/find", method = RequestMethod.POST)
  @POST
  @Path("/find")
  @Operation(summary = "Finds reports",
      description = "Finds reports for the specified query")
  public ReportList findReports(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Query") @RequestParam(value = "query", required = false) String query,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'") @RequestBody PfsParameterJpa pfs,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Report): /find, " + query + " " + pfs);
    final ReportService reportService = new ReportServiceJpa();
    try {
      authorizeApp(securityService, authToken, "find reports", UserRole.VIEWER);
      final Project project = reportService.getProject(projectId);
      ReportList list = reportService.findReports(project, query, pfs);
      
      for (Report report : list.getObjects()) {
        if (report != null) {
          reportService.handleLazyInit(report);
        }
      }
      
      return list;
    } catch (Exception e) {
      handleException(e, "trying to find reports ");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }


  /* see superclass */
  @Override
  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/{id}")
  @Operation(summary = "Remove report",
      description = "Removes the report with the specified id")
  public void removeReport(
    @Parameter(description = "Report id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Report): /" + id);

    final ReportService reportService = new ReportServiceJpa();
    try {
      final String userName = authorizeProject(reportService, id,
          securityService, authToken, "remove report", UserRole.AUTHOR);

      reportService.setLastModifiedBy(userName);
      // Create service and configure transaction scope
      reportService.removeReport(id);

      reportService.addLogEntry(userName, id, id, null, null,
          "REMOVE report " + id);

    } catch (Exception e) {
      handleException(e, "trying to remove a report");
    } finally {
      reportService.close();
      securityService.close();
    }
  }
  
  /* see superclass */
  @Override
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @GET
  @Path("/{id}")
  @Operation(summary = "Get report for id",
      description = "Gets the report for the specified id")
  public Report getReport(
    @Parameter(description = "Project internal id, e.g. 2", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Project): /" + id);

    final ReportService reportService = new ReportServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get the report",
          UserRole.VIEWER);
      final Report report = reportService.getReport(id);
      reportService.handleLazyInit(report);
      return report;
    } catch (Exception e) {
      handleException(e, "trying to get a report");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }

  @Override
  @RequestMapping(value = "/generate/{projectId}", method = RequestMethod.GET)
  @GET
  @Path("/generate/{projectId}")
  @Operation(summary = "Generates a report",
      description = "Generates a report")
  public Report generateReport(
    @Parameter(description = "Project internal id, e.g. 2", required = true) @PathVariable("projectId") Long projectId,
    @Parameter(description = "Name") @RequestParam(value = "name", required = false) String name,
    @Parameter(description = "Query", required = true) @RequestParam(value = "query", required = false) String query,
    @Parameter(description = "Query Type, e.g. LUCENE", required = true) @RequestParam(value = "queryType", required = false) QueryType queryType,
    @Parameter(description = "Object type name, e.g. AtomJpa") @RequestParam(value = "resultType", required = false) IdType resultType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Report): /generate");

      final ReportService reportService = new ReportServiceJpa();
      try {
        final String userName = authorizeApp(securityService, authToken, "generate the report",
            UserRole.VIEWER);
        reportService.setLastModifiedBy(userName);
      
      Project project = reportService.getProject(projectId);
      Report report = reportService.generateReport(project, name, query, queryType, resultType);

      reportService.addLogEntry(userName, project.getId(), project.getId(),
          null, null, "GENERATE report - " + name);
      return report;
    } catch (Exception e) {
      handleException(e, "trying to generate a report");
      return null;
    } finally {
      reportService.close();
      securityService.close();
    }
  }
}
