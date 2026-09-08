
package com.wci.umls.server.rest.impl;

import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.helpers.KeyValuePairLists;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.PrecedenceList;
import com.wci.umls.server.helpers.meta.AdditionalRelationshipTypeList;
import com.wci.umls.server.helpers.meta.SemanticTypeList;
import com.wci.umls.server.helpers.meta.TermTypeList;
import com.wci.umls.server.helpers.meta.TerminologyList;
import com.wci.umls.server.jpa.model.helpers.PrecedenceListJpa;
import com.wci.umls.server.jpa.model.helpers.meta.AdditionalRelationshipTypeListJpa;
import com.wci.umls.server.jpa.model.helpers.meta.RelationshipTypeListJpa;
import com.wci.umls.server.jpa.model.meta.AdditionalRelationshipTypeJpa;
import com.wci.umls.server.jpa.model.meta.AttributeNameJpa;
import com.wci.umls.server.jpa.model.meta.RelationshipTypeJpa;
import com.wci.umls.server.jpa.model.meta.RootTerminologyJpa;
import com.wci.umls.server.jpa.model.meta.TermTypeJpa;
import com.wci.umls.server.jpa.model.meta.TerminologyJpa;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.MetadataServiceRest;
import com.wci.umls.server.model.meta.AdditionalRelationshipType;
import com.wci.umls.server.model.meta.AttributeName;
import com.wci.umls.server.model.meta.RelationshipType;
import com.wci.umls.server.model.meta.RootTerminology;
import com.wci.umls.server.model.meta.TermType;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.SecurityService;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST implementation for {@link MetadataServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/metadata")
@Path("/metadata")
@Tag(name = "Metadata", description = "Operations providing terminology metadata.")
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MetadataServiceRestImpl extends RootServiceRestImpl implements MetadataServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MetadataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public MetadataServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/terminology/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/terminology/{terminology}/{version}")
  @Operation(summary = "Get terminology",
      description = "Gets the terminology for the specified parameters")
  public Terminology getTerminology(
    @Parameter(description = "Terminology name, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /terminology/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      // authorize call
      authorizeApp(securityService, authToken, "get terminology", UserRole.VIEWER);

      final Terminology termInfo = metadataService.getTerminology(terminology, version);
      if (termInfo == null) {
        return new TerminologyJpa();
      }
      metadataService.getGraphResolutionHandler(terminology).resolve(termInfo);

      return termInfo;

    } catch (Exception e) {

      handleException(e, "trying to get the terminology");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/rootTerminology/{terminology}", method = RequestMethod.GET)
  @GET
  @Path("/rootTerminology/{terminology}")
  @Operation(summary = "Get root terminology",
      description = "Gets the root terminology for the specified parameters")
  public RootTerminology getRootTerminology(
    @Parameter(description = "Terminology name, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /rootTerminology/" + terminology);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      // authorize call
      authorizeApp(securityService, authToken, "get root terminology", UserRole.VIEWER);

      final RootTerminology termInfo = metadataService.getRootTerminology(terminology);
      if (termInfo == null) {
        return new RootTerminologyJpa();
      }
      metadataService.getGraphResolutionHandler(terminology).resolve(termInfo);

      return termInfo;

    } catch (Exception e) {

      handleException(e, "trying to get the root terminology");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/all/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/all/{terminology}/{version}")
  @Operation(summary = "Get metadata for terminology and version",
      description = "Gets the key-value pairs representing all metadata for a particular terminology and version")
  public KeyValuePairLists getAllMetadata(
    @Parameter(description = "Terminology name, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /all/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      // authorize call
      authorizeApp(securityService, authToken, "get all metadata", UserRole.VIEWER);

      final KeyValuePairLists keyValuePairList = getMetadataHelper(terminology, version);

      return keyValuePairList;

    } catch (Exception e) {
      handleException(e, "trying to get the metadata");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /**
   * Gets the metadata helper.
   *
   * @param terminology the terminology
   * @param version the version
   * @return the metadata helper
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private KeyValuePairLists getMetadataHelper(String terminology, String version) throws Exception {
    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      RootTerminology rootTerminology = null;
      for (final RootTerminology root : metadataService.getRootTerminologies().getObjects()) {
        if (root.getTerminology().equals(terminology)) {
          rootTerminology = root;
          break;
        }
      }
      if (rootTerminology == null) {
        throw new LocalException("Unexpected missing terminology - " + terminology);
      }

      Terminology term = null;
      for (final Terminology t : metadataService.getVersions(terminology).getObjects()) {
        if (t.getVersion().equals(version)) {
          term = t;
          break;
        }
      }
      if (term == null) {
        throw new LocalException(
            "Unexpected missing terminology/version - " + terminology + ", " + version);

      }

      // call jpa service and get complex map return type
      final Map<String, Map<String, String>> mapOfMaps =
          metadataService.getAllMetadata(terminology, version);

      // convert complex map to KeyValuePair objects for easy transformation to
      // XML/JSON
      final KeyValuePairLists keyValuePairLists = new KeyValuePairLists();
      for (final Map.Entry<String, Map<String, String>> entry : mapOfMaps.entrySet()) {
        final String metadataType = entry.getKey();
        final Map<String, String> metadataPairs = entry.getValue();
        final KeyValuePairList keyValuePairList = new KeyValuePairList();
        keyValuePairList.setName(metadataType);
        for (final Map.Entry<String, String> pairEntry : metadataPairs.entrySet()) {
          final KeyValuePair keyValuePair =
              new KeyValuePair(pairEntry.getKey().toString(), pairEntry.getValue());
          keyValuePairList.addKeyValuePair(keyValuePair);
        }
        keyValuePairLists.addKeyValuePairList(keyValuePairList);
      }
      keyValuePairLists.sort();
      return keyValuePairLists;
    } catch (Exception e) {
      throw e;
    } finally {
      metadataService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/terminology/current", method = RequestMethod.GET)
  @GET
  @Path("/terminology/current")
  @Operation(summary = "Get current terminologies",
      description = "Gets the list of current terminologies")
  public TerminologyList getCurrentTerminologies(
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /terminology/current");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      // authorize call
      authorizeApp(securityService, authToken, "get terminologies", UserRole.VIEWER);

      final TerminologyList results = metadataService.getCurrentTerminologies();
      for (final Terminology terminology : results.getObjects()) {
        metadataService.getGraphResolutionHandler(terminology.getTerminology())
            .resolve(terminology);
      }
      return results;

    } catch (Exception e) {
      handleException(e, "trying to get all terminologies");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/precedence/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/precedence/{terminology}/{version}")
  @Operation(summary = "Get default precedence list",
      description = "Gets the default precedence list ranking for the specified parameters")
  public PrecedenceList getDefaultPrecedenceList(
    @Parameter(description = "Terminology name, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /precedence/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      // authorize call
      authorizeApp(securityService, authToken, "get precedence list", UserRole.VIEWER);

      final PrecedenceList precedenceList = metadataService.getPrecedenceList(terminology, version);
      // Lazy initialize
      if (precedenceList != null) {
        ConfigUtility.initializeLazy(
            precedenceList.getPrecedence().getKeyValuePairs());
      }
      return precedenceList;

    } catch (Exception e) {

      handleException(e, "trying to get the metadata");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/precedence/{id}", method = RequestMethod.GET)
  @GET
  @Path("/precedence/{id}")
  @Operation(summary = "Gets a precedence list",
      description = "Gets a precedence list")
  public PrecedenceList getPrecedenceList(
    @Parameter(description = "Precedence list id, e.g. 1", required = true) @PathVariable("id") Long precedenceListId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /precedence/" + precedenceListId);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get precedence list", UserRole.VIEWER);

      final PrecedenceList list = metadataService.getPrecedenceList(precedenceListId);
      if (list == null) {
        return null;
      }
      // lazy initialize
      ConfigUtility.initializeLazy(list.getPrecedence().getKeyValuePairs());
      ConfigUtility.initializeLazy(list.getTermTypeRankMap());
      return list;
    } catch (Exception e) {
      handleException(e, "trying to get precedence list");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/precedence", method = RequestMethod.PUT)
  @PUT
  @Path("/precedence")
  @Operation(summary = "Add a precedence list",
      description = "Add a precedence list")
  public PrecedenceList addPrecedenceList(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Precedence list to add", required = true) @RequestBody PrecedenceListJpa precedenceList,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /precedence");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "add precedence list", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.addPrecedenceList(precedenceList);
    } catch (Exception e) {
      handleException(e, "trying to add precedence list");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/precedence", method = RequestMethod.POST)
  @POST
  @Path("/precedence")
  @Operation(summary = "Update a precedence list",
      description = "Update a precedence list")
  public void updatePrecedenceList(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Precedence list to update", required = true) @RequestBody PrecedenceListJpa precedenceList,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /precedence");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update precedence list", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updatePrecedenceList(precedenceList);
    } catch (Exception e) {
      handleException(e, "trying to update precedence list");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/precedence/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/precedence/{id}")
  @Operation(summary = "Remove a precedence list",
      description = "Remove a precedence list")
  public void removePrecedenceList(
    @Parameter(description = "Precedence list id, e.g. 1", required = true) @PathVariable("id") Long id,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /precedence");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "remove precedence list", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.removePrecedenceList(id);
    } catch (Exception e) {

      handleException(e, "trying to get the metadata");
    } finally {
      metadataService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/termType/{type}/{terminology}/{version}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/termType/{type}/{terminology}/{version}")
  @Operation(summary = "Remove a term type",
      description = "Remove a term type")
  public void removeTermType(
    @Parameter(description = "Term type, e.g. AB", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /termType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "remove term type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      TermType tty = metadataService.getTermType(type, terminology, version);
      metadataService.removeTermType(tty.getId());
    } catch (Exception e) {

      handleException(e, "trying to remove the term type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/termType/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/termType/{terminology}/{version}")
  @Operation(summary = "Retrieve all term type",
      description = "Retrieve all term types")
  public TermTypeList getTermTypes(
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /termType/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "get term types ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.getTermTypes(terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the term types");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/termType/{type}/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/termType/{type}/{terminology}/{version}")
  @Operation(summary = "Retrieve a term type",
      description = "Retrieve a term type")
  public TermType getTermType(
    @Parameter(description = "Term type, e.g. AB", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /termType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "get term type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.getTermType(type, terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the term type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attributeName/{type}/{terminology}/{version}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/attributeName/{type}/{terminology}/{version}")
  @Operation(summary = "Remove a attribute name",
      description = "Remove a attribute name")
  public void removeAttributeName(
    @Parameter(description = "Attribute name, e.g. AMT", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /attributeName/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "remove attribute name ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      AttributeName atn = metadataService.getAttributeName(type, terminology, version);
      metadataService.removeAttributeName(atn.getId());
    } catch (Exception e) {

      handleException(e, "trying to remove the attribute name");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attributeName/{type}/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/attributeName/{type}/{terminology}/{version}")
  @Operation(summary = "Retrieve a attribute name",
      description = "Retrieve a attribute name")
  public AttributeName getAttributeName(
    @Parameter(description = "Attribute name, e.g. AMT", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /attributeName/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "get attribute name ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.getAttributeName(type, terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the attribute name");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/additionalRelationshipType/{type}/{terminology}/{version}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/additionalRelationshipType/{type}/{terminology}/{version}")
  @Operation(summary = "Remove a add relationship type",
      description = "Remove a additional relationship type")
  public void removeAdditionalRelationshipType(
    @Parameter(description = "Additional Relationship type, e.g. RB", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /additionalRelationshipType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "remove add relationship type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      AdditionalRelationshipType relType =
          metadataService.getAdditionalRelationshipType(type, terminology, version);
      AdditionalRelationshipType inverse = relType.getInverse();
      relType.setInverse(null);
      metadataService.updateAdditionalRelationshipType(relType);
      inverse.setInverse(null);
      metadataService.updateAdditionalRelationshipType(inverse);
      metadataService.removeAdditionalRelationshipType(relType.getId());
      metadataService.removeAdditionalRelationshipType(inverse.getId());

    } catch (Exception e) {

      handleException(e, "trying to remove the add relationship type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/additionalRelationshipType/{type}/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/additionalRelationshipType/{type}/{terminology}/{version}")
  @Operation(summary = "Retrieve a additional relationship type",
      description = "Retrieve a additional relationship type")
  public AdditionalRelationshipType getAdditionalRelationshipType(
    @Parameter(description = "Relationship type, e.g. RN", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /additionalRelationshipType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName = authorizeApp(securityService, authToken,
          "get additional relationship type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.getAdditionalRelationshipType(type, terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the additional relationship type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/additionalRelationshipType/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/additionalRelationshipType/{terminology}/{version}")
  @Operation(summary = "Retrieve all additional relationship types",
      description = "Retrieve all additional relationship types")
  public AdditionalRelationshipTypeList getAdditionalRelationshipTypes(
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Metadata): /additionalRelationshipType/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName = authorizeApp(securityService, authToken,
          "get additional relationship type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);
      return metadataService.getAdditionalRelationshipTypes(terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the additional relationship type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationshipType/{type}/{terminology}/{version}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/relationshipType/{type}/{terminology}/{version}")
  @Operation(summary = "Remove a rel type",
      description = "Remove a rel type")
  public void removeRelationshipType(
    @Parameter(description = "Relationship type, e.g. AB", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /relationshipType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "remove rel type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      // must also remove the inverse to avoid foreign key constraint
      // TODO setTranPerOper begin
      RelationshipType relType = metadataService.getRelationshipType(type, terminology, version);
      RelationshipType inverse = relType.getInverse();
      relType.setInverse(null);
      metadataService.updateRelationshipType(relType);
      inverse.setInverse(null);
      metadataService.updateRelationshipType(inverse);
      metadataService.removeRelationshipType(relType.getId());
      metadataService.removeRelationshipType(inverse.getId());
      // commit
    } catch (Exception e) {

      handleException(e, "trying to remove the rel type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationshipType/{type}/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("/relationshipType/{type}/{terminology}/{version}")
  @Operation(summary = "Retrieve a relationship type",
      description = "Retrieve a relationship type")
  public RelationshipType getRelationshipType(
    @Parameter(description = "Relationship type, e.g. RN", required = true) @PathVariable("type") String type,
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /relationshipType/" + type);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {

      final String userName =
          authorizeApp(securityService, authToken, "get relationship type ", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.getRelationshipType(type, terminology, version);
    } catch (Exception e) {

      handleException(e, "trying to retrieve the relationship type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "sty/{terminology}/{version}", method = RequestMethod.GET)
  @GET
  @Path("sty/{terminology}/{version}")
  @Operation(summary = "Get semantic types",
      description = "Get semantic types for the specified parameters")
  public SemanticTypeList getSemanticTypes(
    @Parameter(description = "Terminology, e.g. UMLS", required = true) @PathVariable("terminology") String terminology,
    @Parameter(description = "Version, e.g. latest", required = true) @PathVariable("version") String version,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Metadata): /sty/" + terminology + "/" + version);

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get semantic types", UserRole.USER);

      return metadataService.getSemanticTypes(terminology, version);
    } catch (Exception e) {
      handleException(e, "trying to get semantic types");
    } finally {
      metadataService.close();
      securityService.close();
    }
    return null;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/termType", method = RequestMethod.POST)
  @POST
  @Path("/termType")
  @Operation(summary = "Update a term type",
      description = "Update a term type")
  public void updateTermType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Term type to update", required = true) @RequestBody TermTypeJpa termType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /termType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update term type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateTermType(termType);
    } catch (Exception e) {
      handleException(e, "trying to update term type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attributeName", method = RequestMethod.POST)
  @POST
  @Path("/attributeName")
  @Operation(summary = "Update an attribute name",
      description = "Update an attribute name")
  public void updateAttributeName(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Attribute name to update", required = true) @RequestBody AttributeNameJpa attributeName,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /attributeName");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update attribute name", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateAttributeName(attributeName);
    } catch (Exception e) {
      handleException(e, "trying to update attribute name");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationshipType", method = RequestMethod.POST)
  @POST
  @Path("/relationshipType")
  @Operation(summary = "Update a relationship type",
      description = "Update a relationship type")
  public void updateRelationshipType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Relationship type to update", required = true) @RequestBody RelationshipTypeJpa relType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /relationshipType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update rel type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateRelationshipType(relType);
    } catch (Exception e) {
      handleException(e, "trying to update rel type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/rootTerminology", method = RequestMethod.POST)
  @POST
  @Path("/rootTerminology")
  @Operation(summary = "Update a root terminology",
      description = "Update a root terminology")
  public void updateRootTerminology(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Root terminology to update", required = true) @RequestBody RootTerminologyJpa rootTerminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /rootTerminology");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update root terminology", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateRootTerminology(rootTerminology);
    } catch (Exception e) {
      handleException(e, "trying to update root terminology");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/terminology", method = RequestMethod.POST)
  @POST
  @Path("/terminology")
  @Operation(summary = "Update a terminology",
      description = "Update a terminology")
  public void updateTerminology(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Terminology to update", required = true) @RequestBody TerminologyJpa terminology,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /terminology");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update terminology", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateTerminology(terminology);
    } catch (Exception e) {
      handleException(e, "trying to update terminology");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/additionalRelationshipType", method = RequestMethod.POST)
  @POST
  @Path("/additionalRelationshipType")
  @Operation(summary = "Update a relationship type",
      description = "Update a relationship type")
  public void updateAdditionalRelationshipType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "AdditionalRelationship type to update", required = true) @RequestBody AdditionalRelationshipTypeJpa relType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Metadata): /additionalRelationshipType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "update add rel type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      metadataService.updateAdditionalRelationshipType(relType);
    } catch (Exception e) {
      handleException(e, "trying to update add rel type");
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/termType", method = RequestMethod.PUT)
  @PUT
  @Path("/termType")
  @Operation(summary = "Add a term type",
      description = "Add a term type")
  public TermType addTermType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Term type to add", required = true) @RequestBody TermTypeJpa termType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /termType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "add term type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.addTermType(termType);
    } catch (Exception e) {
      handleException(e, "trying to add term type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attributeName", method = RequestMethod.PUT)
  @PUT
  @Path("/attributeName")
  @Operation(summary = "Add an attribute name",
      description = "Add an attribute name")
  public AttributeName addAttributeName(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Attribute name to add", required = true) @RequestBody AttributeNameJpa attributeName,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /attributeName");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "add attribute name", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      return metadataService.addAttributeName(attributeName);
    } catch (Exception e) {
      handleException(e, "trying to add attribute name");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationshipType", method = RequestMethod.PUT)
  @PUT
  @Path("/relationshipType")
  @Operation(summary = "Add a relationship type (and its inverse)",
      description = "Add a relationship type and its inverse")
  public RelationshipType addRelationshipType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Relationship type (and its inverse) to add", required = true) @RequestBody RelationshipTypeListJpa relationshipTypeList,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /relationshipType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "add relationship type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      // add relType and its inverse
      metadataService.setTransactionPerOperation(false);
      metadataService.beginTransaction();
      RelationshipType relType1 = relationshipTypeList.getObjects().get(0);
      RelationshipType relType2 = relationshipTypeList.getObjects().get(1);
      relType1.setInverse(null);
      relType2.setInverse(null);
      relType1 = metadataService.addRelationshipType(relType1);
      relType2 = metadataService.addRelationshipType(relType2);
      relType1.setInverse(relType2);
      metadataService.updateRelationshipType(relType1);
      relType2.setInverse(relType1);
      metadataService.updateRelationshipType(relType2);
      metadataService.commit();

      return relType1;

    } catch (Exception e) {
      handleException(e, "trying to add relationship type and its inverse");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/additionalRelationshipType", method = RequestMethod.PUT)
  @PUT
  @Path("/additionalRelationshipType")
  @Operation(summary = "Add an additional relationship type and its inverse",
      description = "Add an additional relationship type and its inverse")
  public AdditionalRelationshipType addAdditionalRelationshipType(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "AdditionalRelationship type (and its inverse) to add", required = true) @RequestBody AdditionalRelationshipTypeListJpa addRelTypeList,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Metadata): /additionalRelationshipType");

    final MetadataService metadataService = new MetadataServiceJpa();
    try {
      final String userName =
          authorizeApp(securityService, authToken, "add term type", UserRole.USER);
      metadataService.setLastModifiedBy(userName);

      // add relType and its inverse
      metadataService.setTransactionPerOperation(false);
      metadataService.beginTransaction();
      AdditionalRelationshipType relType1 = addRelTypeList.getObjects().get(0);
      AdditionalRelationshipType relType2 = addRelTypeList.getObjects().get(1);
      relType1.setInverse(null);
      relType2.setInverse(null);
      relType1 = metadataService.addAdditionalRelationshipType(relType1);
      relType2 = metadataService.addAdditionalRelationshipType(relType2);
      relType1.setInverse(relType2);
      metadataService.updateAdditionalRelationshipType(relType1);
      relType2.setInverse(relType1);
      metadataService.updateAdditionalRelationshipType(relType2);
      metadataService.commit();

      return relType1;

    } catch (Exception e) {
      handleException(e, "trying to add term type");
      return null;
    } finally {
      metadataService.close();
      securityService.close();
    }

  }
}
