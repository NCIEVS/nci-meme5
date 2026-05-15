/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.QueryStyle;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.helpers.WorkflowBinDefinitionList;
import com.wci.umls.server.jpa.model.ProjectJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.model.helpers.WorkflowBinDefinitionListJpa;
import com.wci.umls.server.jpa.model.report.ReportJpa;
import com.wci.umls.server.jpa.model.report.ReportListJpa;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
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
 * REST implementation for {@link ReportServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/report")
@Path("/report")
@Api(value = "/report")
@SwaggerDefinition(info = @Info(description = "Operations for reporting.", title = "Report API", version = "1.0.1"))
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
  @RequestMapping(value = "/concept/{id}", method = RequestMethod.GET)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/concept/{id}")
  @ApiOperation(value = "Get concept report", notes = "Gets a concept report", response = String.class)
  public String getConceptReport(
    @ApiParam(value = "Project id, e.g. UMLS", required = false) @RequestParam(value = "projectId", required = false) Long projectId,
    @ApiParam(value = "Concept id, e.g. UMLS", required = true) @PathVariable("id") Long conceptId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @RequestMapping(value = "/descriptor/{id}", method = RequestMethod.GET)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/descriptor/{id}")
  @ApiOperation(value = "Get descriptor report", notes = "Gets a descriptor report", response = String.class)
  public String getDescriptorReport(
    @ApiParam(value = "Project id, e.g. UMLS", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @ApiParam(value = "Descriptor id, e.g. UMLS", required = true) @PathVariable("id") Long descriptorId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @RequestMapping(value = "/code/{id}", method = RequestMethod.GET)
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/code/{id}")
  @ApiOperation(value = "Get code report", notes = "Gets a code report", response = String.class)
  public String getCodeReport(
    @ApiParam(value = "Project id, e.g. UMLS", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @ApiParam(value = "Code id, e.g. UMLS", required = true) @PathVariable("id") Long codeId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @ApiOperation(value = "Find report definitions", notes = "Find report definitions")
  public WorkflowBinDefinitionList findReportDefinitions(
    @ApiParam(value = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @ApiOperation(value = "Finds reports", notes = "Finds reports for the specified query", response = ReportListJpa.class)
  public ReportList findReports(
    @ApiParam(value = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @ApiParam(value = "Query", required = false) @RequestParam(value = "query", required = false) String query,
    @ApiParam(value = "PFS Parameter, e.g. '{ \"startIndex\":\"1\", \"maxResults\":\"5\" }'", required = false) @RequestBody PfsParameterJpa pfs,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @ApiOperation(value = "Remove report", notes = "Removes the report with the specified id")
  public void removeReport(
    @ApiParam(value = "Report id, e.g. 3", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @ApiOperation(value = "Get report for id", notes = "Gets the report for the specified id", response = ProjectJpa.class)
  public Report getReport(
    @ApiParam(value = "Project internal id, e.g. 2", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @ApiOperation(value = "Generates a report", notes = "Generates a report", response = ReportJpa.class)
  public Report generateReport(
    @ApiParam(value = "Project internal id, e.g. 2", required = true) @PathVariable("projectId") Long projectId,
    @ApiParam(value = "Name", required = false) @RequestParam(value = "name", required = false) String name,
    @ApiParam(value = "Query", required = true) @RequestParam(value = "query", required = false) String query,
    @ApiParam(value = "Query Type, e.g. LUCENE", required = true) @RequestParam(value = "queryType", required = false) QueryType queryType,
    @ApiParam(value = "Object type name, e.g. AtomJpa", required = false) @RequestParam(value = "resultType", required = false) IdType resultType,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
