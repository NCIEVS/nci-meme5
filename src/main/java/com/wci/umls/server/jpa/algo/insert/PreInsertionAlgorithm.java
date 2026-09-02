/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.insert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.NoResultException;

import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ProcessExecution;
import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.model.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;

/**
 * Implementation of an algorithm to save information before an insertion.
 */
public class PreInsertionAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  /** The estimated completion field name. */
  private static final String ESTIMATED_COMPLETION_FIELD =
      "estimatedCompletion";

  /** The algorithm properties. */
  private Properties properties = new Properties();

  /**
   * Instantiates an empty {@link PreInsertionAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public PreInsertionAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("PREINSERTION");
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
      throw new LocalException("Pre Insertion requires a project to be set");
    }

    // Go through all the files needed by insertion and check for presence.
    setSrcDirFileFromProcessInputPath();

    checkFileExist("attributes.src");
    checkFileExist("classes_atoms.src");
    checkFileExist("contexts.src");
    checkFileExist("mergefacts.src");
    checkFileExist("MRDOC.RRF");
    checkFileExist("relationships.src");
    checkFileExist("sources.src");
    checkFileExist("termgroups.src");

    // Checking for UMLS-specific files.
    if (getProcess().getTerminology().equals("MTH")) {
      checkFileExist("umlscui.txt");
      checkFileExist("bequeathal.relationships.src");
    }

    // Ensure permissions are sufficient to write files
    try {
      final File outputFile = getSrcFile("testFile.txt");

      final PrintWriter out =
          new PrintWriter(new FileWriter(outputFile, StandardCharsets.UTF_8));
      out.print("Test");
      out.close();

      // Remove test file
      ConfigUtility.deleteFileIfExists(outputFile);
    } catch (Exception e) {
      throw new LocalException("Unable to write files to " + getSrcDirFile()
          + " - update permissions before continuing insertion.");
    }

    // Makes sure editing is turned off before continuing
    if(getProject().isEditingEnabled()){
      throw new LocalException("Editing is turned on - disable before continuing insertion.");
    }
    
    // Makes sure automations are turned off before continuing
    if(getProject().isAutomationsEnabled()){
      throw new LocalException("Automations are turned on - disable before continuing insertion.");
    }
    
    //
    // Check for duplicate source atom ids
    //

    // Lookup all existing source atom ids from the database
    logInfo("[PreInsertionAlgorithm] Loading Source Atom Ids from database");
    
    String query = "select value(b) from AtomJpa a join a.alternateTerminologyIds b "
        + "where KEY(b) = :terminology ";
    
    jakarta.persistence.Query jpaQuery =
      getEntityManager().createQuery(query);
    jpaQuery.setParameter("terminology", getProject().getTerminology() + "-SRC");

    List<Object> list = jpaQuery.getResultList();
    Set<String> existingSourceAtomIds = new HashSet<>();
    for (Object entry : list) {
      existingSourceAtomIds.add(entry.toString());
    }
    
    // Check each of the classes_atoms source lines
    List<String> srcLines = loadFileIntoStringList(getSrcDirFile(), "classes_atoms.src",
        null, null, null);
    
    String fields[] = new String[14];

    for (String line : srcLines) {
      FieldedStringTokenizer.split(line, "|", 14, fields);
      if (existingSourceAtomIds.contains(fields[0])) {
        validationResult.addError("ERROR: classes_atoms.src references a SRC atom id " + fields[0]
            + " that is already contained in the database.");
        break;
      }
    }

    // check sufficient disk space (for now, if less than ~20GB)
    NumberFormat nf = NumberFormat.getNumberInstance();
    Path root = Paths.get("");

    try {
      FileStore store = Files.getFileStore(root);

      logInfo("[PreInsertionAlgorithm] Checking sufficient disk space on " + root.toAbsolutePath()
          + ": available=" + store.getUsableSpace() + ", total="
          + nf.format(store.getTotalSpace()));

      if (store.getUsableSpace() < 20000000000L) {
        validationResult
            .addError("ERROR: Insufficient disk space: " + nf.format(store.getUsableSpace()));
        return validationResult;
      }

    } catch (IOException e) {
      validationResult.addError("ERROR: error querying space: " + e.toString());
    }

    return validationResult;
  }

  /**
   * Check file exist.
   *
   * @param fileName the file name
   * @throws Exception the exception
   */
  private void checkFileExist(String fileName) throws Exception {

    File sourceFile = getSrcFile(fileName);
    if (!sourceFile.exists()) {
      throw new Exception(fileName
          + " file doesn't exist at specified input directory: "
          + getSrcDirFile());
    }

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

    // No Molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);

    // Populate the executionInfo map of the process' execution.
    ProcessExecution processExecution = getProcess();

    // Get the max atom Id prior to the insertion starting (used to identify
    // which atoms are new)
    Long atomId = null;
    try {
      final jakarta.persistence.Query query =
          manager.createQuery("select max(a.id) from AtomJpa a ");
      final Long atomId2 = (Long) query.getSingleResult();
      atomId = atomId2 != null ? atomId2 : 0L;
    } catch (NoResultException e) {
      atomId = 0L;
    }
    processExecution.getExecutionInfo().put("maxAtomIdPreInsertion",
        atomId.toString());
    logInfo(" maxAtomIdPreInsertion = "
        + processExecution.getExecutionInfo().get("maxAtomIdPreInsertion"));
    commitClearBegin();

    // Get the max AUI prior to the insertion starting (used to identify
    // newly created AUIs)
    Long AUI = null;
    try {
      final jakarta.persistence.Query query =
          manager.createQuery("select max(a.id) from AtomIdentityJpa a ");
      final Long AUI2 = (Long) query.getSingleResult();
      AUI = AUI2 != null ? AUI2 : 0L;
    } catch (NoResultException e) {
      AUI = 0L;
    }
    processExecution.getExecutionInfo().put("maxAUIPreInsertion",
        AUI.toString());
    logInfo(" maxAUIPreInsertion = "
        + processExecution.getExecutionInfo().get("maxAUIPreInsertion"));
    commitClearBegin();

    // Get the max Semantic Type Component Id prior to the insertion starting
    Long styId = null;
    try {
      final jakarta.persistence.Query query = manager
          .createQuery("select max(a.id) from SemanticTypeComponentJpa a ");
      final Long styId2 = (Long) query.getSingleResult();
      styId = styId2 != null ? styId2 : 0L;
    } catch (NoResultException e) {
      styId = 0L;
    }
    processExecution.getExecutionInfo().put("maxStyIdPreInsertion",
        styId.toString());
    logInfo(" maxStyIdPreInsertion = "
        + processExecution.getExecutionInfo().get("maxStyIdPreInsertion"));

    // Get the max MapSet Id prior to the insertion starting
    Long mapSetId = null;
    try {
      final jakarta.persistence.Query query =
          manager.createQuery("select max(a.id) from MapSetJpa a ");
      final Long mapSetId2 = (Long) query.getSingleResult();
      mapSetId = mapSetId2 != null ? mapSetId2 : 0L;
    } catch (NoResultException e) {
      mapSetId = 0L;
    }
    processExecution.getExecutionInfo().put("maxMapSetIdPreInsertion",
        mapSetId.toString());
    logInfo(" maxMapSetIdPreInsertion = "
        + processExecution.getExecutionInfo().get("maxMapSetIdPreInsertion"));

    // Get the max Atom Subset Id prior to the insertion starting
    Long atomSubsetId = null;
    try {
      final jakarta.persistence.Query query =
          manager.createQuery("select max(a.id) from AtomSubsetJpa a ");
      final Long atomSubsetId2 = (Long) query.getSingleResult();
      atomSubsetId = atomSubsetId2 != null ? atomSubsetId2 : 0L;
    } catch (NoResultException e) {
      atomSubsetId = 0L;
    }
    processExecution.getExecutionInfo().put("maxAtomSubsetIdPreInsertion",
        atomSubsetId.toString());
    logInfo(" maxAtomSubsetIdPreInsertion = " + processExecution
        .getExecutionInfo().get("maxAtomSubsetIdPreInsertion"));

    // Get the max Concept Subset Id prior to the insertion starting
    Long conceptSubsetId = null;
    try {
      final jakarta.persistence.Query query =
          manager.createQuery("select max(a.id) from ConceptSubsetJpa a ");
      final Long conceptSubsetId2 = (Long) query.getSingleResult();
      conceptSubsetId =
          conceptSubsetId2 != null ? conceptSubsetId2 : 0L;
    } catch (NoResultException e) {
      conceptSubsetId = 0L;
    }
    processExecution.getExecutionInfo().put("maxConceptSubsetIdPreInsertion",
        conceptSubsetId.toString());
    logInfo(" maxConceptSubsetIdPreInsertion = " + processExecution
        .getExecutionInfo().get("maxConceptSubsetIdPreInsertion"));

    // NOTE: the processExecution is updated by the calling method,
    // typically RunProcessAsThread in ProcessServiceRestImpl

    sendPreInsertionEmail(processExecution);

    logInfo("Finished " + getName());
  }

  /**
   * Sends an insertion-in-progress notification.
   *
   * @param processExecution the process execution
   * @throws Exception the exception
   */
  private void sendPreInsertionEmail(final ProcessExecution processExecution)
    throws Exception {

    final Properties config = PropertyUtility.getProperties();
    final String recipients = getInsertionNotificationRecipients(config);
    if (isBlank(recipients)) {
      logInfo(" No insertion notification recipients configured; "
          + "skipping pre-insertion email.");
      return;
    }

    if ("false".equals(config.getProperty("mail.enabled"))) {
      logInfo(" Mail disabled; skipping pre-insertion email.");
      return;
    }

    final String estimatedCompletion =
        properties.getProperty(ESTIMATED_COMPLETION_FIELD, "").trim();
    if (isBlank(estimatedCompletion)) {
      throw new LocalException("Required property " + ESTIMATED_COMPLETION_FIELD
          + " missing for pre-insertion email.");
    }

    final String from = getMailFrom(config);

    final String insertionName = processExecution.getTerminology() + "_"
        + processExecution.getVersion();
    final String server = InetAddress.getLocalHost().getHostName();
    final String insertionEnvironment = getInsertionEnvironment(server);
    final String insertionType = getInsertionType(insertionEnvironment);
    final String subjectPrefix =
        "test".equals(insertionType) ? "Test Insertion" : "Insertion";
    final String subject = subjectPrefix + " of " + insertionName
        + ": IN PROGRESS ("
        + insertionEnvironment.toUpperCase(Locale.ROOT) + ")";
    final String body = "Hi all,\n\nThe " + insertionType + " insertion of "
        + insertionName + " is in progress on "
        + insertionEnvironment.toLowerCase(Locale.ROOT)
        + ". We expect that it will be complete "
        + asSentence(estimatedCompletion) + "\n\n"
        + getEmailSignature(processExecution);

    ConfigUtility.sendEmail(subject, from, recipients, body, config);
    logInfo(" Sent pre-insertion email to " + recipients);
  }

  /**
   * Returns the process executor signature for the email.
   *
   * @param processExecution the process execution
   * @return the email signature
   */
  private String getEmailSignature(final ProcessExecution processExecution)
    throws Exception {

    final String userName;
    if (!isBlank(getLastModifiedBy())) {
      userName = getLastModifiedBy();
    } else if (!isBlank(processExecution.getLastModifiedBy())) {
      userName = processExecution.getLastModifiedBy();
    } else {
      return "";
    }

    final User user = getUser(userName);
    return user != null && !isBlank(user.getName()) ? user.getName() : userName;
  }

  /**
   * Ensures a sentence-ending punctuation mark is present.
   *
   * @param value the value
   * @return the value as a sentence
   */
  private String asSentence(final String value) {

    final String trimmed = value.trim();
    if (trimmed.endsWith(".") || trimmed.endsWith("!")
        || trimmed.endsWith("?")) {
      return trimmed;
    }
    return trimmed + ".";
  }

  /**
   * Indicates whether the string is blank.
   *
   * @param value the value
   * @return true if the value is blank
   */
  private boolean isBlank(final String value) {

    return value == null || value.trim().isEmpty();
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
    if (p == null) {
      throw new LocalException("Algorithm properties must not be null");
    }
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    checkProperties(p);
    properties = new Properties();
    properties.putAll(p);
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    params.add(new AlgorithmParameterJpa("Estimated Completion",
        ESTIMATED_COMPLETION_FIELD,
        "Estimated time frame for the insertion completion email.",
        "e.g. by tomorrow morning July 24th", 255,
        AlgorithmParameter.Type.STRING, ""));

    return params;
  }

  @Override
  public String getDescription() {
    return "Prepares an insertion to operate and validates starting conditions.";
  }
}
