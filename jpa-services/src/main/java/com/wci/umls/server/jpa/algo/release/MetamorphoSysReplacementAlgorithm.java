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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;

/**
 * Algorithm to replace MetamorphoSys functionality.  Will create MRCOLS, MRFILES, MRDOC, MRSAB, release.dat
 */
public class MetamorphoSysReplacementAlgorithm extends AbstractAlgorithm {

  /** The email. */
  private String email;
  
  private String dataDir = ConfigUtility.getConfigProperties().getProperty("source.data.dir") + File.separator + "ncim" + File.separator + "config" + File.separator + "META";
  
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
        ConfigUtility.getConfigProperties().getProperty("source.data.dir")
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
    logInfo("  dataDir: " + dataDir);
    this.getProject();
    ConfigUtility.getLocalConfigFolder();
    
    updateMrsab();
    updateMrcols();
    updateMrfiles();

    logInfo("Finished " + getName());
  }
  
  
  
  

      public void updateMrfiles() throws Exception {
    	  String inputFile = "MRFILES.RRF";   
          String outputFile = "MRFILES.mod";
          String backupFile = "MRFILES.bak";
          
          String mrfilesRow = "";

          try (BufferedReader reader = new BufferedReader(new FileReader(dataDir + File.separator + inputFile));
               BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + File.separator + outputFile))) {

              String line;
              while ((line = reader.readLine()) != null) {
                  // Split the line by pipe character
                  String[] fields = line.split("\\|");
                  
                  // Get the filename from the first field
                  String filename = fields[0];
                  
                  if (filename.equals("MRFILES.RRF")) {
                	  mrfilesRow = line;
                	  continue;
                  }
                  
                  try {
                      // Get file size in bytes
                      long fileSize = Files.size(Paths.get(outputPath + File.separator + filename));
                      
                      // Get the line count
                      long lineCount = Files.lines(Paths.get(outputPath + File.separator + filename), StandardCharsets.UTF_8).count();
                    	
                      
                      // Create new line with all fields except last one
                      StringBuilder newLine = new StringBuilder();
                      for (int i = 0; i < fields.length - 2; i++) {
                          newLine.append(fields[i]).append("|");
                      }
                      // Append the lineCount and a pipe
                      newLine.append(lineCount).append("|");
                      
                      // Append the new file size and final pipe
                      newLine.append(fileSize).append("|");
                      
                      // Write the modified line to output file
                      writer.write(newLine.toString());
                      writer.newLine();
                      
                      
                  } catch (NoSuchFileException e) {
                      System.out.println("Warning: File " + filename + " not found. Keeping original line.");
                      writer.write(line);
                      writer.newLine();
                  }
              }
              writer.write(mrfilesRow);
              writer.newLine();
              writer.close();
              
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
                  backup.renameTo(original);
                  throw new IOException("Failed to rename " + outputFile + " to " + inputFile);
              }
              
              // Delete original, bc modified is the new MRFILES
              //Files.deleteIfExists(original.toPath());
              
          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
          }
      }
      

          public void adjustMrfilesByteCount(String filename) throws IOException {
              // Get current file byte count
              File file = new File(filename);
              long actualByteCount = file.length();
              
              // Read all lines and get last line's byte count field
              List<String> lines = new ArrayList<>();
              String lastLine;
              long claimedByteCount;
              
              try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                  String line;
                  while ((line = reader.readLine()) != null) {
                      lines.add(line);
                  }
                  
                  if (lines.isEmpty()) {
                      throw new IOException("File is empty");
                  }
                  
                  lastLine = lines.get(lines.size() - 1);
                  String[] fields = lastLine.split("\\|");
                  claimedByteCount = Long.parseLong(fields[fields.length - 1]);
              }
              
              // If byte counts match, we're done
              if (actualByteCount == claimedByteCount) {
                  return;
              }
              
              // Otherwise, update the last line and rewrite the file
              String[] lastLineFields = lastLine.split("\\|");
              StringBuilder newLastLine = new StringBuilder();
              
              // Rebuild the last line with all fields except the last
              for (int i = 0; i < lastLineFields.length - 1; i++) {
                  newLastLine.append(lastLineFields[i]).append("|");
              }
              
              // Add the actual byte count
              newLastLine.append(actualByteCount).append("|");
              
              // Write all lines back to file
              try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                  // Write all lines except the last
                  for (int i = 0; i < lines.size() - 1; i++) {
                      writer.write(lines.get(i));
                      writer.newLine();
                  }
                  
                  // Write the modified last line
                  writer.write(newLastLine.toString());
                  writer.newLine();
              }
              
              // Recursive call to check if we're done
              adjustMrfilesByteCount(filename);
          }
      
      
      public void updateMrcols() throws Exception {
    	  String dataDir = ConfigUtility.getConfigProperties().getProperty("source.data.dir") + File.separator + "ncim" + File.separator + "config" + File.separator + "META";
          String mrcolsFile = "MRCOLS.RRF";
          
          // delete MRCOLS.RRF if it exists
          File file = new File(outputPath + File.separator + mrcolsFile);
          if (file.exists()) {
              file.delete();
          }

          // parse template MRCOLS.RRF and create new one with updated column averages
          try (BufferedReader reader = new BufferedReader(new FileReader(dataDir + File.separator + mrcolsFile));
               BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + File.separator + mrcolsFile))) {

              String line;
              while ((line = reader.readLine()) != null) {
                  // Split the line by pipe character
                  String[] fields = line.split("\\|");
                  
                  // Get the filename from the sixth field
                  String filename = fields[6];
                  
                  // Get the field number for the column listed in the first field
                  int colIndex = findColumnIndex(filename, fields[0]);          
                  
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
                      System.out.println("Warning: File " + filename + " not found. Keeping original line.");
                      writer.write(line);
                      writer.newLine();
                  }
              }
              
              writer.close();
              
          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
          }
      }
      
      public void updateMrsab() throws Exception {
    	  String inputFile = "MRSAB.RRF";       
          String outputFile = "MRSAB.mod";
          String backupFile = "MRSAB.bak";

          try (BufferedReader reader = new BufferedReader(new FileReader(outputPath + File.separator + inputFile));
               BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + File.separator + outputFile))) {

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
                  backup.renameTo(original);
                  throw new IOException("Failed to rename " + outputFile + " to " + inputFile);
              }
              
              
          } catch (IOException e) {
              System.err.println("Error processing files: " + e.getMessage());
              e.printStackTrace();
          }
      }
      
      
      public int findColumnIndex(String filename, String colname) throws IOException {
          try (BufferedReader reader = new BufferedReader(new FileReader(dataDir + File.separator + "MRFILES.RRF"))) {
              String line;
              while ((line = reader.readLine()) != null) {
                  // Split the line by vertical bar
                  String[] fields = line.split("\\|");
                  
                  // Check if this is the row we're looking for
                  if (fields.length >= 3 && fields[0].equals(filename)) {
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
      public double calculateAverageFieldLength(String filename, int fieldNumber) throws IOException {
          if (fieldNumber < 0) {
              throw new IllegalArgumentException("Field number must be non-negative");
          }
          int totalLength =0;
          int count = 0;

        	        try (BufferedReader reader = new BufferedReader(new FileReader(outputPath + File.separator + filename), 32768)) { // Increased buffer size
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
          
          try (BufferedReader reader = new BufferedReader(new FileReader(outputPath + File.separator + "MRCONSO.RRF"), 32768)) {
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
          
          try (BufferedReader reader = new BufferedReader(new FileReader(outputPath + File.separator + "MRCONSO.RRF"), 32768)) {
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
        ConfigUtility.getConfigProperties().getProperty("mail.smtp.to"));
    params.add(email);
    return params;
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
