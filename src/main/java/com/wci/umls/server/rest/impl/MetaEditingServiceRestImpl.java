/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.model.actions.ChangeEventJpa;
import com.wci.umls.server.jpa.algo.action.AddAtomMolecularAction;
import com.wci.umls.server.jpa.algo.action.AddAttributeMolecularAction;
import com.wci.umls.server.jpa.algo.action.AddDemotionMolecularAction;
import com.wci.umls.server.jpa.algo.action.AddRelationshipMolecularAction;
import com.wci.umls.server.jpa.algo.action.AddSemanticTypeMolecularAction;
import com.wci.umls.server.jpa.algo.action.ApproveMolecularAction;
import com.wci.umls.server.jpa.algo.action.MergeMolecularAction;
import com.wci.umls.server.jpa.algo.action.MoveMolecularAction;
import com.wci.umls.server.jpa.algo.action.RedoMolecularAction;
import com.wci.umls.server.jpa.algo.action.RemoveAtomMolecularAction;
import com.wci.umls.server.jpa.algo.action.RemoveAttributeMolecularAction;
import com.wci.umls.server.jpa.algo.action.RemoveRelationshipMolecularAction;
import com.wci.umls.server.jpa.algo.action.RemoveSemanticTypeMolecularAction;
import com.wci.umls.server.jpa.algo.action.SplitMolecularAction;
import com.wci.umls.server.jpa.algo.action.UndoMolecularAction;
import com.wci.umls.server.jpa.algo.action.UpdateAtomMolecularAction;
import com.wci.umls.server.jpa.model.content.AtomJpa;
import com.wci.umls.server.jpa.model.content.AttributeJpa;
import com.wci.umls.server.jpa.model.content.ConceptRelationshipJpa;
import com.wci.umls.server.jpa.model.content.SemanticTypeComponentJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.rest.MetaEditingServiceRest;
import com.wci.umls.server.model.actions.ChangeEvent;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptRelationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.workflow.WorkflowStatus;
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
 * REST implementation for {@link MetaEditingServiceRest}..
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/meta")
@Path("/meta")
// TODO: consider renaming this to "MetathesaurusRestImpl" vs "Authoring API"
@Tag(name = "Meta Editing", description = "Operations to support metathesaurus editing.")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class MetaEditingServiceRestImpl extends RootServiceRestImpl
    implements MetaEditingServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link MetaEditingServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public MetaEditingServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/sty/add", method = RequestMethod.POST)
  @POST
  @Path("/sty/add")
  @Operation(summary = "Add semantic type to concept",
      description = "Add semantic type to concept on a project branch")
  public ValidationResult addSemanticType(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Semantic type to add", required = true) @RequestParam(value = "semanticType", required = false) String semanticTypeValue,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /sty/add " + projectId + ","
            + conceptId + " for user " + authToken + " with sty value "
            + semanticTypeValue);

    // Instantiate services
    final AddSemanticTypeMolecularAction action =
        new AddSemanticTypeMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName =
          authorizeProject(action, projectId, securityService, authToken,
              "adding a semantic type", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Create semantic type component
      final SemanticTypeComponent sty = new SemanticTypeComponentJpa();
      sty.setTerminologyId("");
      sty.setObsolete(false);
      sty.setPublishable(true);
      sty.setPublished(false);
      sty.setWorkflowStatus(WorkflowStatus.PUBLISHED);
      sty.setSemanticType(semanticTypeValue);
      sty.setTerminology(project.getTerminology());
      sty.setVersion(project.getVersion());
      sty.setTimestamp(new Date());

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setSemanticTypeComponent(sty);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.SEMANTIC_TYPE.toString(),
          action.getSemanticTypeComponent().getId(), action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "adding a semantic type");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/sty/remove/{id}", method = RequestMethod.POST)
  @POST
  @Path("/sty/remove/{id}")
  @Operation(summary = "Remove semantic type from concept",
      description = "Remove semantic type from concept on a project branch")
  public ValidationResult removeSemanticType(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, in ms ", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Semantic type id, e.g. 3", required = true) @PathVariable("id") Long semanticTypeComponentId,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /sty/remove " + projectId + ","
            + conceptId + " for user " + authToken + " with id "
            + semanticTypeComponentId);

    // Instantiate services
    final RemoveSemanticTypeMolecularAction action =
        new RemoveSemanticTypeMolecularAction();
    try {
      // Authorize project role, get userName
      final String userName =
          authorizeProject(action, projectId, securityService, authToken,
              "removing a semantic type", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setSemanticTypeComponentId(semanticTypeComponentId);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.SEMANTIC_TYPE.toString(),
          action.getSemanticTypeComponent().getId(), action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "removing a semantic type");
      return null;
    } finally {
      action.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attribute/add", method = RequestMethod.POST)
  @POST
  @Path("/attribute/add")
  @Operation(summary = "Add attribute to concept",
      description = "Add attribute to concept on a project branch")
  public ValidationResult addAttribute(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Attribute to add", required = true) @RequestBody AttributeJpa attribute,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /attribute/add " + projectId + ","
            + conceptId + " for user " + authToken + " with attribute value "
            + attribute.getName());

    // Instantiate services
    final AddAttributeMolecularAction action =
        new AddAttributeMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "adding an attribute", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // All new content is unpublished and publishable
      attribute.setPublished(false);
      attribute.setPublishable(true);

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAttribute(attribute);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.ATTRIBUTE.toString(), action.getAttribute().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }

      handleException(e, "adding an attribute");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/attribute/remove/{id}", method = RequestMethod.POST)
  @POST
  @Path("/attribute/remove/{id}")
  @Operation(summary = "Remove attribute from concept",
      description = "Remove attribute from concept on a project branch")
  public ValidationResult removeAttribute(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, in ms ", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Attribute id, e.g. 3", required = true) @PathVariable("id") Long attributeId,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /attribute/remove " + projectId + ","
            + conceptId + " for user " + authToken + " with id " + attributeId);

    // Instantiate services
    final RemoveAttributeMolecularAction action =
        new RemoveAttributeMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "removing an attribute", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAttributeId(attributeId);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.ATTRIBUTE.toString(), action.getAttribute().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "removing an attribute");
      return null;
    } finally {
      action.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/add", method = RequestMethod.POST)
  @POST
  @Path("/atom/add")
  @Operation(summary = "Add atom to concept",
      description = "Add atom to concept on a project branch")
  public ValidationResult addAtom(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Atom to add", required = true) @RequestBody AtomJpa atom,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /atom/add " + projectId + ","
            + conceptId + " for user " + authToken + " with atom value "
            + atom.getName());

    // Instantiate services
    final AddAtomMolecularAction action = new AddAtomMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "adding an atom", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // All new content is unpublished and publishable
      atom.setPublished(false);
      atom.setPublishable(true);

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAtom(atom);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("adding an atom", authToken,
          IdType.ATOM.toString(), action.getAtom().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "adding an atom");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/remove/{id}", method = RequestMethod.POST)
  @POST
  @Path("/atom/remove/{id}")
  @Operation(summary = "Remove atom from concept",
      description = "Remove atom from concept on a project branch")
  public ValidationResult removeAtom(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, in ms ", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Atom id, e.g. 3", required = true) @PathVariable("id") Long atomId,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /atom/remove " + projectId + ","
            + conceptId + " remove for user " + authToken + " with id "
            + atomId);

    // Instantiate services
    final RemoveAtomMolecularAction action = new RemoveAtomMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "removing an atom", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAtomId(atomId);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.ATTRIBUTE.toString(), action.getAtom().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "removing an atom");
      return null;
    } finally {
      action.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/update", method = RequestMethod.POST)
  @POST
  @Path("/atom/update")
  @Operation(summary = "Update an atom",
      description = "Update an atom on a project branch")
  public ValidationResult updateAtom(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Atom to add", required = true) @RequestBody AtomJpa atom,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /atom/update " + projectId
            + ", for user " + authToken + " with atom value " + atom.getName());

    // Instantiate services
    final UpdateAtomMolecularAction action = new UpdateAtomMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "updating an atom", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAtom(atom);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa("updating an atom",
          authToken, IdType.ATOM.toString(), action.getAtom().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "updating an atom");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationship/add", method = RequestMethod.POST)
  @POST
  @Path("/relationship/add")
  @Operation(summary = "Add relationship to concept",
      description = "Add relationship to concept on a project branch")
  public ValidationResult addRelationship(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Relationship to add", required = true) @RequestBody ConceptRelationshipJpa relationship,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /relationship/add " + projectId + ","
            + conceptId + " for user " + authToken + " with relationship value "
            + relationship);

    // Instantiate services
    final AddRelationshipMolecularAction action =
        new AddRelationshipMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "adding a relationship", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // All new content is unpublished and publishable
      relationship.setPublished(false);
      if (relationship.getRelationshipType().equals("XR")) {
        relationship.setPublishable(false);
      } else {
        relationship.setPublishable(true);
      }
      // Set defaults for a concept level relationship
      relationship.setStated(true);
      relationship.setInferred(true);
      relationship.setSuppressible(false);
      relationship.setObsolete(false);
      
      // If RelGroup is null, set to blank
      if (relationship.getGroup() == null){
        relationship.setGroup("");
      }
      
      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      // The relationship is FROM conceptId -> conceptId2, and REL
      // is represented in that direction
      action.setConceptId(conceptId);
      action.setConceptId2(relationship.getTo().getId());
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setRelationship(relationship);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.RELATIONSHIP.toString(), action.getRelationship().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "adding a relationship");
      return null;
    } finally {
      action.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationships/add", method = RequestMethod.POST)
  @POST
  @Path("/relationships/add")
  @Operation(summary = "Add relationships to concept",
      description = "Add relationships to concept on a project branch")
  public ValidationResult addRelationships(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Relationships to add", required = true) @RequestBody List<ConceptRelationshipJpa> relationships,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /relationships/add " + projectId
            + "," + conceptId + " for user " + authToken
            + " with relationships = " + relationships);

    // Pre-create all of the vars that will be used across all added
    // relationships
    String userName = null;
    Project project = null;
    Long innerLastModified = lastModified;
    final ValidationResult allValidationResults = new ValidationResultJpa();

    // Create seperate molecular actions for each relationship to be added
    for (ConceptRelationship relationship : relationships) {

      // Instantiate services
      final AddRelationshipMolecularAction action =
          new AddRelationshipMolecularAction();

      try {

        // Authorize project role, get userName (first time only)
        if (userName == null) {
          userName = authorizeProject(action, projectId, securityService,
              authToken, "adding a relationship", UserRole.AUTHOR);
        }

        // Retrieve the project (first time only)
        if (project == null) {
          project = action.getProject(projectId);
          if (!project.isEditingEnabled()) {
            throw new LocalException(
                "Editing is disabled on project: " + project.getName());
          }
        }

        // All new content is unpublished and publishable
        relationship.setPublished(false);
        if (relationship.getRelationshipType().equals("XR")) {
          relationship.setPublishable(false);
        } else {
          relationship.setPublishable(true);
        }
        // Set defaults for a concept level relationship
        relationship.setStated(true);
        relationship.setInferred(true);
        relationship.setSuppressible(false);
        relationship.setObsolete(false);
        
        // If RelGroup is null, set to blank
        if (relationship.getGroup() == null){
          relationship.setGroup("");
        }

        // Configure the action
        action.setProject(project);
        action.setActivityId(activityId);
        // The relationship is FROM conceptId -> conceptId2, and REL
        // is represented in that direction
        action.setConceptId(conceptId);
        action.setConceptId2(relationship.getTo().getId());
        action.setLastModifiedBy("E-" + userName);
        action.setLastModified(innerLastModified);
        action.setOverrideWarnings(overrideWarnings);
        action.setTransactionPerOperation(false);
        action.setMolecularActionFlag(true);
        action.setChangeStatusFlag(true);

        action.setRelationship(relationship);

        // Perform the action
        final ValidationResult validationResult =
            action.performMolecularAction(action, userName, true, false);

        // Add all of the errors/warnings/comments to allValidationResults
        for (String error : validationResult.getErrors()) {
          allValidationResults.getErrors().add(error);
        }
        for (String warning : validationResult.getWarnings()) {
          allValidationResults.getWarnings().add(warning);
        }
        for (String comment : validationResult.getComments()) {
          allValidationResults.getComments().add(comment);
        }

        // If this action failed, bail out here.
        if (!validationResult.isValid() || (!overrideWarnings
            && validationResult.getWarnings().size() > 0)) {
          return allValidationResults;
        }

        // Websocket notification
        final ChangeEvent event = new ChangeEventJpa(action.getName(),
            authToken, IdType.RELATIONSHIP.toString(),
            action.getRelationship().getId(), action.getConcept());
        sendChangeEvent(userName, event);

        // Reload the fromConcept for next pass
        Concept fromConcept = action.getConcept(conceptId);
        innerLastModified = fromConcept.getLastModified().getTime();

      } catch (Exception e) {
        try {
          action.rollback();
        } catch (Exception e2) {
          // do nothing
        }
        handleException(e, "adding relationships");
        return allValidationResults;
      } finally {
        action.close();
        securityService.close();
      }
    }

    return allValidationResults;

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/demotion/add", method = RequestMethod.POST)
  @POST
  @Path("/demotion/add")
  @Operation(summary = "Add demotion between atoms",
      description = "Add demotion between atoms on a project branch")
  public ValidationResult addDemotion(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "From Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "From Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "To Concept id, e.g. 3", required = true) @RequestParam(value = "conceptId2", required = false) Long conceptId2,
    @Parameter(description = "From Atom id, e.g. 3", required = true) @RequestParam(value = "atomId", required = false) Long atomId,
    @Parameter(description = "To Atom id, e.g. 3", required = true) @RequestParam(value = "atomId2", required = false) Long atomId2,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /demotion/add " + projectId + ","
            + conceptId + " from atom " + atomId + " to atom " + atomId2
            + " for user " + authToken);

    // Instantiate services
    final AddDemotionMolecularAction action = new AddDemotionMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "adding a demotion", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setConceptId2(conceptId2);
      action.setAtomId(atomId);
      action.setAtomId2(atomId2);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.RELATIONSHIP.toString(),
          action.getDemotionRelationship().getId(), action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "adding a demotion");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/relationship/remove/{id}", method = RequestMethod.POST)
  @POST
  @Path("/relationship/remove/{id}")
  @Operation(summary = "Remove relationship from concept",
      description = "Remove relationship from concept on a project branch")
  public ValidationResult removeRelationship(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, in ms ", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Relationship id, e.g. 3", required = true) @PathVariable("id") Long relationshipId,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /relationship/remove " + projectId
            + "," + conceptId + " for user " + authToken + " with id "
            + relationshipId);

    // Instantiate services
    final RemoveRelationshipMolecularAction action =
        new RemoveRelationshipMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName =
          authorizeProject(action, projectId, securityService, authToken,
              "removing a relationship", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Look up second conceptId.
      final Long conceptId2 =
          action.getRelationship(relationshipId, ConceptRelationshipJpa.class)
              .getTo().getId();

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(conceptId2);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setRelationshipId(relationshipId);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }
      // Websocket notification
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.RELATIONSHIP.toString(), action.getRelationship().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "removing a relationship");
      return null;
    } finally {
      action.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/merge", method = RequestMethod.POST)
  @POST
  @Path("/concept/merge")
  @Operation(summary = "Merge concepts together",
      description = "Merge concepts together on a project branch")
  public ValidationResult mergeConcepts(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "From Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "From Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "To Concept id, e.g. 3", required = true) @RequestParam(value = "conceptId2", required = false) Long conceptId2,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /concept/merge " + projectId + ","
            + conceptId + " with concept " + conceptId2 + " for user "
            + authToken);

    // Instantiate services
    final MergeMolecularAction action = new MergeMolecularAction();

    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "merging concepts", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(conceptId2);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification - one for the updating of the toConcept, and one
      // for the deletion of the fromConcept

      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getToConcept().getId(),
          action.getToConcept());
      final ChangeEvent event2 = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getFromConcept().getId(),
          action.getFromConcept());
      sendChangeEvents(userName, event2, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "merging concepts");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/atom/move", method = RequestMethod.POST)
  @POST
  @Path("/atom/move")
  @Operation(summary = "Move atoms from concept to concept",
      description = "Move atoms from concept to concept on a project branch")
  public ValidationResult moveAtoms(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "From Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "From Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "To Concept id, e.g. 3", required = true) @RequestParam(value = "conceptId2", required = false) Long conceptId2,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Atoms to move", required = true) @RequestBody List<Long> atomIds,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /atom/move " + projectId + ","
            + conceptId + " move atoms for user " + authToken + " to concept "
            + conceptId2);

    // Instantiate services
    final MoveMolecularAction action = new MoveMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "moving atoms", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(conceptId2);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAtomIds(atomIds);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification - one each for the updating the from and
      // toConcept
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getToConcept().getId(),
          action.getToConcept());
      final ChangeEvent event2 = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getFromConcept().getId(),
          action.getFromConcept());
      sendChangeEvents(userName, event, event2);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "moving atoms");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/split", method = RequestMethod.POST)
  @POST
  @Path("/concept/split")
  @Operation(summary = "Split concept into two",
      description = "Split concept into two on a project branch")
  public ValidationResult splitConcept(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Atoms to move", required = true) @RequestBody List<Long> atomIds,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(description = "Copy relationships") @RequestParam(value = "copyRelationships", required = false, defaultValue = "false") boolean copyRelationships,
    @Parameter(description = "Copy semantic types") @RequestParam(value = "copySemanticTypes", required = false, defaultValue = "false") boolean copySemanticTypes,
    @Parameter(description = "Relationship to new concept", required = true) @RequestParam(value = "relationshipType", required = false) String relationshipType,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /concept/split " + projectId + ","
            + conceptId + " for user " + authToken);

    // Instantiate services
    final SplitMolecularAction action = new SplitMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "splitting concept", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      action.setAtomIds(atomIds);
      action.setRelationshipType(relationshipType);
      action.setCopyRelationships(copyRelationships);
      action.setCopySemanticTypes(copySemanticTypes);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification - one for the updating of the originating
      // Concept, and one
      // for the created Concept
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getFromConcept().getId(),
          action.getFromConcept());

      final ChangeEvent event2 = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getToConcept().getId(),
          action.getToConcept());
      sendChangeEvents(userName, event, event2);

      // Surface the new concept id so the client can add it to the concept list
      validationResult.getComments().add("newConceptId:" + action.getToConcept().getId());
      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "splitting concept");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/concept/approve", method = RequestMethod.POST)
  @POST
  @Path("/concept/approve")
  @Operation(summary = "Approve concept",
      description = "Approve concept on a project branch")
  public ValidationResult approveConcept(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Concept id, e.g. 2", required = true) @RequestParam(value = "conceptId", required = false) Long conceptId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Concept lastModified, as date", required = true) @RequestParam(value = "lastModified", required = false) Long lastModified,
    @Parameter(description = "Override warnings") @RequestParam(value = "overrideWarnings", required = false, defaultValue = "false") boolean overrideWarnings,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /concept/approve " + projectId + ","
            + conceptId + " for user " + authToken);

    // Instantiate services
    final ApproveMolecularAction action = new ApproveMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "approving concept", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(null);
      action.setLastModifiedBy("E-" + userName);
      action.setLastModified(lastModified);
      action.setOverrideWarnings(overrideWarnings);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(true);
      action.setChangeStatusFlag(true);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.isValid()
          || (!overrideWarnings && validationResult.getWarnings().size() > 0)) {
        return validationResult;
      }

      // Websocket notification - one for the updating of the toConcept, and one
      // for the deletion of the fromConcept
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), action.getConcept().getId(),
          action.getConcept());
      sendChangeEvent(userName, event);

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "approving concept");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/action/undo", method = RequestMethod.POST)
  @POST
  @Path("/action/undo")
  @Operation(summary = "Undo action",
      description = "Undo a previously performed action")
  public ValidationResult undoAction(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Molecular Action id, e.g. 2", required = true) @RequestParam(value = "molecularActionId", required = false) Long molecularActionId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Force action") @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /action/undo " + projectId
            + ", undo action with id " + molecularActionId + " for user "
            + authToken);

    // Instantiate services
    final UndoMolecularAction action = new UndoMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "undoing action", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Note - the undo action doesn't create its own molecular and atomic
      // actions

      // Get concepts
      final Long conceptId =
          action.getMolecularAction(molecularActionId).getComponentId();
      final Long conceptId2 =
          action.getMolecularAction(molecularActionId).getComponentId2();
      Concept concept = action.getConcept(conceptId);
      Concept concept2 = action.getConcept(conceptId2);

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(conceptId2);
      action.setLastModifiedBy("E-" + userName);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(false);
      action.setChangeStatusFlag(true);

      action.setMolecularActionId(molecularActionId);
      action.setForce(force);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.getErrors().isEmpty()) {
        return validationResult;
      }

      // Reread concepts in case they were null before the action
      if (concept == null) {
        concept = action.getConcept(conceptId);
      }
      if (concept2 == null) {
        concept2 = action.getConcept(conceptId2);
      }
      // Websocket notification
      final List<ChangeEvent> events = new ArrayList<>();
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), null, concept);
      events.add(event);

      if (action.getMolecularAction(molecularActionId)
          .getComponentId2() != null) {
        final ChangeEvent event2 = new ChangeEventJpa(action.getName(),
            authToken, IdType.CONCEPT.toString(), null, concept2);
        events.add(event2);
      }
      sendChangeEvents(userName, events.toArray(new ChangeEvent[] {}));

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "undoing action");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/action/redo", method = RequestMethod.POST)
  @POST
  @Path("/action/redo")
  @Operation(summary = "Redo action",
      description = "Redo a previously undone action")
  public ValidationResult redoAction(
    @Parameter(description = "Project id, e.g. 1", required = true) @RequestParam(value = "projectId", required = false) Long projectId,
    @Parameter(description = "Molecular Action id, e.g. 2", required = true) @RequestParam(value = "molecularActionId", required = false) Long molecularActionId,
    @Parameter(description = "Activity id, e.g. wrk16a_demotions_001", required = true) @RequestParam(value = "activityId", required = false) String activityId,
    @Parameter(description = "Force action") @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
    @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .info("RESTful call (MetaEditing): /action/redo " + projectId
            + ", redo action with id " + molecularActionId + " for user "
            + authToken);

    // Instantiate services
    final RedoMolecularAction action = new RedoMolecularAction();
    try {

      // Authorize project role, get userName
      final String userName = authorizeProject(action, projectId,
          securityService, authToken, "undoing action", UserRole.AUTHOR);

      // Retrieve the project
      final Project project = action.getProject(projectId);
      if (!project.isEditingEnabled()) {
        throw new LocalException(
            "Editing is disabled on project: " + project.getName());
      }

      // Note - the redo action doesn't create its own molecular and atomic
      // actions

      // Get concepts
      final Long conceptId =
          action.getMolecularAction(molecularActionId).getComponentId();
      final Long conceptId2 =
          action.getMolecularAction(molecularActionId).getComponentId2();
      Concept concept = action.getConcept(conceptId);
      Concept concept2 = action.getConcept(conceptId2);

      // Configure the action
      action.setProject(project);
      action.setActivityId(activityId);
      action.setConceptId(conceptId);
      action.setConceptId2(conceptId2);
      action.setLastModifiedBy("E-" + userName);
      action.setTransactionPerOperation(false);
      action.setMolecularActionFlag(false);
      action.setChangeStatusFlag(true);

      action.setMolecularActionId(molecularActionId);
      action.setForce(force);

      // Perform the action
      final ValidationResult validationResult =
          action.performMolecularAction(action, userName, true, false);

      // If the action failed, bail out now.
      if (!validationResult.getErrors().isEmpty()) {
        return validationResult;
      }

      // Reread concepts in case they were null before the action
      if (concept == null) {
        concept = action.getConcept(conceptId);
      }
      if (concept2 == null) {
        concept2 = action.getConcept(conceptId2);
      }

      // Websocket notification
      final List<ChangeEvent> events = new ArrayList<>();
      final ChangeEvent event = new ChangeEventJpa(action.getName(), authToken,
          IdType.CONCEPT.toString(), null, concept);
      events.add(event);

      if (action.getMolecularAction(molecularActionId)
          .getComponentId2() != null) {
        final ChangeEvent event2 = new ChangeEventJpa(action.getName(),
            authToken, IdType.CONCEPT.toString(), null, concept2);
        events.add(event2);
      }
      sendChangeEvents(userName, events.toArray(new ChangeEvent[] {}));

      return validationResult;

    } catch (Exception e) {
      try {
        action.rollback();
      } catch (Exception e2) {
        // do nothing
      }
      handleException(e, "undoing action");
      return null;
    } finally {
      action.close();
      securityService.close();
    }

  }

}
