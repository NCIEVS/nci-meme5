/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.model.meta.Terminology;
import com.wci.umls.server.model.workflow.Checklist;

/**
 * Implementation of an algorithm to create report table checklists.
 */
public class ReportChecklistAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /**
   * Instantiates an empty {@link ReportChecklistAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public ReportChecklistAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("REPORTCHECKLIST");
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
      throw new Exception(
          "Report Checklist Algorithm requires a project to be set");
    }

    // Check the input directories

    final String srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    setSrcDirFile(new File(srcFullPath));
    if (!getSrcDirFile().exists()) {
      throw new Exception("Specified input directory does not exist");
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
    logInfo("Starting " + getName());

    // No molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    try {

      logInfo("  Creating the report table checklists");
      commitClearBegin();

      final File outputFile =
          new File(getSrcDirFile(), "reportChecklistResults.txt");
      String server = InetAddress.getLocalHost().getHostName();
      
      final PrintWriter out = new PrintWriter(new FileWriter(outputFile));
      
      out.println("Hi all,");
 
      out.println("The " + (server.equals("ncias-q3009-c") ? "test" : "real") + "insertion of " + getProcess().getTerminology() + "_" + 
    	  getProcess().getVersion() + " is complete on " + (server.equals("ncias-q3009-c") ? "meme-test" : "meme-edit") + 
    	  ". The counts are as follows:");
      out.println("");
      
      // Get all terminologies referenced in the sources.src file
      Set<Terminology> terminologies = new HashSet<>();
      terminologies = getReferencedTerminologies();

      setSteps(terminologies.size());

      // For each terminology, create four checklists
      for (final Terminology terminology : terminologies) {
        final String term = terminology.getTerminology();
        final String version = terminology.getVersion();

        checkCancel();

        // All four queries start with the same clauses
        final String queryPrefix =
            "select c.id as conceptId from ConceptJpa c join c.atoms a "
                + "where c.terminology=:projectTerminology and "
                + "a.terminology='" + term + "' and a.version='" + version
                + "'";

        if (getProcess().getTerminology().contains(term) && getProcess().getTerminology().contains(version)) {      	
        	out.println("");
        }
        
        Checklist checklist = computeChecklist(getProject(),
            queryPrefix + " AND a.workflowStatus='NEEDS_REVIEW'", QueryType.JPQL,
            "chk_" + term + "_" + version + "_NEEDS_REVIEW", null, true);
        String result = "Created chk_" + term + "_" + version
            + "_NEEDS_REVIEW checklist, containing "
            + checklist.getTrackingRecords().size() + " tracking records.";
        logInfo("  " + result);
        if (checklist.getTrackingRecords().size() > 0) {
        	out.println(result);
        }
        commitClearBegin();

        checklist = computeChecklist(getProject(),
            queryPrefix + " AND a.workflowStatus='DEMOTION'", QueryType.JPQL,
            "chk_" + term + "_" + version + "_DEMOTION", null, true);
        result = "Created chk_" + term + "_" + version
            + "_DEMOTION checklist, containing "
            + checklist.getTrackingRecords().size() + " tracking records.";
        logInfo("  " + result);
        if (checklist.getTrackingRecords().size() > 0) {
        	out.println(result);
        }
        commitClearBegin();

        checklist = computeChecklist(getProject(),
            queryPrefix
                + " AND (a.workflowStatus='READY_FOR_PUBLICATION' OR a.workflowStatus='PUBLISHED')",
            QueryType.JPQL,
            "chk_" + term + "_" + version + "_READY_FOR_PUBLICATION", null,
            true);
        result = "Created chk_" + term + "_" + version
            + "_READY_FOR_PUBLICATION checklist, containing "
            + checklist.getTrackingRecords().size() + " tracking records.";
        logInfo("  " + result);
        if (getProcess().getTerminology().contains(term) && getProcess().getTerminology().contains(version)) {
        	out.println(result);
        }
        commitClearBegin();

        checklist = computeChecklist(getProject(),
            queryPrefix + " AND a.lastModifiedBy like 'ENG-%'", QueryType.JPQL,
            "chk_" + term + "_" + version + "_MIDMERGES", null, true);
        result = "Created chk_" + term + "_" + version
            + "_MIDMERGES checklist, containing "
            + checklist.getTrackingRecords().size() + " tracking records.";
        logInfo("  " + result);
        if (checklist.getTrackingRecords().size() > 0) {
        	out.println(result);
        }
        commitClearBegin();

        // Update the progress
        updateProgress();
        if (getProcess().getTerminology().contains(term) && getProcess().getTerminology().contains(version)) {      	
        	out.println("");
        }
        logInfo("");
      }

      commitClearBegin();

      logInfo("Finished " + getName());
      out.close();

      // Email report checklist count document to people specified by process's
      // feedback email
      final String recipients = getProcess().getFeedbackEmail();

      if (!ConfigUtility.isEmpty(recipients)) {
        final Properties config = ConfigUtility.getConfigProperties();
        String from;
        if (config.containsKey("mail.smtp.from")) {
          from = config.getProperty("mail.smtp.from");
        } else {
          from = config.getProperty("mail.smtp.user");
        }

        ConfigUtility.sendEmail(
            "Report Checklist Algorithm Complete for Process: "
                + getProcess().getName(),
            from, recipients,
            "Checklist counts attached - please edit and email.", config,
            outputFile.getAbsolutePath());
      }

    } catch (

    Exception e) {
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

  /**
   * Returns the parameters.
   *
   * @return the parameters
   */
  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    return params;
  }

  @Override
  public String getDescription() {
    return "Generates standard report table checklists for the insertion.";
  }
}