/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.Relationship;
import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * Implementation of an algorithm to execute an action based on a user-defined
 * query.
 */
public class QueryActionAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  /** The object type class. */
  @SuppressWarnings("rawtypes")
  private Class objectTypeClass;

  /** The query type. */
  private QueryType queryType;

  /** The query. */
  private String query;

  /** The action. */
  private String action;

  /** The successful actions. */
  private int successfulActions;

  /**
   * Instantiates an empty {@link QueryActionAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public QueryActionAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("QUERYACTION");
    setLastModifiedBy("admin");
  }

  /**
   * Sets the object type class.
   *
   * @param objectTypeClass the object type class
   */
  @SuppressWarnings("rawtypes")
  public void setObjectTypeClass(Class objectTypeClass) {
    this.objectTypeClass = objectTypeClass;
  }

  /**
   * Sets the query type.
   *
   * @param queryType the query type
   */
  public void setQueryType(QueryType queryType) {
    this.queryType = queryType;
  }

  /**
   * Sets the query.
   *
   * @param query the query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Sets the action.
   *
   * @param action the action
   */
  public void setAction(String action) {
    this.action = action;
  }

  /**
   * Check preconditions.
   *
   * @return the validation result
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    ValidationResult validationResult = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception(
          "Query Action algorithms requires a project to be set");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings({
      "unchecked",
  })
  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    logInfo("  action = " + action);
    logInfo("  objectTypeClass = "
        + ConfigUtility.getNameFromClass(objectTypeClass));

    // No Molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    logInfo("[QueryAction]: " + action + " "
        + ConfigUtility.getNameFromClass(objectTypeClass) + " for results of "
        + queryType.toString() + " query: " + query);
    commitClearBegin();

    // Count number of actions successfully performed
    successfulActions = 0;

    // Generate parameters to pass into query executions
    Map<String, String> params = new HashMap<>();
    params.put("terminology", getTerminology());
    params.put("version", getVersion());
    params.put("projectTerminology", getProject().getTerminology());
    params.put("projectVersion", getProject().getVersion());

    // Execute query to get component Ids
    List<Long> componentIds = executeSingleComponentIdQuery(query, queryType,
        params, objectTypeClass, false);

    setSteps(componentIds.size());

    for (Long componentId : componentIds) {
      checkCancel();

      // Handle field-change actions
      if (!action.equals("Remove Demotions")) {
        // Load the component
        Component component = getComponent(componentId, objectTypeClass);
        Boolean componentChanged = false;

        // Handle Make Publishable
        if (action.equals("Make Publishable")) {
          if (!component.isPublishable()) {
            component.setPublishable(true);
            componentChanged = true;
          }
        }

        // Handle Make Unpublishable
        else if (action.equals("Make Unpublishable")) {
          if (component.isPublishable()) {
            component.setPublishable(false);
            componentChanged = true;
          }
        }

        // Handle Approve
        else if (action.equals("Approve")) {
          componentChanged = changeComponentApprovalStatus(component,
              WorkflowStatus.READY_FOR_PUBLICATION);
        }

        // Handle Unapprove
        else if (action.equals("Unapprove")) {
          componentChanged = changeComponentApprovalStatus(component,
              WorkflowStatus.NEEDS_REVIEW);
        }

        if (componentChanged) {
          successfulActions++;
        }
      }

      // Handle the remove-demotion action
      else {
        AtomRelationship atomRelationship =
            (AtomRelationship) getRelationship(componentId, objectTypeClass);

        Atom fromAtom = atomRelationship.getFrom();
        Atom toAtom = atomRelationship.getTo();
        AtomRelationship inverseAtomRelationship =
            (AtomRelationship) getInverseRelationship(
                getProject().getTerminology(), getProject().getVersion(),
                atomRelationship);

        fromAtom.getRelationships().remove(atomRelationship);
        toAtom.getRelationships().remove(inverseAtomRelationship);

        updateAtom(fromAtom);
        updateAtom(toAtom);

        removeRelationship(atomRelationship.getId(), AtomRelationship.class);
        removeRelationship(inverseAtomRelationship.getId(),
            AtomRelationship.class);
        successfulActions++;
      }
      // Update the progress
      updateProgress();
    }

    commitClearBegin();

    logInfo("[QueryAction] " + successfulActions + " " + action + " "
        + ConfigUtility.getNameFromClass(objectTypeClass)
        + " actions successfully performed.");

    logInfo("  project = " + getProject().getId());
    logInfo("  workId = " + getWorkId());
    logInfo("  activityId = " + getActivityId());
    logInfo("  user  = " + getLastModifiedBy());
    logInfo("Finished " + getName());

  }

  /**
   * Change component approval status.
   *
   * @param component the component
   * @param approvalStatus the approval status
   * @return the boolean
   * @throws Exception the exception
   */
  @SuppressWarnings("rawtypes")
  private Boolean changeComponentApprovalStatus(Component component,
    WorkflowStatus approvalStatus) throws Exception {

    if (component instanceof AtomClass) {
      final AtomClass castComponent = (AtomClass) component;
      if (!castComponent.getWorkflowStatus().equals(approvalStatus)) {
        castComponent.setWorkflowStatus(approvalStatus);
        return true;
      } else {
        return false;
      }
    }

    if (component instanceof Atom) {
      final Atom castComponent = (Atom) component;
      if (!castComponent.getWorkflowStatus().equals(approvalStatus)) {
        castComponent.setWorkflowStatus(approvalStatus);
        return true;
      } else {
        return false;
      }
    }

    if (component instanceof SemanticTypeComponent) {
      final SemanticTypeComponent castComponent =
          (SemanticTypeComponent) component;
      if (!castComponent.getWorkflowStatus().equals(approvalStatus)) {
        castComponent.setWorkflowStatus(approvalStatus);
        return true;
      } else {
        return false;
      }
    }

    if (component instanceof Relationship) {
      final Relationship castComponent = (Relationship) component;
      if (!castComponent.getWorkflowStatus().equals(approvalStatus)) {
        castComponent.setWorkflowStatus(approvalStatus);
        return true;
      } else {
        return false;
      }
    }

    throw new Exception("Approve/Unapprove for object " + component
        + " failed - approving/unapproving objects of type " + objectTypeClass
        + " is unhandled.");
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
    checkRequiredProperties(new String[] {
        "queryType", "query", "objectType", "action"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("objectType") != null) {
      String componentPath = AtomJpa.class.getName().substring(0,
          AtomJpa.class.getName().indexOf("AtomJpa"));
      objectTypeClass =
          Class.forName(componentPath + p.getProperty("objectType"));
    }
    if (p.getProperty("action") != null) {
      action = String.valueOf(p.getProperty("action"));
    }
    if (p.getProperty("queryType") != null) {
      queryType = Enum.valueOf(QueryType.class,
          String.valueOf(p.getProperty("queryType")));
    }
    if (p.getProperty("query") != null) {
      query = String.valueOf(p.getProperty("query"));
    }

  }

  /**
   * Returns the parameters.
   *
   * @return the parameters
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    AlgorithmParameter param = new AlgorithmParameterJpa("ObjectType",
        "objectType", "Type of object an action will be performed on",
        "e.g. Concept", 200, AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(Arrays.asList("AtomJpa", "AtomRelationshipJpa",
        "AtomSubsetJpa", "AtomSubsetMemberJpa", "AttributeJpa", "CodeJpa",
        "CodeRelationshipJpa", "ComponentInfoRelationshipJpa", "ConceptJpa",
        "ConceptRelationshipJpa", "ConceptSubsetJpa", "ConceptSubsetMemberJpa",
        "DefinitionJpa", "DescriptorJpa", "DescriptorRelationshipJpa",
        "MappingJpa", "MapSetJpa", "SemanticTypeComponentJpa"));
    params.add(param);

    param = new AlgorithmParameterJpa("Action", "action",
        "An action to perform on the specified object type", "e.g. Approve",
        200, AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(Arrays.asList("Remove Demotions",
        "Make Publishable", "Make Unpublishable", "Approve", "Unapprove"));
    params.add(param);

    param = new AlgorithmParameterJpa("QueryType", "queryType",
        "The language the query is written in", "e.g. JPQL", 200,
        AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(EnumSet.allOf(QueryType.class).stream()
        .map(e -> e.toString()).collect(Collectors.toList()));
    params.add(param);

    param = new AlgorithmParameterJpa("Query", "query",
        "A query to perform action only on objects that meet the criteria",
        "e.g. query in format of the query type", 4000,
        AlgorithmParameter.Type.QUERY_ID, "");
    params.add(param);

    return params;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Perform <action> on <type> for <purpose>.";
  }

}