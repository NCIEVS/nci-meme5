/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.model.algo.SourceData;
import com.wci.umls.server.model.algo.SourceDataFile;
import com.wci.umls.server.jpa.model.SourceDataFileJpa;
import com.wci.umls.server.jpa.model.SourceDataJpa;
import com.wci.umls.server.jpa.services.SourceDataServiceJpa;
import com.wci.umls.server.jpa.services.handlers.RrfSourceDataHandler;
import com.wci.umls.server.services.SourceDataService;
import com.wci.umls.server.services.handlers.ExceptionHandler;

/**
 * Admin tool which loads RRF files into a source data object.
 *
 * <p>Usage:
 * <pre>
 *   ./gradlew adminLoadRrfSourceData \
 *       -Pterminology=SNOMEDCT_US -Pversion=latest -Pprefix=MR \
 *       -Pinput.dir=/data/rrf -Pmode=create
 * </pre>
 */
public class RrfSourceDataLoader extends AbstractLoader {

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(RrfSourceDataLoader.class.getName());

  @Override
  public void run() throws Exception {
    final String terminology = System.getProperty("terminology");
    final String version = System.getProperty("version");
    final String prefix = System.getProperty("prefix", "MR");
    final String inputDirProp = System.getProperty("input.dir");
    final String mode = System.getProperty("mode");

    LOG.info("Starting RRF source data load");
    LOG.info("  terminology = " + terminology);
    LOG.info("  version = " + version);
    LOG.info("  prefix = " + prefix);
    LOG.info("  mode = " + mode);
    LOG.info("  inputDir = " + inputDirProp);

    SourceDataService service = null;
    try {
      if ("create".equals(mode)) {
        createDb(false);
      }

      service = new SourceDataServiceJpa();

      if (inputDirProp == null) {
        throw new IllegalArgumentException("Input directory not specified");
      }
      final File dir = ConfigUtility.validateExistingDirectoryPath(inputDirProp,
          "input directory");

      final SourceDataFile sdFile = new SourceDataFileJpa();
      sdFile.setDirectory(true);
      sdFile.setLastModifiedBy("loader");
      sdFile.setName(dir.getName());
      sdFile.setPath(dir.getPath());
      sdFile.setSize(1000000L);
      sdFile.setTimestamp(new Date());
      service.addSourceDataFile(sdFile);
      LOG.info("    file = " + sdFile);

      final RrfSourceDataHandler loader = new RrfSourceDataHandler();

      final SourceData sourceData = new SourceDataJpa();
      sourceData.setName(getName(terminology, version));
      sourceData.setDescription("Set of RRF files loaded from " + dir);
      sourceData.setLastModifiedBy("loader");
      sourceData.setHandler(loader.getName());
      sourceData.getSourceDataFiles().add(sdFile);
      service.addSourceData(sourceData);
      LOG.info("    source data = " + sourceData);

      service.updateSourceDataFile(sdFile);

      final Properties p = new Properties();
      p.setProperty("prefix", prefix);
      loader.setSourceData(sourceData);
      loader.setProperties(p);
      loader.compute();
      loader.close();
      LOG.info("Done ...");

    } catch (Exception e) {
      try {
        ExceptionHandler.handleException(e, "Error loading sample source data");
      } catch (Exception e1) {
        e1.printStackTrace();
        throw e;
      }
    } finally {
      if (service != null) {
        try {
          service.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  /** Main entry point. */
  public static void main(String[] args) throws Exception {
    new RrfSourceDataLoader().run();
  }
}
