/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.helper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.LocalException;

/**
 * Utility class for handling source data files.
 */
public class SourceDataFileUtility {

  /** Size of the buffer to read/write data. */
  private static final int BUFFER_SIZE = 4096;

  /**
   * Function to directly write a file to a destination folder from an input
   * stream.
   *
   * @param fileInputStream the input stream
   * @param destinationFolder the destination folder
   * @param fileName the name of the file
   * @return the file
   * @throws Exception the exception
   */
  public static File writeSourceDataFile(InputStream fileInputStream,
    File destinationFolder, String fileName) throws Exception {

    final File destinationDir = ConfigUtility.validateOrCreateDirectory(
        destinationFolder, "source data destination directory");
    final File outputFile = ConfigUtility.resolveFileUnderDirectory(
        destinationDir, fileName, "source data file");

    Logger.getLogger(SourceDataFileUtility.class).info(
        "Writing file " + outputFile);

    if (outputFile.exists()) {
      throw new LocalException("File " + fileName
          + " already exists. Write aborted.");

    }

    try (BufferedOutputStream bos =
        new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))) {
      byte[] bytesIn = new byte[BUFFER_SIZE];
      int read = 0;
      while ((read = fileInputStream.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }

    return outputFile;
  }

  /**
   * Extract compressed source data file.
   *
   * @param fileInputStream the file input stream
   * @param destinationFolder the destination folder
   * @param fileName the file name
   * @return the list
   * @throws Exception the exception thrown
   */
  public static List<File> extractCompressedSourceDataFile(
    InputStream fileInputStream, File destinationFolder, String fileName)
    throws Exception {

    final File destinationDir = ConfigUtility.validateOrCreateDirectory(
        destinationFolder, "source data destination directory");

    Logger.getLogger(SourceDataFileUtility.class).info(
        "Extracting zip file to " + destinationDir);

    List<File> files = new ArrayList<>();

    // convert file stream to zip input stream and get first entry
    ZipInputStream zipIn = new ZipInputStream(fileInputStream);
    ZipEntry entry = zipIn.getNextEntry();

    if (entry == null) {
      throw new LocalException("Could not unzip file " + fileName
          + ": not a ZIP file");
    }

    Logger.getLogger(SourceDataFileUtility.class)
        .info("  Cycling over entries");

    try {

      // iterates over entries in the zip file
      while (entry != null) {

        final String shortName = ConfigUtility.validateZipEntryPath(
            entry.getName(), "source data zip entry", true);
        if (shortName.isEmpty()) {
          zipIn.closeEntry();
          entry = zipIn.getNextEntry();
          continue;
        }
        final File outputFile = ConfigUtility.resolvePathUnderDirectory(
            destinationDir, "source data zip entry", shortName);

        Logger.getLogger(SourceDataFileUtility.class).info(
            "  Processing " + shortName);

        // construct local directory to match file structure
        if (entry.isDirectory()) {

          Logger.getLogger(SourceDataFileUtility.class).info(
              "    Directory detected, creating folder");
          if (outputFile.exists()) {
            throw new LocalException("Unzipped folder " + shortName
                + " already exists. Write aborted");
          }

          // create the directory
          ConfigUtility.ensureDirectoryExists(outputFile);
        }

        // if not a directory, simply extract the file
        else {

          Logger.getLogger(SourceDataFileUtility.class).info(
              "    File detected, extracting");

          if (outputFile.exists()) {
            throw new LocalException("Unzipped file " + shortName
                + " already exists. Write aborted.");
          }

          final File parent = outputFile.getParentFile();
          if (parent != null) {
            ConfigUtility.ensureDirectoryExists(parent);
          }
          File f = extractZipEntry(zipIn, outputFile);

          files.add(f);
        }

        // if not a valid directory, delete previously added files and throw
        // exception
        /*
         * else { for (final File f : files) { f.delete(); } throw new
         * LocalException( "Compressed file " + fileName +
         * " contains subdirectories. Upload aborted"); }
         */
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
      zipIn.close();

      return files;
    } catch (Exception e) {
      // TODO Delete any successfully extracted files on failed load
      if (e instanceof LocalException) {
        throw e;
      } else {
        throw new Exception(e);
      }
    }

  }

  /**
   * Private helper class. Extracts a zip entry (file entry)
   *
   * @param zipIn the zip in
   * @param filePath the file path
   * @return the file
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static File extractZipEntry(ZipInputStream zipIn, File filePath)
    throws IOException {

    Logger.getLogger(SourceDataFileUtility.class).info(
        "Extracting file " + filePath);

    try (BufferedOutputStream bos =
        new BufferedOutputStream(Files.newOutputStream(filePath.toPath()))) {
      byte[] bytesIn = new byte[BUFFER_SIZE];
      int read = 0;
      while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }

    // return the newly created file
    return filePath;

  }
}
