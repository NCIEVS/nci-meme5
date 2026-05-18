/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.TypeKeyValue;
import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.jpa.model.content.AtomRelationshipJpa;
import com.wci.umls.server.jpa.model.content.ConceptJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.model.helpers.TypeKeyValueJpa;
import com.wci.umls.server.jpa.services.ContentServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.WorkflowServiceJpa;
import com.wci.umls.server.jpa.services.rest.IntegrationTestServiceRest;
import com.wci.umls.server.jpa.model.workflow.WorklistJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.Worklist;
import com.wci.umls.server.services.ContentService;
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
import org.springframework.context.annotation.Conditional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST implementation for {@link IntegrationTestServiceRest}..
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Conditional(NonProdRestCondition.class)
@RequestMapping(value = "/test")
@Path("/test")
@Tag(name = "Integration test", description = "Operations to support integration tests.")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class IntegrationTestServiceRestImpl extends RootServiceRestImpl
    implements IntegrationTestServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link IntegrationTestServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public IntegrationTestServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/add", method = RequestMethod.PUT)
  @PUT
  @Path("/concept/add")
  @Operation(summary = "Add new concept",
      description = "Creates a new concept")
  public Concept addConcept(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Concept, e.g. newConcept", required = true) @RequestBody ConceptJpa concept,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Test): /add " + concept);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "add concept", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      // Add concept
      final Concept newConcept = contentService.addConcept(concept);
      newConcept.setTerminologyId(newConcept.getId().toString());
      contentService.updateConcept(newConcept);
      return newConcept;
    } catch (Exception e) {
      handleException(e, "trying to add a concept");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/update", method = RequestMethod.PUT)
  @PUT
  @Path("/concept/update")
  @Operation(summary = "Update concept",
      description = "Updates the concept")
  public void updateConcept(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Concept, e.g. newConcept", required = true) @RequestBody ConceptJpa concept,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /update " + concept);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "update concept", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      if (concept.getId() == null) {
        throw new Exception(
            "Only a concept that exists can be udpated: " + concept);
      }
      // Load old concept
      final Concept oldConcept = contentService.getConcept(concept.getId());

      // Update fields
      oldConcept.setWorkflowStatus(concept.getWorkflowStatus());

      // Verify no other first order fields changed
      final String oldStr = ConfigUtility.getFirstOrderFieldHash(oldConcept);
      final String newStr = ConfigUtility.getFirstOrderFieldHash(concept);
      if (!oldStr.equals(newStr)) {
        Logger.getLogger(getClass()).error("old = " + oldStr);
        Logger.getLogger(getClass()).error("new = " + newStr);
        throw new Exception("Unexpected attempt to change a first order field");
      }

      // Update concept
      contentService.updateConcept(oldConcept);

    } catch (Exception e) {
      handleException(e, "trying to update a concept");
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /**
   * Removes the concept.
   *
   * @param id the id
   * @param cascade the cascade
   * @param authToken the auth token
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/remove/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/concept/remove/{id}")
  @Operation(summary = "Remove concept",
      description = "Removes the concept with the specified id")
  public void removeConcept(
    @Parameter(description = "Concept id, e.g. 3", required = true) @PathVariable("id") Long id,
    @Parameter(description = "Remove all attached components") @RequestParam(value = "cascade", required = false, defaultValue = "false") boolean cascade,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Test): /remove/" + id);

    final ContentService contentService = new ContentServiceJpa();
    try {
      // Get the project
      Project project = contentService.getProjects().getObjects().get(0);

      String authUser = authorizeApp(securityService, authToken,
          "remove concept", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);
      Concept concept = contentService.getConcept(id);

      if (cascade) {
        for (ConceptRelationship rel : concept.getRelationships()) {
          // Remove relationship and inverse.
          contentService.removeRelationship(rel.getId(), rel.getClass());

          // Remove inverse as well
          for (Relationship<? extends ComponentInfo, ? extends ComponentInfo> inverseRel : contentService
              .getInverseRelationships(project.getTerminology(),
                  project.getVersion(), rel)
              .getObjects()) {
            // TODO - figure out what distinguishing feature is
            if (inverseRel.getTo().getId().equals(rel.getFrom().getId())) {
              contentService.removeRelationship(inverseRel.getId(),
                  rel.getClass());
            }
          }
        }
      }

      // Make copy of the concept, so we can remove all of the components once
      // it's been removed
      Concept copyConcept = new ConceptJpa(concept, false);

      // Create service and configure transaction scope
      contentService.removeConcept(id);

      if (cascade) {
        for (Attribute atr : copyConcept.getAttributes()) {
          contentService.removeAttribute(atr.getId());
        }
        for (Atom a : copyConcept.getAtoms()) {
          contentService.removeAtom(a.getId());
        }

        for (SemanticTypeComponent sty : copyConcept.getSemanticTypes()) {
          contentService.removeSemanticTypeComponent(sty.getId());
        }
      }

    } catch (Exception e) {
      handleException(e, "trying to remove a concept");
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  @Override
  @RequestMapping(value = "/sty/{styId}", method = RequestMethod.GET)
  @GET
  @Path("/sty/{styId}")
  @Operation(summary = "Get a semantic type component",
      description = "Get a semantic type component")
  public SemanticTypeComponent getSemanticTypeComponent(
    @Parameter(description = "Semantic Type Component id, e.g. 1", required = true) @PathVariable("styId") Long styId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Test): /sty/" + styId);

    ContentService contentService = new ContentServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get semantic type component",
          UserRole.ADMINISTRATOR);
      SemanticTypeComponent newSty =
          contentService.getSemanticTypeComponent(styId);
      if (newSty == null) {
        return null;
      } else {
        contentService.getGraphResolutionHandler(ConfigUtility.DEFAULT)
            .resolve(newSty);
        return newSty;
      }
    } catch (Exception e) {

      handleException(e, "trying to get a semantic type component");
    } finally {
      contentService.close();
      securityService.close();
    }
    return null;
  }

  @Override
  @RequestMapping(value = "/concept/relationship/{id}", method = RequestMethod.GET)
  @GET
  @Path("/concept/relationship/{id}")
  @Operation(summary = "Get a concept relationship",
      description = "Get a concept relationship")
  public ConceptRelationship getConceptRelationship(
    @Parameter(description = "Concept Relationship id, e.g. 1", required = true) @PathVariable("id") Long relationshipId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /relationship/" + relationshipId);

    ContentService contentService = new ContentServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get concept relationship",
          UserRole.ADMINISTRATOR);

      ConceptRelationship newRel = (ConceptRelationship) contentService
          .getRelationship(relationshipId, ConceptRelationshipJpa.class);
      if (newRel == null) {
        return null;
      } else {
        contentService.getGraphResolutionHandler(ConfigUtility.DEFAULT)
            .resolve(newRel);
        return newRel;
      }

    } catch (Exception e) {

      handleException(e, "trying to get a concept relationship");
    } finally {
      contentService.close();
      securityService.close();
    }
    return null;
  }

  @Override
  @RequestMapping(value = "/attribute/{attributeId}", method = RequestMethod.GET)
  @GET
  @Path("/attribute/{attributeId}")
  @Operation(summary = "Get an attribute",
      description = "Get an attribute")
  public Attribute getAttribute(
    @Parameter(description = "Attribute id, e.g. 1", required = true) @PathVariable("attributeId") Long attributeId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /attribute/" + attributeId);

    ContentService contentService = new ContentServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get attribute",
          UserRole.ADMINISTRATOR);

      Attribute newAttribute = contentService.getAttribute(attributeId);
      if (newAttribute == null) {
        return null;
      } else {
        // Handle lazy init
        newAttribute.getAlternateTerminologyIds().size();
        return newAttribute;
      }
   } catch (Exception e) {

      handleException(e, "trying to get an attribute");
    } finally {
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/{id}", method = RequestMethod.GET)
  @GET
  @Path("/atom/{id}")
  @Operation(summary = "Get an atom",
      description = "Get an atom")
  public Atom getAtom(
    @Parameter(description = "Atom id, e.g. 1", required = true) @PathVariable("id") Long atomId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Test): /atom/" + atomId);

    ContentService contentService = new ContentServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get atom",
          UserRole.ADMINISTRATOR);

      Atom newAtom = contentService.getAtom(atomId);
      if (newAtom == null) {
        return null;
      } else {
        contentService.getGraphResolutionHandler(ConfigUtility.DEFAULT)
            .resolve(newAtom);
        return newAtom;
      }
    } catch (Exception e) {

      handleException(e, "trying to get an atom");
    } finally {
      contentService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/update", method = RequestMethod.PUT)
  @PUT
  @Path("/atom/update")
  @Operation(summary = "Update atom",
      description = "Updates the atom")
  public void updateAtom(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Atom, e.g. new atom", required = true) @RequestBody AtomJpa atom,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Test): /update " + atom);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "update atom", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      if (atom.getId() == null) {
        throw new Exception(
            "Only a concept that exists can be udpated: " + atom);
      }
      final Atom oldAtom = contentService.getAtom(atom.getId());

      // Apply "atom" changes to "old atom"
      oldAtom.setWorkflowStatus(atom.getWorkflowStatus());

      // Verify no other first order fields changed
      final String oldStr = ConfigUtility.getFirstOrderFieldHash(oldAtom);
      final String newStr = ConfigUtility.getFirstOrderFieldHash(atom);
      if (!oldStr.equals(newStr)) {
        Logger.getLogger(getClass()).error("old = " + oldStr);
        Logger.getLogger(getClass()).error("new = " + newStr);
        throw new Exception("Unexpected attempt to change a first order field");
      }
      // Update atom
      contentService.updateAtom(oldAtom);

    } catch (Exception e) {
      handleException(e, "trying to update a atom");
    } finally {
      contentService.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/relationship/add", method = RequestMethod.PUT)
  @PUT
  @Path("/concept/relationship/add")
  @Operation(summary = "Add new concept relationship",
      description = "Creates a new concept relationship")
  public ConceptRelationship addRelationship(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "a concept relationship", required = true) @RequestBody ConceptRelationshipJpa relationship,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /relationship/add " + relationship);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "add relationship", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      // Add relationship
      final ConceptRelationship newRel =
          (ConceptRelationship) contentService.addRelationship(relationship);
      return newRel;
    } catch (Exception e) {
      handleException(e, "trying to add a relationship");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/relationship/add", method = RequestMethod.PUT)
  @PUT
  @Path("/atom/relationship/add")
  @Operation(summary = "Add new atom relationship",
      description = "Creates a new atom relationship")
  public AtomRelationship addRelationship(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A atom relationship", required = true) @RequestBody AtomRelationshipJpa relationship,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /relationship/add " + relationship);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "add relationship", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      // Add relationship
      final AtomRelationship newRel =
          (AtomRelationship) contentService.addRelationship(relationship);
      return newRel;
    } catch (Exception e) {
      handleException(e, "trying to add a relationship");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/relationship/update", method = RequestMethod.PUT)
  @PUT
  @Path("/concept/relationship/update")
  @Operation(summary = "Update relationship",
      description = "Updates the relationship")
  public void updateRelationship(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "ConceptRelationship", required = true) @RequestBody ConceptRelationshipJpa relationship,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /relationship/update " + relationship);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "update relationship", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      if (relationship.getId() == null) {
        throw new Exception(
            "Only a relationship that exists can be udpated: " + relationship);
      }

      // Load old concept
      final ConceptRelationship oldRel = (ConceptRelationship) contentService
          .getRelationship(relationship.getId(), ConceptRelationshipJpa.class);

      // Update fields
      oldRel.setWorkflowStatus(relationship.getWorkflowStatus());

      // Verify no other first order fields changed
      final String oldStr = ConfigUtility.getFirstOrderFieldHash(oldRel);
      final String newStr = ConfigUtility.getFirstOrderFieldHash(relationship);
      if (!oldStr.equals(newStr)) {
        Logger.getLogger(getClass()).error("old = " + oldStr);
        Logger.getLogger(getClass()).error("new = " + newStr);
        throw new Exception("Unexpected attempt to change a first order field");
      }

      // Update relationship
      contentService.updateRelationship(oldRel);

    } catch (Exception e) {
      handleException(e, "trying to add a relationship");
    } finally {
      contentService.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/worklist/{id}", method = RequestMethod.GET)
  @GET
  @Path("/worklist/{id}")
  @Operation(summary = "Get a worklist",
      description = "Get a worklist")
  public Worklist getWorklist(
    @Parameter(description = "Worklist id, e.g. 1", required = true) @PathVariable("id") Long worklistId,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /worklist/" + worklistId);

    WorkflowService workflowService = new WorkflowServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get worklist", UserRole.USER);
      return workflowService.getWorklist(worklistId);
    } catch (Exception e) {

      handleException(e, "trying to remove a worklist");
    } finally {
      workflowService.close();
      securityService.close();
    }
    return null;
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/typekeyvalue/add", method = RequestMethod.PUT)
  @PUT
  @Path("/typekeyvalue/add")
  @Operation(summary = "Add new TypeKeyValue",
      description = "Creates a new TypeKeyValue")
  public TypeKeyValue addTypeKeyValue(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "TypeKeyValue, e.g. newTypeKeyValue", required = true) @RequestBody TypeKeyValueJpa typeKeyValue,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Test): /add " + typeKeyValue);

    final ContentService contentService = new ContentServiceJpa();
    try {
      final String authUser = authorizeApp(securityService, authToken,
          "add typeKeyValue", UserRole.ADMINISTRATOR);
      contentService.setLastModifiedBy(authUser);
      contentService.setMolecularActionFlag(false);

      // Add TypeKeyValue
      return contentService.addTypeKeyValue(typeKeyValue);
    } catch (Exception e) {
      handleException(e, "trying to add a typeKeyValue");
      return null;
    } finally {
      contentService.close();
      securityService.close();
    }

  }

}
