/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.content.SourceIdRangeList;
import com.wci.umls.server.jpa.model.inversion.SourceIdRangeJpa;
import com.wci.umls.server.jpa.services.InversionServiceJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.InversionServiceRest;
import com.wci.umls.server.model.inversion.SourceIdRange;
import com.wci.umls.server.services.InversionService;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;

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
 * REST implementation for {@link InversionServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/inversion")
@Path("/inversion")
@Api(value = "/inversion")
@SwaggerDefinition(info = @Info(description = "Operations for inversion.", title = "Inversion API", version = "1.0.1"))
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class InversionServiceRestImpl extends RootServiceRestImpl
    implements InversionServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link InversionServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public InversionServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }


  static {
    Logger.getLogger("InversionServiceRestImpl registered");
  }
  /* see superclass */
  @Override
  @RequestMapping(value = "/range/{id}/{terminology}", method = RequestMethod.GET)
  @GET
  @Path("/range/{id}/{terminology}")
  @ApiOperation(value = "Get sourceIdRange for vsab", notes = "Gets the sourceIdRange for the specified versioned source abbreviation", response = SourceIdRangeJpa.class)
  public SourceIdRangeList getSourceIdRange(
    @ApiParam(value = "Project id, e.g. 2", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "SourceIdRange terminology, e.g. MTH", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (SourceIdRange): /" + id + "/" + terminology );

    final InversionService inversionService = new InversionServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get the inversion service",
          UserRole.VIEWER);
      final ProjectService projectService = new ProjectServiceJpa();
      final Project project = projectService.getProject(id);
      final SourceIdRangeList sourceIdRangeList = inversionService.getSourceIdRange(project, terminology);
      return sourceIdRangeList;
    } catch (Exception e) {
      handleException(e, "trying to get sourceIdRange(s)");
      return null;
    } finally {
      inversionService.close();
      securityService.close();
    }
  }

  
  /* see superclass */
  @Override
  @RequestMapping(value = "/range/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/range/{id}")
  @ApiOperation(value = "Remove source id range", notes = "Removes the source id range with the specified id")
  public void removeSourceIdRange(
    @ApiParam(value = "Source id range id, e.g. 3", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Inversion): /" + id);

    final InversionService inversionService = new InversionServiceJpa();
    try {
      final String userName = authorizeProject(new InversionServiceJpa(), id,
          securityService, authToken, "remove source id range", UserRole.AUTHOR);

      inversionService.setLastModifiedBy(userName);
      // Create service and configure transaction scope
      inversionService.removeSourceIdRange(id);

      inversionService.addLogEntry(userName, id, id, null, null,
          "REMOVE source id range " + id);

    } catch (Exception e) {
      handleException(e, "trying to remove a source id range");
    } finally {
      inversionService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/range/{id}/{terminology}/{numberofids}", method = RequestMethod.GET)
  @GET
  @Path("/range/{id}/{terminology}/{numberofids}")
  @ApiOperation(value = "Request new sourceIdRange for vsab", notes = "Requests a new sourceIdRange for the specified versioned source abbreviation", response = SourceIdRangeJpa.class)
  public SourceIdRange requestSourceIdRange(
    @ApiParam(value = "Project id, e.g. 2", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "SourceIdRange terminology, e.g. MTH", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Number of ids requested, e.g. 100000", required = true) @PathVariable("numberofids") Integer numberOfIds,
    @ApiParam(value = "Begin id to start the range (only for SNOMED)", required = false) @RequestParam(value = "beginSourceId", required = false) Long beginSourceId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (SourceIdRange): /range/" + id + "/" + terminology + "/" + numberOfIds + ": " + beginSourceId);

    final InversionService inversionService = new InversionServiceJpa();
    try {
      String userName = authorizeApp(securityService, authToken, "get the inversion service",
          UserRole.VIEWER);
      
      inversionService.setLastModifiedBy(userName);
      final Project project = inversionService.getProject(id);
      SourceIdRange sourceIdRange;
      try {
        long beginId = beginSourceId != null ? beginSourceId.longValue() : 0L;
        sourceIdRange = inversionService.requestSourceIdRange(project, terminology, numberOfIds, beginId);
      } catch (Exception e) {
        if (e instanceof LocalException) {
          throw new LocalException(e.getMessage());
        } else {
          throw new LocalException(
            "The source id range has already been assigned for " + terminology + " with version " +
            ".  Consider the 'Submit Adjustment' option.");
        }
      }
      return sourceIdRange;
    } catch (Exception e) {
      handleException(e, "trying to request a source id range");
      return null;
    } finally {
      inversionService.close();
      securityService.close();
    }
  }
  
  /* see superclass */
  @Override
  @RequestMapping(value = "/range/update/{id}/{terminology}/{numberofids}", method = RequestMethod.GET)
  @GET
  @Path("/range/update/{id}/{terminology}/{numberofids}")
  @ApiOperation(value = "Request new sourceIdRange for vsab", notes = "Requests a new sourceIdRange for the specified versioned source abbreviation", response = SourceIdRangeJpa.class)
  public SourceIdRange updateSourceIdRange(
    @ApiParam(value = "Project id, e.g. 2", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "SourceIdRange terminology, e.g. MTH", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Number of ids requested, e.g. 100000", required = true) @PathVariable("numberofids") Integer numberOfIds,
    @ApiParam(value = "Begin id to start the range (only for SNOMED)", required = false) @RequestParam(value = "beginSourceId", required = false) Long beginSourceId,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (SourceIdRange): /range/" + id + "/" + terminology + "/" + numberOfIds + ": " + beginSourceId);

    final InversionService inversionService = new InversionServiceJpa();
    try {
      String userName = authorizeApp(securityService, authToken, "get the inversion service",
          UserRole.VIEWER);
      
      inversionService.setLastModifiedBy(userName);
      final Project project = inversionService.getProject(id);
      SourceIdRangeList sourceIdRangeList; 
      SourceIdRange sourceIdRange = null;
      try {
        sourceIdRangeList = inversionService.getSourceIdRange(project, terminology);
        for (SourceIdRange sir : sourceIdRangeList.getObjects()) {
          if (sir.getTerminology().equals(terminology)) {
            sourceIdRange = sir;
          }
        }
        if (sourceIdRange == null) {
          throw new Exception();
        }
      } catch(Exception e) {
        throw new LocalException(
            "The source id range has not yet been assigned for " + terminology  +
            ".  Consider the 'Request Range' option.");
      }
      
      sourceIdRange = inversionService.updateSourceIdRange(sourceIdRange, numberOfIds, beginSourceId);
      return sourceIdRange;
    } catch (Exception e) {
      if (e instanceof LocalException) {
        handleException(e, e.getMessage());
      } else {
        handleException(e, "trying to update a source id range");
      }
      return null;
    } finally {
      inversionService.close();
      securityService.close();
    }
  }
}
