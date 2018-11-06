/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.client;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import com.wci.umls.server.AlgorithmConfig;
import com.wci.umls.server.ProcessConfig;
import com.wci.umls.server.ProcessExecution;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.helpers.ProcessConfigList;
import com.wci.umls.server.helpers.ProcessExecutionList;
import com.wci.umls.server.helpers.QueryStyle;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.AlgorithmConfigJpa;
import com.wci.umls.server.jpa.ProcessConfigJpa;
import com.wci.umls.server.jpa.ProcessExecutionJpa;
import com.wci.umls.server.jpa.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.helpers.ProcessConfigListJpa;
import com.wci.umls.server.jpa.helpers.ProcessExecutionListJpa;
import com.wci.umls.server.jpa.services.rest.ProcessServiceRest;

/**
 * A client for connecting to a processClient REST service.
 */
public class ProcessClientRest extends RootClientRest
    implements ProcessServiceRest {

  /** The config. */
  private Properties config = null;

  /**
   * Instantiates a {@link ProcessClientRest} from the specified parameters.
   *
   * @param config the config
   */
  public ProcessClientRest(Properties config) {
    this.config = config;
  }

  /* see superclass */
  @Override
  public ProcessConfig addProcessConfig(Long projectId,
    ProcessConfigJpa processConfig, String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("ProcessConfig Client - add processConfig" + processConfig);

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config" + "?projectId=" + projectId);

    final String processConfigString = ConfigUtility.getStringForGraph(
        processConfig == null ? new ProcessConfigJpa() : processConfig);

    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .put(Entity.xml(processConfigString));

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(resultString);
    }

    // converting to object
    ProcessConfigJpa result =
        ConfigUtility.getGraphForString(resultString, ProcessConfigJpa.class);

    return result;
  }

  /* see superclass */
  @Override
  public void updateProcessConfig(Long projectId,
    ProcessConfigJpa processConfig, String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - update processConfig " + processConfig);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config" + "?projectId=" + projectId);

    String processConfigString = ConfigUtility.getStringForGraph(
        processConfig == null ? new ProcessConfigJpa() : processConfig);
    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .post(Entity.xml(processConfigString));

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /* see superclass */
  @Override
  public void removeProcessConfig(Long projectId, Long id, Boolean cascade,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - remove processConfig " + id);
    validateNotEmpty(id, "id");
    Client client = ClientBuilder.newClient();
    WebTarget target =
        client.target(config.getProperty("base.url") + "/process/config/" + id
            + "?projectId=" + projectId + (cascade ? "&cascade=true" : ""));

    if (id == null)
      return;

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).delete();

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /* see superclass */
  @Override
  public ProcessConfig getProcessConfig(Long projectId, Long id,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - get processConfig " + id);
    validateNotEmpty(id, "id");

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config/" + id + "?projectId=" + projectId);

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    ProcessConfigJpa processConfig =
        ConfigUtility.getGraphForString(resultString, ProcessConfigJpa.class);
    return processConfig;
  }

  /* see superclass */
  @Override
  public ProcessConfigList findProcessConfigs(Long projectId, String query,
    PfsParameterJpa pfs, String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - find processConfigs " + query);

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config/find" + "?projectId=" + projectId + "&query="
        + URLEncoder.encode(query == null ? "" : query, "UTF-8")
            .replaceAll("\\+", "%20"));
    final String pfsString = ConfigUtility
        .getStringForGraph(pfs == null ? new PfsParameterJpa() : pfs);
    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).post(Entity.xml(pfsString));

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return ConfigUtility.getGraphForString(resultString,
        ProcessConfigListJpa.class);

  }

  /* see superclass */
  @Override
  public ProcessExecution getProcessExecution(Long projectId, Long id,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - get processExecution " + id);
    validateNotEmpty(id, "id");

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/" + id + "?projectId=" + projectId);

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    ProcessExecutionJpa processExecution = ConfigUtility
        .getGraphForString(resultString, ProcessExecutionJpa.class);
    return processExecution;
  }

  /* see superclass */
  @Override
  public ProcessExecutionList findProcessExecutions(Long projectId,
    String query, PfsParameterJpa pfs, String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - find processExecutions " + query);

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/find" + "?projectId=" + projectId + "&query="
        + URLEncoder.encode(query == null ? "" : query, "UTF-8")
            .replaceAll("\\+", "%20"));
    final String pfsString = ConfigUtility
        .getStringForGraph(pfs == null ? new PfsParameterJpa() : pfs);
    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).post(Entity.xml(pfsString));

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return ConfigUtility.getGraphForString(resultString,
        ProcessExecutionListJpa.class);

  }

  /* see superclass */
  @Override
  public ProcessExecutionList findCurrentlyExecutingProcesses(Long projectId,
    String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - find currently executing processes ");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/executing" + "?projectId=" + projectId);
    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return ConfigUtility.getGraphForString(resultString,
        ProcessExecutionListJpa.class);

  }

  /* see superclass */
  @Override
  public void removeProcessExecution(Long projectId, Long id, Boolean cascade,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - remove processExecution " + id);
    validateNotEmpty(id, "id");
    Client client = ClientBuilder.newClient();
    WebTarget target = client
        .target(config.getProperty("base.url") + "/process/execution/" + id
            + "?projectId=" + projectId + (cascade ? "&cascade=true" : ""));

    if (id == null)
      return;

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).delete();

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /* see superclass */
  @Override
  public AlgorithmConfig addAlgorithmConfig(Long projectId, Long processId,
    AlgorithmConfigJpa algorithmConfig, String authToken) throws Exception {
    Logger.getLogger(getClass()).debug(
        "AlgorithmConfig Client - add algorithmConfig" + algorithmConfig);

    final Client client = ClientBuilder.newClient();
    final WebTarget target =
        client.target(config.getProperty("base.url") + "/process/config/algo"
            + "?projectId=" + projectId + "&processId=" + processId);

    final String algorithmConfigString = ConfigUtility.getStringForGraph(
        algorithmConfig == null ? new AlgorithmConfigJpa() : algorithmConfig);

    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .put(Entity.xml(algorithmConfigString));

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(resultString);
    }

    // converting to object
    AlgorithmConfigJpa result =
        ConfigUtility.getGraphForString(resultString, AlgorithmConfigJpa.class);

    return result;
  }

  /* see superclass */
  @Override
  public AlgorithmConfig newAlgorithmConfig(Long projectId, Long processId,
    String key, String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("AlgorithmConfig Client - new algorithmConfig" + key);

    final Client client = ClientBuilder.newClient();
    final WebTarget target =
        client.target(config.getProperty("base.url") + "/config/algo/" + key
            + "/new" + "?projectId=" + projectId + "&processId=" + processId);

    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(resultString);
    }

    // converting to object
    AlgorithmConfigJpa result =
        ConfigUtility.getGraphForString(resultString, AlgorithmConfigJpa.class);

    return result;
  }

  /* see superclass */
  @Override
  public void updateAlgorithmConfig(Long projectId, Long processId,
    AlgorithmConfigJpa algorithmConfig, String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - update algorithmConfig " + algorithmConfig);
    Client client = ClientBuilder.newClient();
    WebTarget target =
        client.target(config.getProperty("base.url") + "/process/config/algo"
            + "?projectId=" + projectId + "&processId=" + processId);

    String algorithmConfigString = ConfigUtility.getStringForGraph(
        algorithmConfig == null ? new AlgorithmConfigJpa() : algorithmConfig);
    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .post(Entity.xml(algorithmConfigString));

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /* see superclass */
  @Override
  public void validateAlgorithmConfig(Long projectId, Long processId,
    AlgorithmConfigJpa algorithmConfig, String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - validate algorithmConfig " + algorithmConfig);
    Client client = ClientBuilder.newClient();
    WebTarget target = client
        .target(config.getProperty("base.url") + "/process/config/algo/validate"
            + "?projectId=" + projectId + "&processId=" + processId);

    String algorithmConfigString = ConfigUtility.getStringForGraph(
        algorithmConfig == null ? new AlgorithmConfigJpa() : algorithmConfig);
    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .post(Entity.xml(algorithmConfigString));

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /* see superclass */
  @Override
  public void removeAlgorithmConfig(Long projectId, Long id, String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - remove algorithmConfig " + id);
    validateNotEmpty(id, "id");
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config/algo/" + id + "?projectId=" + projectId);

    if (id == null)
      return;

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).delete();

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // do nothing, successful
    } else {
      throw new Exception("Unexpected status - " + response.getStatus());
    }
  }

  /**
   * Returns the algorithm config.
   *
   * @param projectId the project id
   * @param id the id
   * @param authToken the auth token
   * @return the algorithm config
   * @throws Exception the exception
   */
  @Override
  public AlgorithmConfig getAlgorithmConfig(Long projectId, Long id,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - get algorithmConfig " + id);
    validateNotEmpty(id, "id");

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config/algo/" + id + "?projectId=" + projectId);

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    AlgorithmConfigJpa algorithmConfig =
        ConfigUtility.getGraphForString(resultString, AlgorithmConfigJpa.class);
    return algorithmConfig;
  }

  /* see superclass */
  @Override
  public KeyValuePairList getAlgorithmsForType(Long projectId, String type,
    String authToken) throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - get release Algorithms");

    validateNotEmpty(type, "type");
    validateNotEmpty(projectId, "projectId");

    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/algo/" + type + "?projectId=" + projectId);

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    KeyValuePairList releaseAlgorithms =
        ConfigUtility.getGraphForString(resultString, KeyValuePairList.class);
    return releaseAlgorithms;
  }

  /* see superclass */
  @Override
  public Long executeProcess(Long projectId, Long id, Boolean background,
    String authToken) throws Exception {

    Logger.getLogger(getClass()).debug("Process Client - execute process");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/" + id + "/execute?projectId=" + projectId
        + (background ? "&background=true" : ""));
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Long.parseLong(resultString);

  }

  /* see superclass */
  @Override
  public Long stepProcess(Long projectId, Long id, Integer step,
    Boolean background, String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - run next step of algo");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/" + id + "/step?projectId=" + projectId
        + (background ? "&background=true" : "")
        + (step == null ? "&step=" + step : ""));
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Long.parseLong(resultString);

  }

  @Override
  public Long prepareProcess(Long projectId, Long id, String authToken)
    throws Exception {

    Logger.getLogger(getClass()).debug(
        "Process Client - find progress of currently executing algorithm");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/config/" + id + "/prepare?projectId=" + projectId);
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Long.parseLong(resultString);

  }

  /**
   * Prepare and execute process.
   *
   * @param projectId the project id
   * @param configId the config id
   * @param background the background
   * @param authToken the auth token
   * @return the long
   * @throws Exception the exception
   */
  public Long prepareAndExecuteProcess(Long projectId, Long configId,
    Boolean background, String authToken) throws Exception {

    Long executionId = prepareProcess(projectId, configId, authToken);
    executionId = executeProcess(projectId, executionId, background, authToken);
    return executionId;

  }

  /* see superclass */
  @Override
  public Long cancelProcess(Long projectId, Long id, String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .debug("Process Client - cancel process execution " + id);
    Client client = ClientBuilder.newClient();
    WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/" + id + "/cancel" + "?projectId=" + projectId);

    Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Long.parseLong(resultString);
  }

  /* see superclass */
  @Override
  public Long restartProcess(Long projectId, Long id, Boolean background,
    String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - restart a previously canceled process");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/execution/" + id + "/restart?projectId=" + projectId
        + (background ? "&background=true" : ""));
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Long.parseLong(resultString);

  }

  /* see superclass */
  @Override
  public Integer getProcessProgress(Long projectId, Long id, String authToken)
    throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - find progress of currently executing process");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/" + id + "/progress?projectId=" + projectId);
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Integer.parseInt(resultString);

  }

  /* see superclass */
  @Override
  public Integer getAlgorithmProgress(Long projectId, Long id, String authToken)
    throws Exception {

    Logger.getLogger(getClass()).debug(
        "Process Client - find progress of currently executing algorithm");

    validateNotEmpty(projectId, "projectId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/algo/" + id + "/progress?projectId=" + projectId);
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Integer.parseInt(resultString);

  }

  @Override
  public String getProcessLog(Long projectId, Long processExecutionId,
    String query, String authToken) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Process Client - find log entries of specified process execution");

    validateNotEmpty(projectId, "projectId");
    validateNotEmpty(processExecutionId, "processExecutionId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/" + processExecutionId + "/log?projectId=" + projectId
        + "&query=" + URLEncoder.encode(query == null ? "" : query, "UTF-8")
            .replaceAll("\\+", "%20"));
    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return resultString;
  }

  @Override
  public String getAlgorithmLog(Long projectId, Long algorithmExecutionId,
    String query, String authToken) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Process Client - find log entries of specified process execution");

    validateNotEmpty(projectId, "projectId");
    validateNotEmpty(algorithmExecutionId, "algorithmExecutionId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target =
        client.target(config.getProperty("base.url") + "/process/algo/"
            + algorithmExecutionId + "/log?projectId=" + projectId + "&query="
            + URLEncoder.encode(query == null ? "" : query, "UTF-8")
                .replaceAll("\\+", "%20"));
    final Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return resultString;
  }

  /* see superclass */
  @Override
  public ProcessConfig importProcessConfig(
    FormDataContentDisposition contentDispositionHeader, InputStream in,
    Long projectId, String authToken) throws Exception {

    Logger.getLogger(getClass())
        .debug("Process Client - import process config");
    validateNotEmpty(projectId, "projectId");

    StreamDataBodyPart fileDataBodyPart = new StreamDataBodyPart("file", in,
        "filename.dat", MediaType.APPLICATION_OCTET_STREAM_TYPE);
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.bodyPart(fileDataBodyPart);

    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(MultiPartFeature.class);
    Client client = ClientBuilder.newClient(clientConfig);

    WebTarget target = client.target(config.getProperty("base.url")
        + "/config/import" + "?projectId=" + projectId);

    Response response = target.request(MediaType.APPLICATION_XML)
        .header("Authorization", authToken)
        .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));

    String resultString = response.readEntity(String.class);

    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }
    // converting to object
    return ConfigUtility.getGraphForString(resultString,
        ProcessConfigJpa.class);

  }

  @Override
  public InputStream exportProcessConfig(Long projectId, Long processId,
    String authToken) throws Exception {
    Logger.getLogger(getClass()).debug(
        "Refset Client - export process - " + projectId + ", " + processId);

    validateNotEmpty(projectId, "projectId");
    validateNotEmpty(processId, "processId");
    Client client = ClientBuilder.newClient();
    WebTarget target =
        client.target(config.getProperty("base.url") + "/config/export"
            + "?projectId=" + projectId + "&processId=" + processId);
    Response response = target.request(MediaType.APPLICATION_OCTET_STREAM)
        .header("Authorization", authToken).post(Entity.text(""));

    InputStream in = response.readEntity(InputStream.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }
    return in;
  }

  /* see superclass */
  @Override
  public Integer testQuery(Long projectId, Long processId, QueryType queryType,
    QueryStyle queryStyle, String query, String objectTypeName,
    String authToken) throws Exception {

    Logger.getLogger(getClass()).debug("Process Client - test query execution");

    validateNotEmpty(projectId, "projectId");
    validateNotEmpty(processId, "processId");

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(config.getProperty("base.url")
        + "/process/testquery?projectId=" + projectId + "&processId="
        + processId + "&queryType=" + queryType + "&queryStyle=" + queryStyle
        + "&query=" + URLEncoder.encode(query == null ? "" : query, "UTF-8")
        + (objectTypeName == null ? "" : "&objectTypeName=" + objectTypeName));
    final Response response = target.request(MediaType.TEXT_PLAIN)
        .header("Authorization", authToken).get();

    final String resultString = response.readEntity(String.class);
    if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
      // n/a
    } else {
      throw new Exception(response.toString());
    }

    // converting to object
    return Integer.parseInt(resultString);
  }

}
