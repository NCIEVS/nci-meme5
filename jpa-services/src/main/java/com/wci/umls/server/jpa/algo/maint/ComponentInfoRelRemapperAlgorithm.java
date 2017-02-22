/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ComponentInfo;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.ComponentInfoRelationshipJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ComponentInfoRelationship;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.meta.Terminology;

/**
 * Algorithm responsible for re-mapping component info relationships.
 */
public class ComponentInfoRelRemapperAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /** The update count. */
  private int updateCount = 0;

  /**
   * Instantiates an empty {@link ComponentInfoRelRemapperAlgorithm}.
   *
   * @throws Exception the exception
   */
  public ComponentInfoRelRemapperAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("COMPONENTINFORELREMAPPER");
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception(getName() + " requires a project to be set");
    }

    return validationResult;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Cache the current terminologies
    final Map<String, String> currentTerminologyVersions = new HashMap<>();
    for (final Terminology terminology : getCurrentTerminologies()
        .getObjects()) {
      currentTerminologyVersions.put(terminology.getTerminology(),
          terminology.getVersion());
    }

    // Create a cache for to/from components that are updated by this algorithm
    final Set<Component> updatedComponents = new HashSet<>();

    // Find all publishable component info relationships
    final String query = "SELECT r.id FROM ComponentInfoRelationshipJpa r "
        + "WHERE r.publishable=true";

    final List<Long> componentInfoRelIds = executeSingleComponentIdQuery(query,
        QueryType.JQL, getDefaultQueryParams(getProject()),
        ComponentInfoRelationshipJpa.class,false);

    setSteps(componentInfoRelIds.size());

    for (final Long id : componentInfoRelIds) {
      final ComponentInfoRelationship componentInfoRelationship =
          (ComponentInfoRelationship) getRelationship(id,
              ComponentInfoRelationshipJpa.class);
      boolean relNeedsUpdating = false;

      // Get from component
      final ComponentInfo fromComponentInfo =
          componentInfoRelationship.getFrom();
      final Component fromComponent =
          getComponent(getType(componentInfoRelationship.getFrom().getType()),
              fromComponentInfo.getTerminologyId(),
              fromComponentInfo.getTerminology(), null);

      // If from component doesn't exist, mark the relationship as unpublishable
      // (and warn)
      if (fromComponent == null) {
        componentInfoRelationship.setPublishable(false);
        logWarn(
            "WARNING - from component cannot be found for component info relationship = "
                + componentInfoRelationship);
        updateProgress();
        continue;
      }

      // If a previous step already updated this component (e.g. from this
      // relationship's inverse), update the relationship
      if (updatedComponents.contains(fromComponent)) {
        componentInfoRelationship.setFrom(fromComponent);
        relNeedsUpdating = true;
      }

      // If from component exists and the terminology is not the current
      // version, make it the current version (and updateRelationship)
      else if (!fromComponent.getVersion().equals(
          currentTerminologyVersions.get(fromComponent.getTerminology()))) {
        fromComponent.setVersion(
            currentTerminologyVersions.get(fromComponent.getTerminology()));
        updateComponent(fromComponent);

        // Handle ComponentInfoRelationship atom components
        // Change terminology and version from atom's to project's
        if (fromComponent instanceof Atom) {
          fromComponent.setTerminology(getProject().getTerminology());
          fromComponent.setVersion(getProject().getVersion());
        }
        
        updatedComponents.add(fromComponent);

        componentInfoRelationship.setFrom(fromComponent);
        relNeedsUpdating = true;
      }

      // Get to object
      final ComponentInfo toComponentInfo = componentInfoRelationship.getTo();
      final Component toComponent =
          getComponent(getType(toComponentInfo.getType()),
              toComponentInfo.getTerminologyId(),
              toComponentInfo.getTerminology(), null);

      // If to component doesn't exist, mark the relationship as unpublishable
      // (and warn)
      if (toComponent == null) {
        componentInfoRelationship.setPublishable(false);
        logWarn(
            "WARNING - to component cannot be found for component info relationship = "
                + componentInfoRelationship);
        updateProgress();
        continue;
      }

      // If a previous step already updated this component (e.g. from this
      // relationship's inverse), update the relationship
      if (updatedComponents.contains(toComponent)) {
        componentInfoRelationship.setTo(toComponent);
        relNeedsUpdating = true;
      }

      // If to component exists and the terminology is not the current
      // version, make it the current version (and updateRelationship)
      else if (!toComponent.getVersion().equals(
          currentTerminologyVersions.get(toComponent.getTerminology()))) {
        toComponent.setVersion(
            currentTerminologyVersions.get(toComponent.getTerminology()));
        updateComponent(toComponent);
        
        // Handle ComponentInfoRelationship atom components
        // Change terminology and version from atom's to project's
        if (toComponent instanceof Atom) {
          toComponent.setTerminology(getProject().getTerminology());
          toComponent.setVersion(getProject().getVersion());
        }
        
        updatedComponents.add(toComponent);

        componentInfoRelationship.setTo(toComponent);
        relNeedsUpdating = true;
      }

      // Update the relationship, if needed
      if (relNeedsUpdating) {
        updateRelationship(componentInfoRelationship);
        updateCount++;
      }

      updateProgress();
    }

    // Always clean up after yourself...
    clearCaches();

    commitClearBegin();

    logInfo("  updated count = " + updateCount);
    logInfo("Finished " + getName());
  }

  /**
   * Returns the type.
   *
   * @param idType the id type
   * @return the type
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private String getType(IdType idType) throws Exception {
    final String stringType;
    switch (idType) {
      case ATOM:
        stringType = "AUI";
        break;
      case DESCRIPTOR:
        stringType = "SOURCE_DUI";
        break;
      case CODE:
        stringType = "CODE_SOURCE";
        break;
      case CONCEPT:
        stringType = "SOURCE_CUI";
        break;
      default:
        throw new Exception("ERROR - Type not handled = " + idType);
    }
    return stringType;
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a - No reset
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Remaps component info relationships";
  }
}