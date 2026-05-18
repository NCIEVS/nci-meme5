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
 * REST implementation for {@link HistoryServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/history")
@Path("/history")
@Tag(name = "History", description = "Operations to retrieve historical content for a terminology.")
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
  @Operation(summary = "Get release history",
      description = "Gets all release info objects")
  public ReleaseInfoList getReleaseHistory(
    @Parameter(description = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Get current release info",
      description = "Gets release info for current release")
  public ReleaseInfo getCurrentReleaseInfo(
    @Parameter(description = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Get previous release info",
      description = "Gets release info for previous release")
  public ReleaseInfo getPreviousReleaseInfo(
    @Parameter(description = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Get planned release info",
      description = "Gets release info for planned release")
  public ReleaseInfo getPlannedReleaseInfo(
    @Parameter(description = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Get release info",
      description = "Gets release info for specified release name and terminology")
  public ReleaseInfo getReleaseInfo(
    @Parameter(description = "Release info terminology , e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Release version info, e.g. 'latest'", required = true) @PathVariable("name") String name,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Add release info",
      description = "Adds the specified release info")
  public ReleaseInfo addReleaseInfo(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Release info object, e.g. see output of /release/current", required = true) @RequestBody ReleaseInfoJpa releaseInfo,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Update release info",
      description = "Updatess the specified release info")
  public void updateReleaseInfo(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Release info object, e.g. see output of /release/current", required = true) @RequestBody ReleaseInfoJpa releaseInfo,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
  @Operation(summary = "Remove release info",
      description = "Removes the release info for the specified id")
  public void removeReleaseInfo(
    @Parameter(description = "Release info object id, e.g. 2", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
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
