/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.invert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.persistence.Query;

import com.wci.umls.server.AlgorithmParameter;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;

/**
 * Implementation of an algorithm to save information before an insertion.
 */
public class ValidateAttributesAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /**  The src full path. */
  private String srcFullPath;

  /** The check names. */
  private List<String> checkNames;
  
  /**  The max test cases. */
  private int maxTestCases = 50;
  
  /** Monitor the number of errors already logged for each of the test cases */
  private Integer[] errorTallies = new Integer[maxTestCases];
  
  // 0 source_attribute_id
  // 1 sg_id
  // 2 attribute_level
  // 3 attribute_name
  // 4 attribute_value
  // 5 source
  // 6 status
  // 7 tobereleased
  // 8 released
  // 9 suppressible
  // 10 sg_type_1
  // 11 sg_qualifier_1
  // 12 source_atui
  // 13 hashcode
  
  /**
   * Instantiates an empty {@link ValidateAttributesAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public ValidateAttributesAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("VALIDATEATTRIBUTES");
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
      throw new LocalException(
          "Attribute Validation requires a project to be set");
    }

    // Go through all the files needed by insertion and check for presence
    // Check the input directories
    srcFullPath =
        ConfigUtility.getConfigProperties().getProperty("source.data.dir") + "/"
            + getProcess().getInputPath();

    final Path realPath = Paths.get(srcFullPath).toRealPath();
    setSrcDirFile(new File(realPath.toString()));

    if (!getSrcDirFile().exists()) {
      throw new LocalException(
          "Specified input directory does not exist - " + srcFullPath);
    }

    checkFileExist(srcFullPath, "attributes.src");
    checkFileExist(srcFullPath, "classes_atoms.src");

    // Ensure permissions are sufficient to write files
    try {
      final File outputFile = new File(srcFullPath, "testFile.txt");

      final PrintWriter out = new PrintWriter(new FileWriter(outputFile));
      out.print("Test");
      out.close();

      // Remove test file
      outputFile.delete();
    } catch (Exception e) {
      throw new LocalException("Unable to write files to " + srcFullPath
          + " - update permissions before continuing validation.");
    }

    // Makes sure editing is turned off before continuing
    /*
     * if(getProject().isEditingEnabled()){ throw new LocalException(
     * "Editing is turned on - disable before continuing insertion."); }
     */

    // Makes sure automations are turned off before continuing
    if (getProject().isAutomationsEnabled()) {
      throw new LocalException(
          "Automations are turned on - disable before continuing validation.");
    }

    return validationResult;
  }

  /**
   * Check file exist.
   *
   * @param srcFullPath the src full path
   * @param fileName the file name
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  private void checkFileExist(String srcFullPath, String fileName)
    throws Exception {

    File sourceFile = new File(srcFullPath + File.separator + fileName);
    if (!sourceFile.exists()) {
      throw new Exception(fileName
          + " file doesn't exist at specified input directory: " + srcFullPath);
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

    ValidationResult result = new ValidationResultJpa();

    // read in file classes_atoms.src
    BufferedReader in = new BufferedReader(new FileReader(new File(srcFullPath + File.separator + "classes_atoms.src")));
    String fileLine = "";
    Set<String> sauis = new HashSet<>();
    Set<String> scuis = new HashSet<>();
    Set<String> sduis = new HashSet<>();
    
    // cache sgId, sgType, sgQualifier
    while ((fileLine = in.readLine()) != null) {
      String[] fields = FieldedStringTokenizer.split(fileLine, "|");
      sauis.add(fields[3]);
      scuis.add(fields[10]);
      sduis.add(fields[11]);
    } 
    in.close();
    
    // read in file attributes.src
    in = new BufferedReader(new FileReader(
        new File(srcFullPath + File.separator + "attributes.src")));
    fileLine = "";

    Set<String> uniqueAtuiFields = new HashSet<>();
    
    final int ct =
        filterFileForCount(getSrcDirFile(), "attributes.src", null, null);
    logInfo("  Steps: " + ct + " attribute rows to process");

    // Set the number of steps to the number of lines to be processed
    setSteps(ct);

    // do field and line checks
    // initialize caches
    while ((fileLine = in.readLine()) != null) {

      String[] fields = FieldedStringTokenizer.split(fileLine, "|");

      // check each row has the correct number of fields
      if (checkNames.contains("#ATTRS_1")) {
        if (fields.length != 14) {
          if (underErrorTallyThreashold("#ATTRS_1")) {
            result.addError(
              "ATTRS_1: incorrect number of fields in attributes.src row: "
                  + fileLine);
          }
        }
      }

      // check for XML chars in string field
      if (checkNames.contains("#ATTRS_2")) {
        String str = fields[4];
        String pattern = ".*[&#][a-zA-Z0-9]+;.*";
        if (Pattern.matches(pattern, str)) {
          if (underErrorTallyThreashold("#ATTRS_2")) {
            result.addWarning(
              "ATTRS_2: String contains an XML character: " + fields[4]);
          }
        }
      }

      // check S level attributes cannot have 'N' status
      if (checkNames.contains("#ATTRS_3")) {
        String status = fields[6];
        if (status.equals("N") && !fields[3].equals("SEMANTIC_TYPE")) {
          if (underErrorTallyThreashold("#ATTRS_3")) {
            result.addError("ATTRS_3: S level attributes cannot have 'N' status: "
              + fields[3] + ":" + fields[6]);
          }
        }
      }

      // check for non-unique AUI fields
      if (checkNames.contains("#ATTRS_4")) {
        
        // source|attribute_name|source_atui|sg_id|sg_type_1|sg_qualifier_1|hashcode
        if (uniqueAtuiFields
            .contains(fields[5] + "|" + fields[3] + "|" + fields[12] + "|"
                + fields[1] + "|" + fields[10] + "|" + fields[11] + "|" + fields[13])) {
          if (underErrorTallyThreashold("#ATTRS_4")) {
            result.addError("ATTRS_4: Duplicate ATUI fields: " + fields[5] + "|"
              + fields[3] + "|" + fields[12] + "|" + fields[1] + "|"
              + fields[10] + "|" + fields[11] + "|" + fields[13]);
          }
        } else {
          uniqueAtuiFields.add(fields[5] + "|" + fields[3] + "|" + fields[12]
              + "|" + fields[1] + "|" + fields[10] + "|" + fields[11] + "|" + fields[13]);
        }
      }
      
      // check source should be like E-* for non SRC stys.
      if (checkNames.contains("#ATTRS_5")) {
        if (!fields[1].equals("SRC") && !fields[5].equals("SRC") && fields[3].equals("SEMANTIC_TYPE")) {
          if (!fields[5].startsWith("E-")) {
            if (underErrorTallyThreashold("#ATTRS_5")) {
              result.addError(
                "ATTRS_5: Source should be like E-* for non SRC stys: "
                    + fields[5]);
            }
          }
        }
      }

      // check SATUI should be null
      if (checkNames.contains("#ATTRS_6")) {
        if (!fields[12].isEmpty()) {
          if (underErrorTallyThreashold("#ATTRS_6")) {
            result
              .addWarning("ATTRS_6: Source ATUI should be null: " + fields[12]);
          }
        }
      }

      // check for leading, trailing and duplicate white space in attributes
      if (checkNames.contains("#ATTRS_7")) {
        String str = fields[4];
        String checkedString = str.replaceAll("\\s+", " ");
        if (!str.trim().equals(str) || !checkedString.equals(str)) {
          if (underErrorTallyThreashold("#ATTRS_7")) {
            result.addError(
              "ATTRS_7: String has leading, trailing or duplicate white space: "
                  + fields[4]);
          }
        }
      }
      
      // check if STY attribute value is in semantic_type table
      if (checkNames.contains("#ATTRS_8")) {
        String atv = fields[4];
        
        final Query query = manager
            .createQuery("select a.expandedForm from SemanticTypeJpa a "
                + "where a.terminology = :terminology "
                + "  and a.version = :version");
        query.setParameter("terminology", getProcess().getTerminology());
        query.setParameter("version", getProcess().getVersion());
        @SuppressWarnings("unchecked")
        List<Object> list = query.getResultList();
        Set<String> validStys = new HashSet<>();
        for (Object entry : list) {
          validStys.add(entry.toString());
        }
        
        if (fields[3].equals("SEMANTIC_TYPE") &&  !validStys.contains(atv)) {

          if (underErrorTallyThreashold("#ATTRS_8")) {
            result.addError(
              "ATTRS_8: STY attribute value is not in semantic_type table: "
                  + atv);
          }
        }
      }   
      
      // check if STY attribute value is in semantic_type table
      if (checkNames.contains("#ATTRS_9")) {
        String sgType = fields[10];
        if (underErrorTallyThreashold("#ATTRS_9")) {
          if (sgType.equals("SOURCE_CUI")) {
            if (!scuis.contains(fields[1])) {
              result.addError(
                  "ATTRS_9: SgType indicates SOURCE_CUI, but sg_id is not in that classes_atoms field: "
                      + fields[1]);
            }
          } else if (sgType.equals("SOURCE_AUI")) {
            if (!sauis.contains(fields[1])) {
              result.addError(
                  "ATTRS_9: SgType indicates SOURCE_AUI, but sg_id is not in that classes_atoms field: "
                      + fields[1]);
            }
          } else if (sgType.equals("SOURCE_DUI")) {
            if (!sduis.contains(fields[1])) {
              result.addError(
                  "ATTRS_9: SgType indicates SOURCE_DUI, but sg_id is not in that classes_atoms field: "
                      + fields[1]);
            }
          }
        }
      }
      // Update the progress
      updateProgress();
    }

    in.close();


    // print warnings and errors to log
    if (result.getWarnings().size() > 0) {
      for (String warning : result.getWarnings()) {
        logInfo(warning);
      }
    }
    if (result.getErrors().size() > 0) {
      for (String error : result.getErrors()) {
        logError(error);
      }
      throw new Exception(this.getName() + " Failed");
    }

    logInfo("Finished " + getName());
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
    // n/a
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {
    if (p.getProperty("checkNames") != null) {
      checkNames =
          Arrays.asList(String.valueOf(p.getProperty("checkNames")).split(";"));
    }
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    // Integrity check names
    AlgorithmParameter param = new AlgorithmParameterJpa("Validation Checks",
        "checkNames", "The names of the validation checks to run",
        "e.g. #ATTRS_1", 200, AlgorithmParameter.Type.MULTI, "");

    List<String> validationChecks = new ArrayList<>();
    validationChecks.add("#ATTRS_1");
    validationChecks.add("#ATTRS_2");
    validationChecks.add("#ATTRS_3");
    validationChecks.add("#ATTRS_4");
    validationChecks.add("#ATTRS_5");
    validationChecks.add("#ATTRS_6");
    validationChecks.add("#ATTRS_7");
    validationChecks.add("#ATTRS_8");
    validationChecks.add("#ATTRS_9");
    validationChecks.add("#ATTRS_10");

    Collections.sort(validationChecks);
    param.setPossibleValues(validationChecks);
    params.add(param);

    return params;
  }

  
  // check if the number of errors logged for each test case is greater or less than 10
  private boolean underErrorTallyThreashold(String testName) {
    int index = Integer.parseInt(testName.substring(testName.indexOf("_") + 1));
    Integer value = errorTallies[index];
    if (value == null) {
      value = 1;
    } else {
      value = value + 1;
    }
    errorTallies[index] = value;
    return value <= 10;
  }
  
  @Override
  public String getDescription() {
    return "Validation checks related to attributes in the inversion files.";
  }
}