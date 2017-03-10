/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.KeyValuePair;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractMergeAlgorithm;
import com.wci.umls.server.jpa.algo.action.UndoMolecularAction;
import com.wci.umls.server.jpa.content.AtomJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.actions.MolecularActionList;

/**
 * Implementation of an algorithm to import attributes.
 */
public class GeneratedMergeAlgorithm extends AbstractMergeAlgorithm {

  /** The query type. */
  private QueryType queryType;

  /** The query. */
  private String query;

  /** The check names. */
  private List<String> checkNames;

  /** The new atoms only filter. */
  private Boolean newAtomsOnly = null;

  /** The filter query type. */
  private QueryType filterQueryType;

  /** The filter query. */
  private String filterQuery;

  /** The make demotions. */
  private Boolean makeDemotions = null;

  /** The change status. */
  private Boolean changeStatus = null;

  /** The merge set. */
  private String mergeSet;

  /** The mid merge flag. */
  private Boolean midMerge = false;

  /**
   * Instantiates an empty {@link GeneratedMergeAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public GeneratedMergeAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("GENERATEDMERGE");
    setLastModifiedBy("admin");
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
      throw new Exception("Generated Merge requires a project to be set");
    }

    return validationResult;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void compute() throws Exception {

    // If this is being called by Mid-Merge, prepend the lastModifiedBy with
    // "ENG-"
    if (midMerge) {
      setLastModifiedBy("ENG-" + getLastModifiedBy());
    }

    logInfo("Starting " + getName());
    logInfo("  query type = " + queryType);
    logInfo("  query = " + query);
    logInfo("  integrity checks = " + checkNames);
    logInfo("  news atoms only = " + newAtomsOnly);
    logInfo("  filter query type = " + filterQueryType);
    logInfo("  filter query = " + filterQuery);
    logInfo("  make demotions = " + makeDemotions);
    logInfo("  change status = " + changeStatus);
    logInfo("  merge set = " + mergeSet);
    commitClearBegin();

    // Molecular actions WILL be generated by this algorithm
    setMolecularActionFlag(true);

    // Set up a stats map to be passed into the merge function later
    final Map<String, Integer> statsMap = new HashMap<>();
    statsMap.put("atomPairsReturnedByQuery", 0);
    statsMap.put("atomPairsRemovedByFilters", 0);
    statsMap.put("atomPairsRemainingAfterFilters", 0);
    statsMap.put("conceptPairs", 0);
    statsMap.put("successfulMerges", 0);
    statsMap.put("unsuccessfulMerges", 0);
    statsMap.put("successfulDemotions", 0);
    statsMap.put("unsuccessfulDemotions", 0);

    try {
      logInfo("  Performing generated merges");
      commitClearBegin();

      // Generate parameters to pass into query executions
      Map<String, String> params = new HashMap<>();
      params.put("terminology", getProcess().getTerminology());
      params.put("version", getProcess().getVersion());
      params.put("latestTerminologyVersion",
          getProcess().getTerminology() + getProcess().getVersion());
      params.put("previousTerminologyVersion", getProcess().getTerminology()
          + getPreviousVersion(getProcess().getTerminology()));
      params.put("projectTerminology", getProject().getTerminology());
      params.put("projectVersion", getProject().getVersion());

      // Execute query to get atom1,atom2 Id pairs
      List<Long[]> atomIdPairs = executeComponentIdPairQuery(query, queryType,
          params, AtomJpa.class, false);
      statsMap.put("atomPairsReturnedByQuery", atomIdPairs.size());

      // Remove all atom pairs caught by the filters
      final List<Pair<Long, Long>> filteredAtomIdPairs =
          applyFilters(atomIdPairs, params, filterQueryType, filterQuery,
              newAtomsOnly, statsMap);
      statsMap.put("atomPairsRemainingAfterFilters",
          filteredAtomIdPairs.size());

      // Order atomIdPairs
      // sort by MergeLevel, atomId1, atomId2
      sortPairsByMergeLevelAndId(filteredAtomIdPairs);

      // Set the steps count to the number of atomPairs merges will be
      // attempted for
      setSteps(filteredAtomIdPairs.size());

      // Attempt to perform the merge given the integrity checks

      for (Pair<Long, Long> atomIdPair : filteredAtomIdPairs) {
        checkCancel();

        merge(atomIdPair.getLeft(), atomIdPair.getRight(), checkNames,
            makeDemotions, changeStatus, getProject(), statsMap);

        // Update the progress
        updateProgress();
      }

      commitClearBegin();

      logInfo("  atom pairs returned by query count = "
          + statsMap.get("atomPairsReturnedByQuery"));
      logInfo("  atom pairs removed by filters count = "
          + statsMap.get("atomPairsRemovedByFilters"));
      logInfo("  atom pairs remaining after filters count = "
          + statsMap.get("atomPairsRemainingAfterFilters"));
      logInfo("  unique concept-pair merges attempted = "
          + statsMap.get("conceptPairs"));
      logInfo("  merges successfully performed count = "
          + statsMap.get("successfulMerges"));
      logInfo("  unsuccessful merges count = "
          + statsMap.get("unsuccessfulMerges"));
      if (makeDemotions) {
        logInfo("  demotions successfully created count = "
            + statsMap.get("successfulDemotions"));
        logInfo("  attempted demotion unsuccessful count = "
            + statsMap.get("unsuccessfulDemotions"));
      }

      logInfo("Finished " + getName());
    } catch (Exception e) {
      logError("Unexpected problem - " + e.getMessage());
      throw e;
    }
  }

  /**
   * Reset.
   *
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // Collect any merges previously performed, and UNDO them in reverse order
    final PfsParameter pfs = new PfsParameterJpa();
    pfs.setAscending(false);
    pfs.setSortField("lastModified");
    final MolecularActionList molecularActions =
        findMolecularActions(null, getProject().getTerminology(),
            getProject().getVersion(), "activityId:" + getActivityId(), pfs);

    for (MolecularAction molecularAction : molecularActions.getObjects()) {
      // Create and set up an undo action
      final UndoMolecularAction undoAction = new UndoMolecularAction();

      // Configure and run the undo action
      undoAction.setProject(getProject());
      undoAction.setActivityId(molecularAction.getActivityId());
      undoAction.setConceptId(null);
      undoAction.setConceptId2(molecularAction.getComponentId2());
      undoAction.setLastModifiedBy(molecularAction.getLastModifiedBy());
      undoAction.setTransactionPerOperation(false);
      undoAction.setMolecularActionFlag(false);
      undoAction.setChangeStatusFlag(true);
      undoAction.setMolecularActionId(molecularAction.getId());
      undoAction.setForce(false);
      undoAction.performMolecularAction(undoAction, getLastModifiedBy(), false);
    }
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "mergeSet", "queryType", "query"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("queryType") != null) {
      final String qt = p.getProperty("queryType");
      // legacy handling
      if (qt.equals("JQL")) {
        queryType = QueryType.JPQL;
      } else {
        queryType = Enum.valueOf(QueryType.class, qt);
      }
    }
    if (p.getProperty("query") != null) {
      query = String.valueOf(p.getProperty("query"));
    }
    if (p.getProperty("checkNames") != null) {
      checkNames =
          Arrays.asList(String.valueOf(p.getProperty("checkNames")).split(";"));
    }
    if (p.getProperty("newAtomsOnly") != null) {
      newAtomsOnly = Boolean.parseBoolean(p.getProperty("newAtomsOnly"));
    }
    // Filter Query Type can come back as "", which is invalid.
    if (!ConfigUtility.isEmpty(p.getProperty("filterQueryType"))) {
      filterQueryType = Enum.valueOf(QueryType.class,
          String.valueOf(p.getProperty("filterQueryType")));
    }
    if (p.getProperty("filterQuery") != null) {
      filterQuery = String.valueOf(p.getProperty("filterQuery"));
    }
    if (p.getProperty("makeDemotions") != null) {
      makeDemotions = Boolean.parseBoolean(p.getProperty("makeDemotions"));
    }
    if (p.getProperty("changeStatus") != null) {
      changeStatus = Boolean.parseBoolean(p.getProperty("changeStatus"));
    }
    if (p.getProperty("mergeSet") != null) {
      mergeSet = String.valueOf(p.getProperty("mergeSet"));
    }
    if (p.getProperty("midMerge") != null) {
      midMerge = Boolean.parseBoolean(p.getProperty("midMerge"));
    }
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    // Query Type (only support JPQL and SQL)
    AlgorithmParameter param = new AlgorithmParameterJpa("Query Type",
        "queryType", "The language the query is written in", "e.g. JPQL", 200,
        AlgorithmParameter.Type.ENUM, QueryType.JPQL.toString());
    param.setPossibleValues(
        Arrays.asList(QueryType.JPQL.toString(), QueryType.SQL.toString()));
    params.add(param);

    // Query
    param = new AlgorithmParameterJpa("Query", "query",
        "A query to perform action only on objects that meet the criteria",
        "e.g. SELECT a.id, b.id FROM AtomJpa a, AtomJpa b ...", 4000,
        AlgorithmParameter.Type.QUERY_ID_PAIR, "");
    params.add(param);

    // Integrity check names
    param = new AlgorithmParameterJpa("Integrity Checks", "checkNames",
        "The names of the integrity checks to run", "e.g. MGV_B", 200,
        AlgorithmParameter.Type.MULTI, "");

    List<String> validationChecks = new ArrayList<>();
    for (final KeyValuePair validationCheck : getValidationCheckNames()
        .getKeyValuePairs()) {
      // Add handler Name to ENUM list
      validationChecks.add(validationCheck.getKey());
    }
    Collections.sort(validationChecks);
    param.setPossibleValues(validationChecks);
    params.add(param);

    // New atoms only filter
    param = new AlgorithmParameterJpa("New Atoms Only Filter", "newAtomsOnly",
        "Restrict to new atoms only?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "false");
    params.add(param);

    // filter query type
    param = new AlgorithmParameterJpa("Filter Query Type", "filterQueryType",
        "The language the filter query is written in", "e.g. JPQL", 200,
        AlgorithmParameter.Type.ENUM, "");
    param.setPossibleValues(EnumSet.allOf(QueryType.class).stream()
        .map(e -> e.toString()).collect(Collectors.toList()));
    params.add(param);

    // filter query
    param = new AlgorithmParameterJpa("Filter Query", "filterQuery",
        "A filter query to further restrict the objects to run the merge on",
        "e.g. query in format of the query type", 4000,
        AlgorithmParameter.Type.TEXT, "");
    params.add(param);

    // make demotions
    param = new AlgorithmParameterJpa("Make Demotions", "makeDemotions",
        "Make demotions for failed merges?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "true");
    params.add(param);

    // change status
    param = new AlgorithmParameterJpa("Change Status", "changeStatus",
        "Change status when performing merges?", "e.g. true", 5,
        AlgorithmParameter.Type.BOOLEAN, "true");
    params.add(param);

    // mergeSet
    param = new AlgorithmParameterJpa("Merge Set", "mergeSet",
        "The merge set to perform the merges on", "e.g. NCI-SY", 10,
        AlgorithmParameter.Type.STRING, "");
    params.add(param);

    return params;
  }

  @Override
  public String getDescription() {
    return "Merges <x> with <y> for <purpose>";
  }

  /**
   * Sets the mid merge.
   *
   * @param midMerge the mid merge
   */
  public void setMidMerge(Boolean midMerge) {
    this.midMerge = midMerge;
  }
}