/*
 * Copyright 2017 Ciitizen, Inc.
 */
package com.wci.umls.server.rest.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wci.umls.server.model.algo.SourceData;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.jpa.services.SourceDataServiceJpa;
import com.wci.umls.server.jpa.services.rest.ConfigureServiceRest;
import com.wci.umls.server.jpa.services.rest.HistoryServiceRest;
import com.wci.umls.server.jpa.services.rest.SourceDataServiceRest;
import com.wci.umls.server.services.MetadataService;
import com.wci.umls.server.services.SourceDataService;

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
 * REST implementation for {@link HistoryServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/configure")
@Path("/configure")
@Tag(name = "Configure", description = "Operations to configure application")
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class ConfigureServiceRestImpl extends RootServiceRestImpl implements ConfigureServiceRest {

  /**
   * Instantiates an empty {@link ConfigureServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public ConfigureServiceRestImpl() throws Exception {
    // n/a
  }

  /**
   * Validate property.
   *
   * @param name the name
   * @param props the props
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private void validateProperty(String name, Properties props) throws Exception {
    if (props == null) {
      throw new Exception("Properties are null");
    }
    if (props.getProperty(name) == null || props.getProperty(name).isEmpty()) {
      throw new Exception("Property is empty: " + name);
    }

    if (props.getProperty(name).contains("${")) {
      throw new Exception("Configurable value " + name + " not set: " + props.getProperty(name));
    }
  }

  /**
   * Loads the Spring-style starting configuration.
   *
   * @return the properties
   * @throws Exception the exception
   */
  Properties getSpringStartingConfiguration() throws Exception {
    return PropertyUtility.loadApplicationProperties();
  }

  /**
   * Loads the starting configuration template for the configure flow.
   *
   * @return the starting properties
   * @throws Exception the exception
   */
  Properties getStartingConfiguration() throws Exception {
    final Properties springProperties = getSpringStartingConfiguration();
    if (springProperties != null) {
      Logger.getLogger(getClass()).info(
          "Loaded starting configuration from application.properties bridge");
      return springProperties;
    }
    throw new Exception(
        "Could not load starting configuration from application.properties resources");
  }

  /**
   * Initializes the configured database after config has been written.
   *
   * <p>This uses the legacy Hibernate create/update path retained for
   * transitional configure flows. Versioned schema evolution should use Flyway
   * admin tasks.
   *
   * @throws Exception the exception
   */
  void initializeConfiguredDatabase() throws Exception {
    MetadataService metadataService = null;
    PropertyUtility.setProperty("hibernate.hbm2ddl.auto", "create");
    try {
      metadataService = new MetadataServiceJpa();
    } finally {
      if (metadataService != null) {
        metadataService.close();
      }
      PropertyUtility.setProperty("hibernate.hbm2ddl.auto", "update");
    }
  }

  /**
   * Checks if application is configured.
   *
   * @return the release history
   * @throws Exception the exception
   */
  /* see superclass */
  @RequestMapping(value = "/configured", method = RequestMethod.GET)
  @GET
  @Override
  @Path("/configured")
  @Operation(summary = "Checks if application is configured",
      description = "Returns true if application is configured, false if not")
  public boolean isConfigured() throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Configure): /configure/configured");

    try {
      String configFileName = PropertyUtility.getLocalConfigFile();
      boolean configured =
          PropertyUtility.getProperties() != null || (new File(configFileName).exists());
      return configured;

    } catch (Exception e) {
      handleException(e, "checking if application is configured");
      return false;
    }
  }

  /* see superclass */
  @RequestMapping(value = "/configure", method = RequestMethod.POST)
  @POST
  @Override
  @Path("/configure")
  @Operation(summary = "Checks if application is configured",
      description = "Returns true if application is configured, false if not")
  public void configure(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Configuration parameters as JSON string", required = true) @RequestBody HashMap<String, String> parameters)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Configure): /configure/configure with parameters " + parameters.toString());

    // NOTE: Configure calls do not require authorization

    try {

      // construct name and check that the file does not already exist
      String configFileName = PropertyUtility.getLocalConfigFile();

      if (new File(configFileName).exists()) {
        throw new LocalException("System is already configured from file: " + configFileName);
      }

      // get the starting properties
      final Properties properties = getStartingConfiguration();

      // directly replace parameters by key
      for (final String key : parameters.keySet()) {
        if (properties.containsKey(key)) {
          properties.setProperty(key, parameters.get(key));
        }
      }

      // replace config file property values based on replacement pattern ${...}
      for (final Object key : new HashSet<>(properties.keySet())) {
        for (final String param : parameters.keySet()) {

          if (properties.getProperty(key.toString()).contains("${" + param + "}")) {
            properties.setProperty(key.toString(), properties.getProperty(key.toString())
                .replace("${" + param + "}", parameters.get(param)));
          }
        }
      }

      // validate the user-set properties
      validateProperty("source.data.dir", properties);
      validateProperty("hibernate.search.backend.directory.root", properties);
      validateProperty("jakarta.persistence.jdbc.url", properties);
      validateProperty("jakarta.persistence.jdbc.user", properties);
      validateProperty("jakarta.persistence.jdbc.password", properties);

      // TODO Test database connection with supplied parameters
      // Current (commented) code throws SQL Exceptions regarding no driver
      // found
      // e.g. No suitable driver found for
      // jdbc:mysql://127.0.0.1:3306/sskdb?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&useLocalSessionState=true
      // Check (1) existence, (2) credentials
      /*
       * try { java.sql.Connection con = DriverManager.getConnection(
       * properties.getProperty("jakarta.persistence.jdbc.url"),
       * properties.getProperty("jakarta.persistence.jdbc.user"),
       * properties.getProperty("jakarta.persistence.jdbc.password"));
       * con.getMetaData();
       * 
       * } catch (SQLException e) { throw new LocalException(
       * "Could not establish connection to database. Please check database name and credentials."
       * ); }
       */
      // create the local application folder
      File localFolder = new File(PropertyUtility.getLocalConfigFolder());
      if (!localFolder.exists()) {
        localFolder.mkdir();
      } else if (!localFolder.isDirectory()) {
        throw new LocalException(
            "Could not create local directory " + PropertyUtility.getLocalConfigFolder());
      }

      // prerequisite: application directory exists
      File f = new File(parameters.get("app.dir").toString());
      if (!f.exists()) {
        throw new LocalException(
            "Application directory does not exist: " + parameters.get("app.dir"));
      }

      Logger.getLogger(getClass()).info("Writing configuration file: " + configFileName);

      File configFile = new File(configFileName);

      // Make directories
      if (!configFile.getParentFile().exists()) {
        configFile.getParentFile().mkdirs();
      }
      
      try (final Writer writer = new FileWriter(configFile)) {
        properties.store(writer, "User-configured settings");
      }

      // reset the config properties and test retrieval
      System.setProperty("run.config." + PropertyUtility.getConfigLabel(), configFileName);
      PropertyUtility.setProperties(properties);
      if (PropertyUtility.getProperties() == null) {
        throw new LocalException("Failed to retrieve newly written properties");
      }

      /*
       * byte[] buffer = new byte[1024]; OutputStream os = null; InputStream is
       * = ConfigUtility.class.getResourceAsStream("/spelling.txt");
       * 
       * if (is != null) { os = new OutputStream(FILENAMEHERE) int len =
       * in.read(buffer); while (len != -1) { os.write(buffer, 0, len); len =
       * in.read(buffer); } } else {
       * Logger.getLogger(ConfigUtility.class.getName())
       * .error("Cannot find spelling file"); throw new
       * LocalException("Spelling file required for configuration"); }
       */
      //
      // Create the database
      //
      initializeConfiguredDatabase();

    } catch (Exception e) {
      handleException(e, "checking if application is configured");
    }
  }

  /* see superclass */
  @RequestMapping(value = "/destroy", method = RequestMethod.DELETE)
  @DELETE
  @Override
  @Path("/destroy")
  @Operation(summary = "Destroys and rebuilds the database",
      description = "Resets database to clean state and deletes any uploaded files")
  public void destroy(@Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Configure): /configure/destroy");

    // NOTE: Configure calls do not require authorization

    SourceDataService sourceDataService = null;
    try {

      sourceDataService = new SourceDataServiceJpa();

      //
      // Check precondition: last source data object must have failed process
      //
      List<SourceData> sourceDatas = sourceDataService.getSourceDatas().getObjects();

      if (sourceDatas.size() == 0) {
        throw new Exception("Cannot destroy database: fail condition not detected");
      }

      // sort source datas by descending last modified
      Collections.sort(sourceDatas, new Comparator<SourceData>() {
        @Override
        public int compare(SourceData sd1, SourceData sd2) {
          return sd2.getLastModified().compareTo(sd1.getLastModified());
        }
      });

      switch (sourceDatas.get(0).getStatus()) {
        case CANCELLED:
        case LOADING_FAILED:
        case REMOVAL_FAILED:
          // do nothing
          break;
        default:
          throw new LocalException("Cannot destroy database: fail condition not detected");
      }

      //
      // Delete all uploaded files using SourceDataServiceRet
      // NOTE: REST service used for file deletion
      //
      final SourceDataServiceRest sourceDataServiceRest = new SourceDataServiceRestImpl();
      for (final SourceData sd : sourceDatas) {
        sourceDataServiceRest.removeSourceData(sd.getId(), authToken);
      }

      //
      // Recreate the database using the legacy Hibernate create/update path.
      // Versioned schema evolution should use Flyway admin tasks.
      //
      MetadataService metadataService = null;
      PropertyUtility.setProperty("hibernate.hbm2ddl.auto", "create");
      try {
        metadataService = new MetadataServiceJpa();

        // close and re-open factory to trigger creation
        metadataService.closeFactory();
        metadataService.openFactory();
      } catch (Exception e) {
        throw e;
      } finally {
        if (metadataService != null) {
          metadataService.close();
        }

        // return mode to update
        PropertyUtility.setProperty("hibernate.hbm2ddl.auto", "update");

      }

    } catch (Exception e) {
      handleException(e, "resetting the database");
    } finally {
      if (sourceDataService != null) {
        sourceDataService.close();
      }
    }
  }

  /* see superclass */
  @RequestMapping(value = "/properties", method = RequestMethod.GET)
  @GET
  @Override
  @Path("/properties")
  @Produces({
      MediaType.APPLICATION_JSON
  })
  @Operation(summary = "Get configuration properties",
      description = "Gets user interface-relevant configuration properties")
  public Map<String, String> getConfigProperties() {
    Logger.getLogger(getClass()).info("RESTful call (Configure): /configure/properties");
    try {
      Map<String, String> map = new HashMap<>();
      for (final Map.Entry<Object, Object> o : PropertyUtility.getUiProperties().entrySet()) {
        map.put(o.getKey().toString(), o.getValue().toString());
      }
      return map;
    } catch (Exception e) {
      handleException(e, "getting ui config properties");
    } finally {
      // n/a
    }
    return null;
  }
}
