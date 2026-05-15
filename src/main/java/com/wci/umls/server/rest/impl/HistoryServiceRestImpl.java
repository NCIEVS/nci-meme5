/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import com.wci.umls.server.jpa.model.ReleaseInfoJpa;
import com.wci.umls.server.jpa.model.helpers.ReleaseInfoListJpa;
import com.wci.umls.server.model.algo.ReleaseInfo;
import com.wci.umls.server.model.algo.UserRole;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.helpers.ReleaseInfoList;
import com.wci.umls.server.jpa.services.HistoryServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.HistoryServiceRest;
import com.wci.umls.server.services.HistoryService;
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
 * REST implementation for {@link HistoryServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/history")
@Path("/history")
@Api(value = "/history")
@SwaggerDefinition(info = @Info(description = "Operations to retrieve historical content for a terminology.", title = "History API", version = "1.0.1"))
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class HistoryServiceRestImpl extends RootServiceRestImpl
    implements HistoryServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link HistoryServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public HistoryServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/releases/{terminology}", method = RequestMethod.GET)
  @GET
  @Path("/releases/{terminology}")
  @ApiOperation(value = "Get release history", notes = "Gets all release info objects", response = ReleaseInfoListJpa.class)
  public ReleaseInfoList getReleaseHistory(
    @ApiParam(value = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release/history/");

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get release history",
          UserRole.VIEWER);

      ReleaseInfoList result = historyService.getReleaseHistory(terminology);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to get release history");
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release/{terminology}/current", method = RequestMethod.GET)
  @GET
  @Path("/release/{terminology}/current")
  @ApiOperation(value = "Get current release info", notes = "Gets release info for current release", response = ReleaseInfoJpa.class)
  public ReleaseInfo getCurrentReleaseInfo(
    @ApiParam(value = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release/current/");

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get current release info",
          UserRole.VIEWER);

      ReleaseInfo result = historyService.getCurrentReleaseInfo(terminology);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to get current release info");
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release/{terminology}/previous", method = RequestMethod.GET)
  @GET
  @Path("/release/{terminology}/previous")
  @ApiOperation(value = "Get previous release info", notes = "Gets release info for previous release", response = ReleaseInfoJpa.class)
  public ReleaseInfo getPreviousReleaseInfo(
    @ApiParam(value = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release/previous/");

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get previous release info",
          UserRole.VIEWER);

      ReleaseInfo result = historyService.getPreviousReleaseInfo(terminology);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to get previous release info");
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release/{terminology}/planned", method = RequestMethod.GET)
  @GET
  @Path("/release/{terminology}/planned")
  @ApiOperation(value = "Get planned release info", notes = "Gets release info for planned release", response = ReleaseInfoJpa.class)
  public ReleaseInfo getPlannedReleaseInfo(
    @ApiParam(value = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release/planned/");

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get planned release info",
          UserRole.VIEWER);

      ReleaseInfo result = historyService.getPlannedReleaseInfo(terminology);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to get planned release info");
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release/{terminology}/{name}", method = RequestMethod.GET)
  @GET
  @Path("/release/{terminology}/{name}")
  @ApiOperation(value = "Get release info", notes = "Gets release info for specified release name and terminology", response = ReleaseInfoJpa.class)
  public ReleaseInfo getReleaseInfo(
    @ApiParam(value = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @ApiParam(value = "Release version info, e.g. 'latest'", required = true) @PathVariable("name") String name,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release/" + name);

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get release info for " + name,
          UserRole.VIEWER);

      ReleaseInfo result = historyService.getReleaseInfo(terminology, name);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to get release info for " + name);
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release", method = RequestMethod.PUT)
  @PUT
  @Path("/release")
  @ApiOperation(value = "Add release info", notes = "Adds the specified release info", response = ReleaseInfoJpa.class)
  public ReleaseInfo addReleaseInfo(
    @ApiParam(value = "Release info object, e.g. see output of /release/current", required = true) @RequestBody ReleaseInfoJpa releaseInfo,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release " + releaseInfo.getName());

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "add release info",
          UserRole.ADMINISTRATOR);

      releaseInfo
          .setLastModifiedBy(securityService.getUsernameForToken(authToken));
      ReleaseInfo result = historyService.addReleaseInfo(releaseInfo);
      return result;

    } catch (Exception e) {
      handleException(e, "trying to add release info");
      return null;
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release", method = RequestMethod.POST)
  @POST
  @Path("/release")
  @ApiOperation(value = "Update release info", notes = "Updatess the specified release info")
  public void updateReleaseInfo(
    @ApiParam(value = "Release info object, e.g. see output of /release/current", required = true) @RequestBody ReleaseInfoJpa releaseInfo,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (History): /release " + releaseInfo.getName());

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "update release info",
          UserRole.ADMINISTRATOR);

      releaseInfo
          .setLastModifiedBy(securityService.getUsernameForToken(authToken));
      historyService.updateReleaseInfo(releaseInfo);
    } catch (Exception e) {
      handleException(e, "trying to update release info");
    } finally {
      historyService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/release/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/release/{id}")
  @ApiOperation(value = "Remove release info", notes = "Removes the release info for the specified id")
  public void removeReleaseInfo(
    @ApiParam(value = "Release info object id, e.g. 2", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'guest'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (History): /release/" + id);

    HistoryService historyService = new HistoryServiceJpa();
    try {
      authorizeApp(securityService, authToken, "remove release info",
          UserRole.ADMINISTRATOR);

      historyService.removeReleaseInfo(id);
    } catch (Exception e) {
      handleException(e, "trying to remove release info");
    } finally {
      historyService.close();
      securityService.close();
    }
  }
}
