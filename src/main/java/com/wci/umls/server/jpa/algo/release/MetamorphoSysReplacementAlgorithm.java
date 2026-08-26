/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.model.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

/**
 * Algorithm to replace MetamorphoSys functionality.  Will create MRCOLS, MRFILES, MRDOC, MRSAB, release.dat
 */
public class MetamorphoSysReplacementAlgorithm extends AbstractAlgorithm {

  /** MRSAB VSAB field. */
  private static final int MRSAB_VSAB = 2;

  /** MRSAB RSAB field. */
  private static final int MRSAB_RSAB = 3;

  /** MRSAB SVER field. */
  private static final int MRSAB_SVER = 6;

  /** MRSAB CURVER field. */
  private static final int MRSAB_CURVER = 21;

  /** MRSAB SABIN field. */
  private static final int MRSAB_SABIN = 22;

  /** The email. */
  private String email;

  private File outputPath = null;

  /**
   * Instantiates an empty {@link MetamorphoSysReplacementAlgorithm}.
   *
   * @throws Exception the exception
   */
  public MetamorphoSysReplacementAlgorithm() throws Exception {
    // n/a
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    // Check the process input path
    final String path =
        PropertyUtility.getProperties().getProperty("source.data.dir")
            + File.separator + getProcess().getInputPath();

    final File pathAsFile = new File(path);
    if (!pathAsFile.exists()) {
      throw new LocalException(
          "Input path specified in process does not exist");
    }

    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());



    outputPath = new File(config.getProperty("source.data.dir") + "/"
	        + getProcess().getInputPath() + "/" + getProcess().getVersion() + "/"
	        + "META");
    logInfo("  outputPath: " + outputPath.getAbsolutePath());
    logInfo("  templatePath: " + getInputTemplatePath("MRCOLS.RRF").getParent());
    updateMrsab();
    updateMrcols();
    updateMrfiles();

    // Write release.dat
    logInfo("  Write release.dat file");

    final File releaseDat = new File(config.getProperty("source.data.dir") + "/"
        + getProcess().getInputPath() + "/" + getProcess().getVersion()
        + "/release.dat");

    final StringBuilder data = new StringBuilder();
    data.append("umls.release.name=" + getProcess().getVersion()).append("\n");
    data.append("umls.release.description=Base Release for "
        + getProcess().getVersion()).append("\n");
    data.append("umls.release.date=").append(getProcess().getVersion() + "01")
        .append("\n");
    appendIncludedReleaseProperties(data);

    // Write release.dat file
    Files.writeString(releaseDat.toPath(), data.toString(),
        StandardCharsets.UTF_8);


