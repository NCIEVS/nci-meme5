/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.content.ComponentHistoryJpa;
import com.wci.umls.server.jpa.content.ConceptJpa;
import com.wci.umls.server.model.content.ComponentHistory;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * Algorithm for reloading concept history.
 */
public class ReloadConceptHistoryAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /** The mr dir file. */
  private File mrDirFile = null;

  /** The concept created count. */
  private int conceptCreatedCount = 0;

  /** The component history created count. */
  private int componentHistoryCreatedCount = 0;

  /** The component history deleted count. */
  private int componentHistoryDeletedCount = 0;

  /**
   * Instantiates an empty {@link ReloadConceptHistoryAlgorithm}.
   *
   *
   * @throws Exception the exception
   */
  public ReloadConceptHistoryAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("FEEDBACKRELEASE");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResult result = new ValidationResultJpa();

    if (getProject() == null) {
      throw new Exception("Algorithm requires a project to be set");
    }

    // Check the mr directory
    String mrPath = config.getProperty("source.data.dir") + "/"
        + getProcess().getInputPath() + "/" + getProcess().getVersion()
        + "/META";

    mrDirFile = new File(mrPath);
    if (!mrDirFile.exists()) {
      throw new Exception(
          "Specified input directory does not exist = " + mrPath);
    }

    return result;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    fireProgressEvent(0, "Starting");

    // Identify all concepts with concept histories in the DB
    final Set<String> dbConceptsWithHistories = new HashSet<>();

    final javax.persistence.Query jpaQuery = getEntityManager().createQuery(
        "select c.terminologyId from ConceptJpa c where c.componentHistories is not empty");

    final List<String> conceptsTerminologyIds = jpaQuery.getResultList();

    for (final String terminologyId : conceptsTerminologyIds) {
      dbConceptsWithHistories.add(terminologyId);
    }

    //
    // Load the MRCUI.RRF file
    //
    final File path = new File(config.getProperty("source.data.dir") + "/"
        + getProcess().getInputPath() + "/" + getProcess().getVersion()
        + "/META");

    final List<String> lines =
        loadFileIntoStringList(path, "MRCUI.RRF", null, null);

    // Loop through MRCUI, saving all lines associated with each CUI1 to a map
    final Map<String, List<String>> cuiHistoryLines = new HashMap<>();

    for (final String line : lines) {
      final String cui1 = line.substring(0, line.indexOf('|'));

      // If this is the first time this CUI has been encountered, initialize its
      // entry in the map, and remove it from the dbConceptsWithHistories set
      if (cuiHistoryLines.get(cui1) == null) {
        cuiHistoryLines.put(cui1, new ArrayList<>());
        dbConceptsWithHistories.remove(cui1);
      }

      // Add the line to this CUI's entry in the map
      cuiHistoryLines.get(cui1).add(line);
    }

    // Set the number of steps to the number of concepts referenced in MRCUI
    setSteps(cuiHistoryLines.keySet().size());

    // For each referenced CUI1, load the concept, and check its histories
    // against the MRCUI lines.
    // If there are any discrepancies, update concept's histories to match MRCUI

    final String fields[] = new String[7];

    for (final String cui1 : cuiHistoryLines.keySet()) {

      // Check for a cancelled call once every 100 lines
      if (getStepsCompleted() % 100 == 0) {
        checkCancel();
      }

      Concept concept = getConcept(cui1, getProject().getTerminology(),
          getProject().getVersion(), Branch.ROOT);

      // If no concept exists, create a new unpublishable concept
      // Also fire warning, since this should really not happen
      if (concept == null) {

        logWarn("WARNING: Concept could not be found for " + cui1
            + ".  Creating placeholder concept.");

        concept = new ConceptJpa();
        concept.setPublishable(false);
        concept.setTerminologyId(cui1);
        concept.setName("Placeholder concept for " + cui1);
        concept.setBranch(Branch.ROOT);
        concept.setObsolete(false);
        concept.setPublishable(false);
        concept.setPublished(true);
        concept.setSuppressible(false);
        concept.setTerminology(getProject().getTerminology());
        concept.setVersion(getProject().getVersion());
        concept.setWorkflowStatus(WorkflowStatus.READY_FOR_PUBLICATION);

        concept = addConcept(concept);
        conceptCreatedCount++;
      }

      final List<ComponentHistory> cui1Histories =
          new ArrayList<>(concept.getComponentHistory());

      // Loop through histories and MRCUI lines, removing matches.
      for (final ComponentHistory cui1History : new ArrayList<>(
          cui1Histories)) {
        for (final String MRCUIline : new ArrayList<>(
            cuiHistoryLines.get(cui1))) {

          FieldedStringTokenizer.split(MRCUIline, "|", 7, fields);

          // Fields
          // 0 CUI1
          // 1 VER
          // 2 REL
          // 3 RELA
          // 4 MAPREASON
          // 5 CUI2
          // 6 MAPIN

          // e.g. C0000002|2000AC|SY|||C0007404|Y|

          // Remove MAPIN=N rows
          if (fields[6].equals("N")) {
            cui1Histories.remove(cui1History);
            break;
          }

          // Handle DEL cases
          if (fields[2].equals("DEL")) {
            if (cui1History.getAssociatedRelease().equals(fields[1])
                && cui1History.getRelationshipType().equals(fields[2])) {
              cui1Histories.remove(cui1History);
              cuiHistoryLines.get(cui1).remove(MRCUIline);
              break;
            }
          }

          // Handle relationship cases
          else if (cui1History.getAssociatedRelease().equals(fields[1])
              && cui1History.getRelationshipType().equals(fields[2])
              && cui1History.getAdditionalRelationshipType().equals(fields[3])
              && cui1History.getReferencedConcept().getTerminologyId()
                  .equals(fields[5])) {
            cui1Histories.remove(cui1History);
            cuiHistoryLines.get(cui1).remove(MRCUIline);
            break;
          }
        }
      }

      // Anything remaining in the histories needs to be removed and deleted
      // from the concept
      for (final ComponentHistory cui1History : cui1Histories) {
        concept.getComponentHistory().remove(cui1History);
        updateConcept(concept);
        removeComponentHistory(cui1History.getId());
        componentHistoryDeletedCount++;
      }

      // Anything remaining in the MRCUI needs to be created and added to the
      // concept
      for (final String MRCUIline : cuiHistoryLines.get(cui1)) {

        FieldedStringTokenizer.split(MRCUIline, "|", 7, fields);

        // Create the history entry (the referenced
        final ComponentHistory history = new ComponentHistoryJpa();
        history.setPublished(true);
        history.setPublishable(true);
        history.setTerminology(getProject().getTerminology());
        history.setTerminologyId(fields[0]);
        history.setVersion(getProject().getVersion());

        if (!fields[5].isEmpty()) {
          final Concept concept2 =
              getConcept(fields[5], getProject().getTerminology(),
                  getProject().getVersion(), Branch.ROOT);
          if (concept2 == null) {
            throw new Exception("Unexpected dead CUIs " + fields[5]);
          }
          history.setReferencedConcept(concept2);
        }
        history.setRelationshipType(fields[2]);
        history.setAdditionalRelationshipType(fields[3]);
        history.setReason("");
        history.setAssociatedRelease(fields[1]);
        addComponentHistory(history);
        componentHistoryCreatedCount++;

        concept.getComponentHistory().add(history);
        updateConcept(concept);
      }

      // Update the progress
      updateProgress();
    }

    // Anything concept remaining in the dbConceptsWithHistories map needs to
    // have its histories removed.
    for (final String cui : dbConceptsWithHistories) {
      final Concept concept = getConcept(cui, getProject().getTerminology(),
          getProject().getVersion(), Branch.ROOT);
      for (final ComponentHistory cuiHistory : concept.getComponentHistory()) {
        removeComponentHistory(cuiHistory.getId());
        componentHistoryDeletedCount++;
      }

      concept.setComponentHistory(null);
      updateConcept(concept);
    }

    commitClearBegin();

    fireProgressEvent(100, "Finished - 100%");
    logInfo("  placeholder concepts created = " + conceptCreatedCount);
    logInfo("  component histories created = " + componentHistoryCreatedCount);
    logInfo("  component histories deleted = " + componentHistoryDeletedCount);
    logInfo("Finished " + getName());

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // No reset, this can be safely re-run
    logInfo("Finished RESET " + getName());
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        ""
    }, p);
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    return params;
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}
