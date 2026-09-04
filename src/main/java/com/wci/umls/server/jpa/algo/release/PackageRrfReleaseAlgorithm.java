/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;

import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

/**
 * Algorithm for packaging RRF release into a .zip file.
 */
public class PackageRrfReleaseAlgorithm extends AbstractAlgorithm {

  /**
   * Instantiates an empty {@link PackageRrfReleaseAlgorithm}.
   *
   * @throws Exception the exception
   */
  public PackageRrfReleaseAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("PACKAGERRF");

  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    final File path = ConfigUtility.validateExistingDirectory(
        getProcessReleaseDirectory(), "process release directory");
    logInfo("  path " + path);

    final String filename = getProcess().getVersion() + ".zip";
    final File zipFile = ConfigUtility.resolveSourceDataPath(
        "release package file", getProcess().getInputPath(),
        getProcess().getVersion(), filename);
    logInfo("  zipFileName " + zipFile);

    if (zipFile.exists()) {
      throw new Exception("File already exists = " + zipFile.getAbsolutePath());
    }

    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());
    final File path = ConfigUtility.validateExistingDirectory(
        getProcessReleaseDirectory(), "process release directory");
    final String filename = getProcess().getVersion() + ".zip";
    final File zipFile = ConfigUtility.resolveFileUnderDirectory(path,
        filename, "release package file");

    final File pathMeta = ConfigUtility.validateExistingDirectory(
        getProcessReleaseMetaDirectory(), "process release META directory");
    logInfo("  pathMeta " + pathMeta);

// Removed for NM-263
//    final File mmsysPath =
//        new File(path, "/" + getProcess().getVersion() + "/MMSYS");
//    logInfo("  mmsysPath " + mmsysPath);

    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
    logInfo("  Process META");
    zipDirectory(pathMeta, out,
        pathMeta.getPath().length() + 1 - "/META".length());
//    logInfo("  Process MMSYS");
//    zipDirectory(mmsysPath, out, mmsysPath.getPath().length() + 1);

    // Add release.dat if it exists in the version folder
    final File releaseDat = ConfigUtility.resolveFileUnderDirectory(path,
        "release.dat", "release data file");
    if (releaseDat.exists()) {
      logInfo("  Process release.dat");
      final ZipEntry zipEntry = new ZipEntry("release.dat");
      out.putNextEntry(zipEntry);
      try (FileInputStream inputStream = new FileInputStream(releaseDat)) {
        IOUtils.copy(inputStream, out);
      }
      out.closeEntry();
    }

    out.close();
    logInfo("Finished " + getName());

  }

  /**
   * Zip directory.
   *
   * @param folder the folder
   * @param zipOutputStream the zip output stream
   * @param prefixLength the prefix length
   * @throws Exception the exception
   */
  public void zipDirectory(File folder, ZipOutputStream zipOutputStream,
    int prefixLength) throws Exception {
    final File[] files = folder.listFiles();
    if (files == null) {
      return;
    }
    for (final File file : files) {
      if (file.isFile()) {
        logInfo("    " + file.getName());
        final ZipEntry zipEntry =
            new ZipEntry(file.getPath().substring(prefixLength));
        zipOutputStream.putNextEntry(zipEntry);
        try (FileInputStream inputStream = new FileInputStream(file)) {
          IOUtils.copy(inputStream, zipOutputStream);
        }
        zipOutputStream.closeEntry();
        commitClearBegin();
      } else if (file.isDirectory()) {
        zipDirectory(file, zipOutputStream, prefixLength);
      }
    }

  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());

    // Remove the output zip file
    final File path = getProcessReleaseDirectory();
    logInfo("  path " + path);

    final String filename = getProcess().getVersion() + ".zip";
    final File zipFile = ConfigUtility.resolveFileUnderDirectory(path,
        filename, "release package file");
    if (zipFile.exists()) {
      FileUtils.fileDelete(zipFile.getAbsolutePath());
    }
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
  public void setProperties(Properties p) throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}