    logInfo("Finished " + getName());
  }





      public void updateMrfiles() throws Exception {
    	  String inputFile = "MRFILES.RRF";
          String outputFile = "MRFILES.mod";
          String backupFile = "MRFILES.bak";

          String mrfilesRow = "";
          final List<String> mrfilesLines = new ArrayList<>();

          try (BufferedReader reader = newTemplateReader(inputFile)) {

              String line;
              int lineNumber = 0;
              while ((line = reader.readLine()) != null) {
                  lineNumber++;
                  if (line.trim().isEmpty()) {
                      continue;
                  }
                  // Split the line by pipe character
                  String[] fields = line.split("\\|");
                  if (fields.length < 6) {
                      throw new LocalException("Bad MRFILES.RRF template row "
                          + lineNumber + ": " + line);
                  }

                  // Get the filename from the first field
                  String filename = fields[0];

                  if (filename.equals("MRFILES.RRF")) {
                      mrfilesRow = line;
                      continue;
                  }

                  try {
                      final Path filePath = outputPath.toPath().resolve(filename);
                      // Get file size in bytes
                      long fileSize = Files.size(filePath);

                      // Get the line count
                      long lineCount = countFileLines(filePath);

                      // Create new line with all fields except last one
                      mrfilesLines.add(getMrfilesLine(fields, lineCount,
                          fileSize));

                  } catch (NoSuchFileException e) {
                      System.out.println("Warning: File " + filename
                          + " not found. Skipping metadata row.");
                  }
              }
              if (ConfigUtility.isEmpty(mrfilesRow)) {
                  throw new LocalException("MRFILES.RRF template row not found");
              }
              mrfilesLines.add(getMrfilesLine(mrfilesRow.split("\\|"),
                  mrfilesLines.size() + 1, 0));
              mrfilesLines.sort(String::compareTo);
              Files.write(outputPath.toPath().resolve(outputFile), mrfilesLines,
                  StandardCharsets.UTF_8);

              adjustMrfilesByteCount(outputPath + File.separator + outputFile);

              // make a backup of the original MRFILES, and rename the modified one MRFILES.RRF
              File backup = new File(outputPath + File.separator + backupFile);
              File modified = new File(outputPath + File.separator + outputFile);
              File original = new File(outputPath + File.separator + inputFile);

              // Delete existing backup if it exists
              Files.deleteIfExists(backup.toPath());

              // Rename original to backup
              if (!original.renameTo(backup)) {
                  throw new IOException("Failed to rename " + inputFile + " to " + backup.getName());
              }

              // Rename modified to original
              if (!modified.renameTo(original)) {
                  // If second rename fails, try to restore original file
                  ConfigUtility.renameFile(backup, original);
                  throw new IOException("Failed to rename " + outputFile + " to " + inputFile);
              }

              // Delete original, bc modified is the new MRFILES
              //Files.deleteIfExists(original.toPath());

          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
              throw e;
          }
      }


          public void adjustMrfilesByteCount(String filename) throws IOException {
              final Path path = Paths.get(filename);
              final long actualByteCount = Files.size(path);
              final List<String> lines =
                  Files.readAllLines(path, StandardCharsets.UTF_8);
              int mrfilesIndex = -1;
              for (int i = 0; i < lines.size(); i++) {
                  if (lines.get(i).startsWith("MRFILES.RRF|")) {
                      mrfilesIndex = i;
                      break;
                  }
              }
              if (mrfilesIndex == -1) {
                  throw new IOException("MRFILES.RRF row not found in "
                      + filename);
              }

              final String[] fields = lines.get(mrfilesIndex).split("\\|");
              final long claimedRowCount = Long.parseLong(fields[4]);
              final long claimedByteCount = Long.parseLong(fields[5]);
              if (actualByteCount == claimedByteCount
                  && claimedRowCount == lines.size()) {
                  return;
              }

              lines.set(mrfilesIndex, getMrfilesLine(fields, lines.size(),
                  actualByteCount));
              Files.write(path, lines, StandardCharsets.UTF_8);

              // Recursive call to account for byte-count digit length changes.
              adjustMrfilesByteCount(filename);
          }

      /**
       * Returns an MRFILES row with updated row and byte counts.
       *
       * @param fields the MRFILES fields
       * @param lineCount the row count
       * @param fileSize the byte count
       * @return the MRFILES row
       */
      private String getMrfilesLine(String[] fields, long lineCount,
          long fileSize) {
          final StringBuilder newLine = new StringBuilder();
          for (int i = 0; i < fields.length - 2; i++) {
              newLine.append(fields[i]).append("|");
          }
          newLine.append(lineCount).append("|");
          newLine.append(fileSize).append("|");
          return newLine.toString();
      }

      /**
       * Returns the number of lines in a file.
       *
       * @param path the path
       * @return the line count
       * @throws IOException the IO exception
       */
      private long countFileLines(Path path) throws IOException {
          long count = 0;
          try (BufferedReader reader =
              Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
              while (reader.readLine() != null) {
                  count++;
              }
          }
          return count;
      }


      public void updateMrcols() throws Exception {
          String mrcolsFile = "MRCOLS.RRF";

          // parse template MRCOLS.RRF and create new one with updated column averages
          try (BufferedReader reader = newTemplateReader(mrcolsFile);
               BufferedWriter writer = new BufferedWriter(new FileWriter(
                   outputPath + File.separator + mrcolsFile, StandardCharsets.UTF_8))) {

              String line;
              int lineNumber = 0;
              while ((line = reader.readLine()) != null) {
                  lineNumber++;
                  if (line.trim().isEmpty()) {
                      continue;
                  }
                  // Split the line by pipe character
                  String[] fields = line.split("\\|");
                  if (fields.length < 8) {
                      throw new LocalException("Bad MRCOLS.RRF template row "
                          + lineNumber + ": " + line);
                  }

                  // Get the filename from the sixth field
                  String filename = fields[6];
                  if (!Files.isRegularFile(outputPath.toPath().resolve(filename))) {
                      System.out.println("Warning: File " + filename
                          + " not found. Skipping metadata row.");
                      continue;
                  }

                  // Get the field number for the column listed in the first field
                  int colIndex = findColumnIndex(filename, fields[0]);
                  if (colIndex < 0) {
                      System.out.println("Warning: File " + filename
                          + " or column " + fields[0]
                          + " not found in MRFILES.RRF. Skipping metadata row.");
                      continue;
                  }

                  try {
                      // Get the average
                      double colAverage = calculateAverageFieldLength(filename, colIndex);
                      System.out.println("filename:" + filename + " " + fields[0] + " " + colAverage);
                      // Create new line with all fields except last one
                      StringBuilder newLine = new StringBuilder();
                      for (int i = 0; i < fields.length; i++) {
                    	  // if field = AV, replace with newly calculated column average
                    	  if (i == 4) {
                    		  newLine.append(String.format("%.2f", colAverage)).append("|");
                    	  } else {
                    		  newLine.append(fields[i]).append("|");
                    	  }
                      }

                      // Write the modified line to output file
                      writer.write(newLine.toString());
                      writer.newLine();

                  } catch (NoSuchFileException e) {
                      System.out.println("Warning: File " + filename
                          + " not found. Skipping metadata row.");
                  }
              }

              writer.close();

          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
              throw e;
          }
      }

      public void updateMrsab() throws Exception {
    	  String inputFile = "MRSAB.RRF";
          String outputFile = "MRSAB.mod";
          String backupFile = "MRSAB.bak";

          try (BufferedReader reader = new BufferedReader(new FileReader(
              outputPath + File.separator + inputFile, StandardCharsets.UTF_8));
               BufferedWriter writer = new BufferedWriter(new FileWriter(
                   outputPath + File.separator + outputFile, StandardCharsets.UTF_8))) {

              String line;
              while ((line = reader.readLine()) != null) {
                  // Split the line by pipe character
                  String[] fields = line.split("\\|");

                  // Get the filename from the sixth field
                  String terminology = fields[3];


                  try {
                      // Get the average
                      int tfr = countTerminologyOccurrences(terminology);
                      int cfr = countUniqueTerminologyOccurrences(terminology);
                      System.out.println("terminology:" + terminology + " "  + tfr);
                      // Create new line with all fields except last one
                      StringBuilder newLine = new StringBuilder();
                      for (int i = 0; i < fields.length; i++) {
                    	  // if field = TFR, replace with newly calculated term count for the terminology
                    	  if (i == 14) {
                    		  newLine.append(tfr).append("|");
                    	  // if field = CFR, replace with newly calculated cui count for the terminology
                    	  } else if (i == 15) {
                        	  newLine.append(cfr).append("|");
                    	  } else {
                    		  newLine.append(fields[i]).append("|");
                    	  }
                      }

                      // Write the modified line to output file
                      writer.write(newLine.toString());
                      writer.newLine();

                  } catch (NoSuchFileException e) {
                     writer.write(line);
                      writer.newLine();
                  }
              }
              writer.close();

              // make a backup of the original MRSAB, and rename the modified one MRSAB.RRF
              File backup = new File(outputPath + File.separator + backupFile);
              File modified = new File(outputPath + File.separator + outputFile);
              File original = new File(outputPath + File.separator + inputFile);

              // Delete existing backup if it exists
              Files.deleteIfExists(backup.toPath());

              // Rename original to backup
              if (!original.renameTo(backup)) {
                  throw new IOException("Failed to rename " + inputFile + " to " + backup.getName());
              }

              // Rename modified to original
              if (!modified.renameTo(original)) {
                  // If second rename fails, try to restore original file
                  ConfigUtility.renameFile(backup, original);
                  throw new IOException("Failed to rename " + outputFile + " to " + inputFile);
              }


          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
          }
      }


      public int findColumnIndex(String filename, String colname)
        throws Exception {
          try (BufferedReader reader = newTemplateReader("MRFILES.RRF")) {
              String line;
              int lineNumber = 0;
              while ((line = reader.readLine()) != null) {
                  lineNumber++;
                  if (line.trim().isEmpty()) {
                      continue;
                  }
                  // Split the line by vertical bar
                  String[] fields = line.split("\\|");
                  if (fields.length < 3) {
                      throw new LocalException("Bad MRFILES.RRF template row "
                          + lineNumber + ": " + line);
                  }

                  // Check if this is the row we're looking for
                  if (fields[0].equals(filename)) {
                      // Get the comma-delimited column names from the third field
                      String columnNamesField = fields[2];
                      List<String> columnNames = Arrays.asList(columnNamesField.split(","));

                      // Find the index of the desired column name
                      int index = columnNames.indexOf(colname.trim());

                      // Return the index (will be -1 if not found)
                      return index;
                  }
              }

              // If we get here, we didn't find the filename
              return -1;
          }
      }

      /**
       * Returns a reader for a release metadata template.
       *
       * @param fileName the file name
       * @return the reader
       * @throws Exception the exception
       */
      private BufferedReader newTemplateReader(String fileName)
        throws Exception {
          final Path source = getInputTemplatePath(fileName);
          if (Files.isRegularFile(source)) {
              return Files.newBufferedReader(source, StandardCharsets.UTF_8);
          }
          final InputStream in =
              getClass().getResourceAsStream("/META/" + fileName);
          if (in != null) {
              return new BufferedReader(
                  new InputStreamReader(in, StandardCharsets.UTF_8));
          }
          throw new LocalException("Release metadata template not found: "
              + fileName + ". Checked input path " + source
              + " and classpath resource META/" + fileName + ".");
      }

      /**
       * Returns the input template path.
       *
       * @param fileName the file name
       * @return the input path
       */
      private Path getInputTemplatePath(String fileName) {
          return Paths.get(config.getProperty("source.data.dir"),
              getProcess().getInputPath(), "META", fileName);
      }
      public double calculateAverageFieldLength(String filename, int fieldNumber) throws IOException {
          if (fieldNumber < 0) {
              throw new IllegalArgumentException("Field number must be non-negative");
          }
          int totalLength =0;
          int count = 0;

          try (BufferedReader reader = Files.newBufferedReader(
              outputPath.toPath().resolve(filename), StandardCharsets.UTF_8)) {
        	            String line;
        	            int start, end, currentField;

        	            while ((line = reader.readLine()) != null) {
        	                if (line.isEmpty()) continue;

        	                // Manual field parsing without splitting
        	                start = 0;
        	                currentField = 0;

        	                // Find the target field
        	                while (currentField < fieldNumber && start < line.length()) {
        	                    if (line.charAt(start) == '|') {
        	                        currentField++;
        	                    }
        	                    start++;
        	                }

        	                if (currentField != fieldNumber) {
        	                    throw new IllegalArgumentException(
        	                        "Field number " + fieldNumber + " is out of bounds for line");
        	                }

        	                // Find end of target field
        	                end = start;
        	                while (end < line.length() && line.charAt(end) != '|') {
        	                    end++;
        	                }

        	                totalLength += (end - start);
        	                count++;
        	            }
        	        }

        	        return count > 0 ? (double) totalLength / count : 0.0;
      }

      public int countTerminologyOccurrences(String terminology) throws IOException {
          int count = 0;
          int fieldIndex = 11; // 12th field (0-based index)

          try (BufferedReader reader = new BufferedReader(new FileReader(
              outputPath + File.separator + "MRCONSO.RRF", StandardCharsets.UTF_8), 32768)) {
              String line;
              int currentField, pos;

              while ((line = reader.readLine()) != null) {
                  // Skip empty lines
                  if (line.isEmpty()) continue;

                  // Find the 12th field
                  currentField = 0;
                  pos = 0;

                  while (currentField < fieldIndex && pos < line.length()) {
                      if (line.charAt(pos) == '|') {
                          currentField++;
                      }
                      pos++;
                  }

                  // Check if we found the correct field
                  if (currentField == fieldIndex) {
                      // Find end of the field
                      int endPos = pos;
                      while (endPos < line.length() && line.charAt(endPos) != '|') {
                          endPos++;
                      }

                      // Compare the field value with the target terminology
                      if (endPos - pos == terminology.length()) {
                          boolean matches = true;
                          for (int i = 0; i < terminology.length(); i++) {
                              if (line.charAt(pos + i) != terminology.charAt(i)) {
                                  matches = false;
                                  break;
                              }
                          }
                          if (matches) {
                              count++;
                          }
                      }
                  }
              }
          }

          return count;
      }


      public int countUniqueTerminologyOccurrences(String terminology) throws IOException {
          Set<String> uniqueFirstFields = new HashSet<>();

          try (BufferedReader reader = new BufferedReader(new FileReader(
              outputPath + File.separator + "MRCONSO.RRF", StandardCharsets.UTF_8), 32768)) {
              String line;

              while ((line = reader.readLine()) != null) {
                  if (line.isEmpty()) continue;

                  // Get first field value
                  int firstFieldEnd = line.indexOf('|');
                  if (firstFieldEnd == -1) continue; // Skip malformed lines

                  // Find 12th field (11 pipe characters)
                  int pipeCount = 0;
                  int pos = firstFieldEnd + 1;
                  int fieldStart = -1;

                  while (pos < line.length() && pipeCount < 11) {
                      if (line.charAt(pos) == '|') {
                          pipeCount++;
                          if (pipeCount == 10) { // Found start of 12th field
                              fieldStart = pos + 1;
                              break;
                          }
                      }
                      pos++;
                  }

                  if (fieldStart == -1) continue; // Skip if we didn't find the 12th field

                  // Find end of 12th field
                  int fieldEnd = line.indexOf('|', fieldStart);
                  if (fieldEnd == -1) fieldEnd = line.length();

                  // Check if 12th field matches terminology
                  if (fieldEnd - fieldStart == terminology.length()) {
                      boolean matches = true;
                      for (int i = 0; i < terminology.length(); i++) {
                          if (line.charAt(fieldStart + i) != terminology.charAt(i)) {
                              matches = false;
                              break;
                          }
                      }

                      if (matches) {
                          // Add first field to set if terminology matches
                          uniqueFirstFields.add(line.substring(0, firstFieldEnd));
                      }
                  }
              }
          }

          return uniqueFirstFields.size();
      }

  public void replaceAllInFile(String folder, String file, String previousRelease, String currentRelease) throws Exception {
	  Path path = Paths.get(folder, file);
	    Charset charset = StandardCharsets.UTF_8;

	    String content = new String(Files.readAllBytes(path), charset);
	    content = content.replaceAll(previousRelease, currentRelease);
	    Files.write(path, content.getBytes(charset));
  }


  public static File getLastModified(File directoryFile)
  {
      File[] files = directoryFile.listFiles(File::isDirectory);
      long lastModifiedTime = Long.MIN_VALUE;
      File chosenFile = null;

      if (files != null)
      {
          for (File file : files)
          {
              if (file.lastModified() > lastModifiedTime)
              {
                  chosenFile = file;
                  lastModifiedTime = file.lastModified();
              }
          }
      }

      return chosenFile;
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());
    // n/a
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
    if (p.getProperty("email") != null) {
      email = p.getProperty("email");
    }
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();
    final AlgorithmParameter email = new AlgorithmParameterJpa(
        "Notification emails", "email", "Email addresses for notification",
        "e.g. a@b.com", 4000, AlgorithmParameter.Type.TEXT,
        PropertyUtility.getProperties().getProperty("mail.smtp.to"));
    params.add(email);
    return params;
  }

  /**
   * Appends source-version properties to release.dat content.
   *
   * @param data the release.dat content builder
   * @throws Exception the exception
   */
  private void appendIncludedReleaseProperties(StringBuilder data)
    throws Exception {
    final File mrsabFile = new File(outputPath, "MRSAB.RRF");
    appendRequiredReleaseProperty(data, "umls.release.ncit",
        formatNcitReleaseVersion(getMrsabVersion(mrsabFile, "NCIT", "NCI")),
        mrsabFile);
    appendRequiredReleaseProperty(data, "umls.release.umls",
        getMrsabVersion(mrsabFile, "MTH"), mrsabFile);
  }

  /**
   * Appends a required release property derived from MRSAB.
   *
   * @param data the release.dat content builder
   * @param property the property name
   * @param version the property value
   * @param mrsabFile the MRSAB file
   * @throws Exception the exception
   */
  private void appendRequiredReleaseProperty(StringBuilder data,
    String property, String version, File mrsabFile) throws Exception {
    if (ConfigUtility.isEmpty(version)) {
      throw new LocalException("Unable to determine " + property + " from "
          + mrsabFile.getAbsolutePath());
    }
    data.append(property).append("=").append(version).append("\n");
    logInfo("    " + property + "=" + version);
  }

  /**
   * Returns the best version for any of the supplied MRSAB RSAB values.
   *
   * @param mrsabFile the MRSAB file
   * @param rsabs the acceptable source abbreviations
   * @return the version, or blank if none was found
   * @throws IOException the IO exception
   */
  private String getMrsabVersion(File mrsabFile, String... rsabs)
    throws IOException {
    String fallbackVersion = "";
    try (BufferedReader reader = new BufferedReader(
        new FileReader(mrsabFile, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        if (fields.length <= MRSAB_SABIN) {
          continue;
        }
        if (!isMrsabSource(fields[MRSAB_RSAB].trim(), rsabs)) {
          continue;
        }
        final String version = getMrsabVersion(fields);
        if (ConfigUtility.isEmpty(version)) {
          continue;
        }
        if ("Y".equals(fields[MRSAB_CURVER].trim())
            && "Y".equals(fields[MRSAB_SABIN].trim())) {
          return version;
        }
        if (ConfigUtility.isEmpty(fallbackVersion)) {
          fallbackVersion = version;
        }
      }
    }
    return fallbackVersion;
  }

  /**
   * Formats an NCIT version for release.dat.
   *
   * @param version the MRSAB source version
   * @return the release.dat formatted NCIT version
   */
  private String formatNcitReleaseVersion(String version) {
    if (ConfigUtility.isEmpty(version)) {
      return version;
    }
    final String trimmedVersion = version.trim();
    if (trimmedVersion.matches("\\d{4}_\\d{2}[A-Za-z]")) {
      return trimmedVersion.substring(2, 4) + "."
          + trimmedVersion.substring(5).toLowerCase();
    }
    return trimmedVersion;
  }

  /**
   * Indicates whether the MRSAB source abbreviation matches one of the supplied
   * values.
   *
   * @param rsab the MRSAB RSAB
   * @param rsabs the acceptable source abbreviations
   * @return true if the source matches
   */
  private boolean isMrsabSource(String rsab, String... rsabs) {
    for (final String candidate : rsabs) {
      if (candidate.equals(rsab)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the source version from an MRSAB row.
   *
   * @param fields the MRSAB fields
   * @return the source version
   */
  private String getMrsabVersion(String[] fields) {
    final String sver = fields[MRSAB_SVER].trim();
    if (!ConfigUtility.isEmpty(sver) && !"latest".equalsIgnoreCase(sver)) {
      return sver;
    }
    final String vsab = fields[MRSAB_VSAB].trim();
    final int index = vsab.indexOf("_");
    return index == -1 ? "" : vsab.substring(index + 1);
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }

  /**
   * Returns the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email the email
   */
  public void setEmail(String email) {
    this.email = email;
  }

}
