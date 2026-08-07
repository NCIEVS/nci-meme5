/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.maint;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.ServerRestartException;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;

/**
 * Requests an application restart and lets the process auto-resume after boot.
 */
public class ServerRestartAlgorithm extends AbstractAlgorithm {

  /** Algorithm key. */
  public static final String ALGORITHM_KEY = "SERVERRESTART";

  /** Pending restart marker key. */
  public static final String RESTART_PENDING_KEY = "serverRestart.pending";

  /** Restart algorithm activity id key. */
  public static final String RESTART_ACTIVITY_ID_KEY =
      "serverRestart.activityId";

  /** Restart algorithm execution id key. */
  public static final String RESTART_ALGORITHM_EXECUTION_ID_KEY =
      "serverRestart.algorithmExecutionId";

  /** Restart request date key. */
  public static final String RESTART_REQUEST_DATE_KEY =
      "serverRestart.requestDate";

  /** Restart feature enabled property. */
  public static final String RESTART_ENABLED_PROPERTY =
      "server.restart.enabled";

  /** Require systemd property. */
  public static final String RESTART_SYSTEMD_REQUIRED_PROPERTY =
      "server.restart.systemd.required";

  /**
   * Instantiates an empty {@link ServerRestartAlgorithm}.
   *
   * @throws Exception if anything goes wrong
   */
  public ServerRestartAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId(ALGORITHM_KEY);
    setLastModifiedBy("admin");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    final ValidationResultJpa result = new ValidationResultJpa();

    if (getProject() == null) {
      result.addError("Server Restart requires a project to be set.");
      return result;
    }
    if (getProcess() == null || getProcess().getId() == null) {
      result.addError("Server Restart requires a process execution to be set.");
      return result;
    }
    if (!isRestartEnabled()) {
      result.addError("Server Restart is disabled by "
          + RESTART_ENABLED_PROPERTY + ".");
    }
    if (isSystemdRequired() && !isSystemdLaunch()) {
      result.addError(
          "Server Restart requires the application to be running under systemd.");
    }

    return result;
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    if (isResumeRun()) {
      clearRestartRequest();
      fireProgressEvent(100, ALGORITHM_KEY + " progress: 100%");
      logInfo("Finished " + getName());
      return;
    }

    throw new ServerRestartException(
        "Server Restart requested by process execution " + getProcess().getId()
            + ".");
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
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
    return super.getParameters();
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return "Algorithm for restarting the server and automatically resuming the "
        + "process run after the application starts again.";
  }

  /**
   * Indicates whether this execution is resuming after the requested restart.
   *
   * @return true if this is the resume run
   */
  private boolean isResumeRun() {
    return getProcess() != null
        && Boolean.parseBoolean(
            getProcess().getExecutionInfo().get(RESTART_PENDING_KEY))
        && getActivityId() != null && getActivityId().equals(
            getProcess().getExecutionInfo().get(RESTART_ACTIVITY_ID_KEY));
  }

  /**
   * Clears the persisted restart request.
   */
  private void clearRestartRequest() {
    getProcess().getExecutionInfo().remove(RESTART_PENDING_KEY);
    getProcess().getExecutionInfo().remove(RESTART_ACTIVITY_ID_KEY);
    getProcess().getExecutionInfo().remove(RESTART_ALGORITHM_EXECUTION_ID_KEY);
    getProcess().getExecutionInfo().remove(RESTART_REQUEST_DATE_KEY);
  }

  /**
   * Indicates whether server restart is enabled.
   *
   * @return true if enabled
   */
  private static boolean isRestartEnabled() {
    return Boolean.parseBoolean(
        PropertyUtility.getProperties().getProperty(RESTART_ENABLED_PROPERTY));
  }

  /**
   * Indicates whether a systemd launch is required.
   *
   * @return true if required
   */
  private static boolean isSystemdRequired() {
    return Boolean.parseBoolean(PropertyUtility.getProperties()
        .getProperty(RESTART_SYSTEMD_REQUIRED_PROPERTY));
  }

  /**
   * Indicates whether the application appears to have been started by systemd.
   *
   * @return true if systemd launch markers are present
   */
  private static boolean isSystemdLaunch() {
    return !ConfigUtility.isEmpty(System.getenv("INVOCATION_ID"))
        || !ConfigUtility.isEmpty(System.getenv("JOURNAL_STREAM"));
  }

  /**
   * Populates the restart request markers.
   *
   * @param properties the execution info properties
   * @param activityId the algorithm activity id
   * @param algorithmExecutionId the algorithm execution id
   */
  public static void markRestartRequest(
    final java.util.Map<String, String> properties, final String activityId,
    final Long algorithmExecutionId) {
    properties.put(RESTART_PENDING_KEY, "true");
    properties.put(RESTART_ACTIVITY_ID_KEY, activityId);
    properties.put(RESTART_ALGORITHM_EXECUTION_ID_KEY,
        algorithmExecutionId == null ? "" : algorithmExecutionId.toString());
    properties.put(RESTART_REQUEST_DATE_KEY,
        ConfigUtility.DATE_YYYYMMDDHHMMSS.format(new Date()));
  }
}
