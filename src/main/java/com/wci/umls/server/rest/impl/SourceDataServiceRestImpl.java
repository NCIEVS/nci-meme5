/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.web.multipart.MultipartFile;

import com.wci.umls.server.model.algo.SourceData;
import com.wci.umls.server.model.algo.SourceDataFile;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.KeyValuePairList;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.helpers.LogEntry;
import com.wci.umls.server.helpers.PfsParameter;
import com.wci.umls.server.helpers.SourceDataFileList;
import com.wci.umls.server.helpers.SourceDataList;
import com.wci.umls.server.helpers.StringList;
import com.wci.umls.server.jpa.model.SourceDataFileJpa;
import com.wci.umls.server.jpa.model.SourceDataJpa;
import com.wci.umls.server.jpa.model.helpers.PfsParameterJpa;
import com.wci.umls.server.jpa.model.helpers.SourceDataListJpa;
import com.wci.umls.server.jpa.services.ProjectServiceJpa;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.jpa.services.SourceDataServiceJpa;
import com.wci.umls.server.jpa.services.helper.SourceDataFileUtility;
import com.wci.umls.server.jpa.services.rest.SourceDataServiceRest;
import com.wci.umls.server.services.ProjectService;
import com.wci.umls.server.services.SecurityService;
import com.wci.umls.server.services.SourceDataService;
import com.wci.umls.server.services.handlers.SourceDataHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST implementation for {@link SourceDataServiceRest}.
 */
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping(value = "/file")
@Path("/file")
@Api(value = "/file")
@SwaggerDefinition(info = @Info(description = "Operations supporting file uploading and importing.", title = "Source Data API", version = "1.0.1"))
@Consumes({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
@Produces({
    MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML
})
public class SourceDataServiceRestImpl extends RootServiceRestImpl
    implements SourceDataServiceRest {

  /** The security service. */
  private SecurityService securityService;

  /**
   * Instantiates an empty {@link SourceDataServiceRestImpl}.
   *
   * @throws Exception the exception
   */
  public SourceDataServiceRestImpl() throws Exception {
    securityService = new SecurityServiceJpa();
  }

  /**
   * Upload source data file.
   *
   * @param multipartFile the multipart file
   * @param unzip the unzip
   * @param authToken the auth token
   * @throws Exception the exception
   */
  @Path("/upload/{id}")
  @RequestMapping(value = "/upload/{id}", method = RequestMethod.POST)
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public void uploadSourceDataFile(
    @RequestParam("file") MultipartFile multipartFile,
    @RequestParam(value = "unzip", required = false, defaultValue = "false") boolean unzip,
    @ApiParam(value = "Source data id, e.g. 1", required = true) @PathVariable("id") Long sourceDataId,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    String fileName = multipartFile.getOriginalFilename() != null
        ? multipartFile.getOriginalFilename() : "UNKNOWN FILE";
    try (InputStream fileInputStream = multipartFile.getInputStream()) {
      uploadSourceDataFile(fileInputStream, fileName, unzip, sourceDataId, authToken);
    }
  }

  /* see superclass */
  @Override
  public void uploadSourceDataFile(InputStream fileInputStream,
    FormDataContentDisposition contentDispositionHeader, boolean unzip, Long sourceDataId,
    String authToken) throws Exception {
    String fileName = contentDispositionHeader != null ? contentDispositionHeader.getFileName()
        : "UNKNOWN FILE";
    uploadSourceDataFile(fileInputStream, fileName, unzip, sourceDataId, authToken);
  }

  /**
   * Uploads a source data file stream.
   *
   * @param fileInputStream the file input stream
   * @param fileName the file name
   * @param unzip the unzip flag
   * @param sourceDataId the source data id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  private void uploadSourceDataFile(InputStream fileInputStream, String fileName, boolean unzip,
    Long sourceDataId, String authToken) throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Source Data): /upload "
        + fileName + " unzip=" + unzip + " authToken=" + authToken);

    final SourceDataService service = new SourceDataServiceJpa();
    SourceData sourceData = null;
    try {
      final String userName = authorizeApp(securityService, authToken,
          "upload source data files", UserRole.USER);

      // get the source data to append files to
      sourceData = service.getSourceData(sourceDataId);

      if (sourceData == null) {
        throw new Exception(
            "Source data with id " + sourceDataId + " does not exist");
      }

      // get the base destination folder (by source data id)
      String destinationFolder =
          PropertyUtility.getProperties().getProperty("source.data.dir")
              + File.separator + sourceDataId.toString();

      final List<File> files = new ArrayList<>();
      // if unzipping requested and file is valid, extract compressed file to
      // destination folder
      if (unzip == true) {
        files.addAll(SourceDataFileUtility.extractCompressedSourceDataFile(
            fileInputStream, destinationFolder, fileName));
      }
      // otherwise, simply write the input stream
      else {
        files.add(SourceDataFileUtility.writeSourceDataFile(fileInputStream,
            destinationFolder, fileName));

      }

      // Iterate through file list and add source data files.
      for (final File uploadedFile : files) {
        final SourceDataFile sdf = new SourceDataFileJpa();
        sdf.setName(uploadedFile.getName());
        sdf.setPath(uploadedFile.getAbsolutePath());
        sdf.setDirectory(uploadedFile.isDirectory());
        sdf.setSize(uploadedFile.length());
        sdf.setTimestamp(new Date());
        sdf.setLastModifiedBy(userName);
        sdf.setSourceData(sourceData);

        sourceData.getSourceDataFiles().add(sdf);

        service.addSourceDataFile(sdf);
      }

      // finally, update the source data object itself
      service.updateSourceData(sourceData);

    } catch (Exception e) {
      handleException(e, "uploading a source data file");
    } finally {
      service.close();
      securityService.close();
    }
  }

  /**
   * Add source data file.
   *
   * @param sourceDataFile the source data file
   * @param authToken the auth token
   * @return the source data file
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/add", method = RequestMethod.PUT)
  @PUT
  @Path("/add")
  public SourceDataFile addSourceDataFile(
    @ApiParam(value = "SourceDataFile to add", required = true) @RequestBody SourceDataFileJpa sourceDataFile,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    Logger.getLogger(getClass()).info("RESTful call (Source Data): /add");
    final SourceDataService service = new SourceDataServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "add source data file", UserRole.USER);

      sourceDataFile.setLastModifiedBy(userName);
      return service.addSourceDataFile(sourceDataFile);

    } catch (Exception e) {
      handleException(e, "update source data files");
    } finally {
      service.close();
      securityService.close();
    }
    return null;
  }

  /**
   * Update source data file.
   *
   * @param sourceDataFile the source data file
   * @param authToken the auth token
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/update", method = RequestMethod.POST)
  @POST
  @Path("/update")
  public void updateSourceDataFile(
    @ApiParam(value = "SourceDataFile to update", required = true) @RequestBody SourceDataFileJpa sourceDataFile,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Source Data): /update");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "add source data file", UserRole.ADMINISTRATOR);

      sourceDataFile.setLastModifiedBy(userName);
      service.updateSourceDataFile(sourceDataFile);

    } catch (Exception e) {
      handleException(e, "update source data files");
    } finally {
      service.close();
      securityService.close();
    }
  }

  /**
   * Remove source data file.
   *
   * @param id the id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/remove/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("/remove/{id}")
  public void removeSourceDataFile(
    @ApiParam(value = "SourceDataFile id, e.g. 5", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /remove/" + id);

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "delete source data file",
          UserRole.USER);

      final SourceDataFile sourceDataFile = service.getSourceDataFile(id);

      try {

        // physically remove the file
        final File file = new File(sourceDataFile.getPath());
        file.delete();

      } catch (Exception e) {
        Logger.getLogger(getClass())
            .warn("Unexpected error removing file " + sourceDataFile.getPath());
      }

      // remove this entry from its source data
      SourceData sourceData = sourceDataFile.getSourceData();
      sourceData.getSourceDataFiles().remove(sourceDataFile);
      service.updateSourceData(sourceData);

      // remove the database entry
      service.removeSourceDataFile(sourceDataFile.getId());

    } catch (Exception e) {
      handleException(e, "delete source data files");
    } finally {
      service.close();
      securityService.close();
    }
  }

  /**
   * Find source data files for query.
   *
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the source data file list
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/find", method = RequestMethod.GET)
  @GET
  @Path("/find")
  @ApiOperation(value = "Query source data files", notes = "Returns list of details for uploaded files returned by query", response = StringList.class)
  public SourceDataFileList findSourceDataFiles(
    @ApiParam(value = "String query, e.g. SNOMEDCT", required = true) @RequestParam(value = "query", required = false) String query,
    @ApiParam(value = "Paging/filtering/sorting object", required = false) @RequestBody PfsParameter pfsParameter,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /find - " + query);

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "search for source data files",
          UserRole.USER);

      return service.findSourceDataFiles(query, pfsParameter);

    } catch (Exception e) {
      handleException(e, "search for source data files");
      return null;
    } finally {
      service.close();
      securityService.close();
    }

  }

  /**
   * Add source data.
   *
   * @param sourceData the source data
   * @param authToken the auth token
   * @return the source data
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @Path("/data/add")
  @RequestMapping(value = "/data/add", method = RequestMethod.PUT)
  @PUT
  public SourceData addSourceData(
    @ApiParam(value = "Source data to add", required = true) @RequestBody SourceDataJpa sourceData,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Source Data): /data/add");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "add new source data", UserRole.USER);

      sourceData.setLastModifiedBy(userName);
      return service.addSourceData(sourceData);

    } catch (Exception e) {
      handleException(e, "adding new source data");
      return null;
    } finally {
      service.close();
      securityService.close();
    }

  }

  /**
   * Update source data.
   *
   * @param sourceData the source data
   * @param authToken the auth token
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @Path("/data/update")
  @RequestMapping(value = "/data/update", method = RequestMethod.POST)
  @POST
  public void updateSourceData(
    @ApiParam(value = "Source data to update", required = true) @RequestBody SourceDataJpa sourceData,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/update");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      final String userName = authorizeApp(securityService, authToken,
          "add new source data", UserRole.USER);

      sourceData.setLastModifiedBy(userName);
      service.updateSourceData(sourceData);

    } catch (Exception e) {
      handleException(e, "adding new source data");
    } finally {
      service.close();
      securityService.close();
    }

  }

  /**
   * Remove source data.
   *
   * @param id the id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "data/remove/{id}", method = RequestMethod.DELETE)
  @DELETE
  @Path("data/remove/{id}")
  public void removeSourceData(
    @ApiParam(value = "SourceData id, e.g. 5", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/remove/" + id);

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken,
          "delete source data with id " + id, UserRole.USER);

      // delete the source data files
      String sdDir =
          PropertyUtility.getProperties().getProperty("source.data.dir")
              + File.separator + id.toString();

      ConfigUtility.deleteDirectory(new File(sdDir));

      // remove the source data
      service.removeSourceData(id);

    } catch (Exception e) {
      handleException(e, "delete source data");
    } finally {
      service.close();
      securityService.close();
    }
  }

  /**
   * Find source data for query.
   *
   * @param query the query
   * @param pfsParameter the pfs parameter
   * @param authToken the auth token
   * @return the source data list
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/data/find", method = RequestMethod.GET)
  @GET
  @Path("/data/find")
  @ApiOperation(value = "Query source data files", notes = "Returns list of details for uploaded files returned by query", response = StringList.class)
  public SourceDataList findSourceData(
    @ApiParam(value = "String query, e.g. SNOMEDCT", required = true) @RequestParam(value = "query", required = false) String query,
    @ApiParam(value = "Paging/filtering/sorting object", required = false) @RequestBody PfsParameter pfsParameter,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/find" + query);

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get source datas",
          UserRole.USER);

      final SourceDataList list = service.findSourceDatas(query, pfsParameter);

      // lazy initialize source data files
      for (final SourceData sd : list.getObjects()) {
        sd.getSourceDataFiles().size();
      }
      return list;
    } catch (Exception e) {
      handleException(e, "retrieving uploaded file list");
      return null;
    } finally {
      service.close();
      securityService.close();
    }

  }

  /**
   * Gets the loader names.
   *
   * @param authToken the auth token
   * @return the loader names
   * @throws Exception the exception
   */
  /* see superclass */
  @Override
  @RequestMapping(value = "/data/sourceDataHandlers", method = RequestMethod.GET)
  @GET
  @Path("/data/sourceDataHandlers")
  @ApiOperation(value = "Get source data handler names", notes = "Gets all loader names.", response = StringList.class)
  public KeyValuePairList getSourceDataHandlerNames(
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/loaders");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get source datas",
          UserRole.USER);

      return service.getSourceDataHandlerNameAndClassPairs();

    } catch (Exception e) {
      handleException(e, "retrieving uploaded file list");
      return null;
    } finally {
      service.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/data/id/{id}", method = RequestMethod.GET)
  @GET
  @Path("/data/id/{id}")
  @ApiOperation(value = "Get source data by id", notes = "Gets a source data object by Hibernate id", response = SourceDataJpa.class)
  public SourceData getSourceData(
    @ApiParam(value = "Source data id, e.g. 1", required = true) @PathVariable("id") Long id,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {

    // NOTE: Debug as used for polling
    Logger.getLogger(getClass())
        .debug("RESTful call (Source Data): /data/loaders");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get source datas",
          UserRole.USER);
      SourceData sourceData = service.getSourceData(id);
      // lazy initialize source data files
      if (sourceData != null) {
        sourceData.getSourceDataFiles().size();
      }
      return sourceData;
    } catch (Exception e) {
      handleException(e, "retrieving uploaded file list");
      return null;
    } finally {
      service.close();
      securityService.close();
    }
  }

  /* see superclass */
  @Override
  @RequestMapping(value = "/data/all", method = RequestMethod.GET)
  @GET
  @Path("/data/all")
  @ApiOperation(value = "Get source datas", notes = "Gets all source datas", response = SourceDataListJpa.class)
  public SourceDataList getSourceData(
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/loaders");

    final SourceDataService service = new SourceDataServiceJpa();
    try {
      authorizeApp(securityService, authToken, "get source datas",
          UserRole.USER);
      SourceDataList sourceDataList = service.getSourceDatas();
      // lazy initialize source data files
      return sourceDataList;
    } catch (Exception e) {
      handleException(e, "retrieving uploaded file list");
      return null;
    } finally {
      service.close();
      securityService.close();
    }
  }

  @Override
  @RequestMapping(value = "/data/load", method = RequestMethod.POST)
  @POST
  @Path("/data/load")
  @ApiOperation(value = "Load data from source data configuration", notes = "Invokes loading of data based on source data files and configuration")
  public void loadFromSourceData(
    @ApiParam(value = "Run as background process", required = false) @RequestParam(value = "background", required = false) Boolean background,
    @ApiParam(value = "Source data to load from", required = true) @RequestBody SourceDataJpa sourceData,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info("RESTful call (Source Data): /data/load "
        + (sourceData == null ? "No source data" : sourceData.getName()));

    try {
      final String userName = authorizeApp(securityService, authToken,
          "load from source data", UserRole.USER);

      final Exception[] exceptions = new Exception[1];
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          SourceDataHandler handler = null;
          try {
            if (sourceData == null) {
              throw new LocalException("Source dat handler is not set");
            }
            // instantiate the handler
            final Class<?> sourceDataHandlerClass =
                Class.forName(sourceData.getHandler());
            handler = (SourceDataHandler) sourceDataHandlerClass.newInstance();
            handler.setLastModifiedBy(userName);
            handler.setSourceData(sourceData);
            handler.compute();

          } catch (Exception e) {
            exceptions[0] = e;
            handleException(e, " during execution of load from source data");
          } finally {
            if (handler != null) {
              try {
                handler.close();
              } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }
      });
      if (background != null && background == true) {
        t.start();
      } else {
        t.start();
        t.join();
        if (exceptions[0] != null) {
          throw new Exception(exceptions[0]);
        }
      }
    } catch (Exception e) {
      handleException(e,
          " attempting to load data from source data configuration");
    } finally {
      securityService.close();
    }
  }

  @Override
  @RequestMapping(value = "/data/remove", method = RequestMethod.POST)
  @POST
  @Path("/data/remove")
  @ApiOperation(value = "Remove data from source data configuration", notes = "Invokes removing of data based on source data files and configuration")
  public void removeFromSourceData(
    @ApiParam(value = "Run as background process", required = false) @RequestParam(value = "background", required = false) Boolean background,
    @ApiParam(value = "Source data to removed loaded data for", required = true) @RequestBody SourceDataJpa sourceData,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass())
        .info("RESTful call (Source Data): /data/remove");

    try {
      final String userName = authorizeApp(securityService, authToken,
          "remove loaded data from source data", UserRole.ADMINISTRATOR);

      final Exception[] exceptions = new Exception[1];
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          SourceDataHandler handler = null;
          try {
            // instantiate the handler
            Class<?> sourceDataHandlerClass =
                Class.forName(sourceData.getHandler());
            handler = (SourceDataHandler) sourceDataHandlerClass.newInstance();
            handler.setLastModifiedBy(userName);
            handler.setSourceData(sourceData);
            handler.remove();

          } catch (Exception e) {
            handleException(e,
                " during removal of loaded data from source data");
          } finally {
            if (handler != null) {
              try {
                handler.close();
              } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        }
      });
      if (background != null && background == true) {
        t.start();
      } else {
        t.join();
        if (exceptions[0] != null) {
          throw new Exception(exceptions[0]);
        }
      }
    } catch (Exception e) {
      handleException(e,
          " attempting to load data from source data configuration");
    } finally {
      securityService.close();
    }
  }

  @Override
  @RequestMapping(value = "/data/cancel", method = RequestMethod.POST)
  @POST
  @Path("/data/cancel")
  @ApiOperation(value = "Load data from source data configuration", notes = "Invokes loading of data based on source data files and configuration")
  public void cancelFromSourceData(
    @ApiParam(value = "Source data running process", required = true) @RequestBody SourceDataJpa sourceData,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "RESTful call (Source Data): /data/cancel " + sourceData.toString());

    try {
      final String userName = authorizeApp(securityService, authToken,
          "cancel from source data", UserRole.USER);

      // instantiate the handler
      Class<?> sourceDataHandlerClass = Class.forName(sourceData.getHandler());
      SourceDataHandler handler =
          (SourceDataHandler) sourceDataHandlerClass.newInstance();
      handler.setLastModifiedBy(userName);
      handler.setSourceData(sourceData);
      handler.cancel();

    } catch (Exception e) {
      handleException(e,
          " attempting to cancel data from source data configuration");
    } finally {
      securityService.close();
    }
  }

  /* see superclass */
  @RequestMapping(value = "/log", method = RequestMethod.GET)
  @GET
  @Path("/log")
  @Produces("text/plain")
  @ApiOperation(value = "Get log entries", notes = "Returns log entries for specified query parameters", response = String.class)
  @Override
  public String getLog(
    @ApiParam(value = "Terminology, e.g. SNOMED_CT", required = true) @RequestParam(value = "terminology", required = false) String terminology,
    @ApiParam(value = "Version, e.g. 20150131", required = true) @RequestParam(value = "version", required = false) String version,
    @ApiParam(value = "Activity, e.g. EDITING", required = true) @RequestParam(value = "activity", required = false) String activity,
    @ApiParam(value = "Lines, e.g. 5", required = false) @RequestParam(value = "lines", required = false, defaultValue = "0") int lines,
    @ApiParam(value = "Authorization token, e.g. 'author1'", required = true) @RequestHeader(value = "Authorization", required = false) String authToken)
    throws Exception {
    // NOTE: Debug as used for polling
    Logger.getLogger(getClass()).debug("RESTful call (Source Data): /log/"
        + terminology + ", " + version + ", " + activity + ", " + lines);

    final ProjectService projectService = new ProjectServiceJpa();
    try {
      authorizeApp(securityService, authToken,
          "remove loaded data from source data", UserRole.ADMINISTRATOR);

      // Precondition checking -- must have terminology/version OR projectId set
      if (terminology == null && version == null) {
        throw new LocalException("terminology/version must be set");
      }

      PfsParameter pfs = new PfsParameterJpa();
      pfs.setStartIndex(0);
      pfs.setMaxResults(lines);
      pfs.setAscending(false);
      pfs.setSortField("lastModified");

      String query = "";

      if (terminology != null) {
        query +=
            (query.length() == 0 ? "" : " AND ") + "terminology:" + terminology;
      }
      if (version != null) {
        query += (query.length() == 0 ? "" : " AND ") + "version:" + version;
      }

      if (activity != null) {
        query += " AND activity:" + activity;
      }

      final List<LogEntry> entries = projectService.findLogEntries(query, pfs);

      final StringBuilder log = new StringBuilder();
      for (int i = entries.size() - 1; i >= 0; i--) {
        final LogEntry entry = entries.get(i);
        final StringBuilder message = new StringBuilder();
        message.append("[")
            .append(ConfigUtility.DATE_FORMAT4.format(entry.getLastModified()));
        message.append("] ");
        message.append(entry.getLastModifiedBy()).append(" ");
        message.append(entry.getMessage()).append("\n");
        log.append(message);
      }

      return log.toString();

    } catch (Exception e) {
      handleException(e, "trying to get log");
    } finally {
      projectService.close();
      securityService.close();
    }
    return null;
  }
}
