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
public class ValidateRelationshipsAlgorithm extends AbstractInsertMaintReleaseAlgorithm {

  private String srcFullPath;
  
  /** The check names. */
  private List<String> checkNames;
   
  /**  The test cases. */
  private List<TestCase> testCases;
  
  /**  The validation checks. */
  private List<String> validationChecks;
 
  /**
   * Instantiates an empty {@link ValidateRelationshipsAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public ValidateRelationshipsAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("VALIDATERELATIONSHIPS");
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
      throw new LocalException("Relationship Validation requires a project to be set");
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

    checkFileExist(srcFullPath, "relationships.src");
    checkFileExist(srcFullPath, "sources.src");

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
    /*if(getProject().isEditingEnabled()){
      throw new LocalException("Editing is turned on - disable before continuing insertion.");
    }*/
    
    // Makes sure automations are turned off before continuing
    if(getProject().isAutomationsEnabled()){
      throw new LocalException("Automations are turned on - disable before continuing validation.");
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
  
  // check if the number of errors logged for each test case is greater or less than 10
  private boolean underErrorTallyThreashold(String testName) {
    int index = Integer.parseInt(testName.substring(testName.indexOf("_") + 1));
    TestCase l_case = testCases.get(index -1 );
    int value = l_case.getErrorCt();
    if (value == 0) {
      value = 1;
    } else {
      value = value + 1;
    }
    l_case.setErrorCt(value);
    return value <= 10;
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
    
    // Fields:
    // 0 src_relationship_id
    // 1 level
    // 2 id_1
    // 3 relationship_name
    // 4 relationship_attribute
    // 5 id_2
    // 6 source
    // 7 source_of_label
    // 8 status
    // 9 tobereleased
    // 10 released
    // 11 suppressible
    // 12 id_type_1
    // 13 id_qualifier_1
    // 14 id_type_2
    // 15 id_qualifier_2
    // 16 source_rui
    // 17 relationship_group

    // e.g.
    // 40|S|C17260|RT|Gene_Plays_Role_In_Process|C29949|NCI_2016_05E|
    // NCI_2016_05E|R|Y|N|N|SOURCE_CUI|NCI_2016_05E|SOURCE_CUI|NCI_2016_05E|||


    // No Molecular actions will be generated by this algorithm
    setMolecularActionFlag(false);
    
    ValidationResult result = new ValidationResultJpa();
    
    Set<String> uniqueRuiFields = new HashSet<>();
    Set<String> uniqueSruis = new HashSet<>();
    
    // read in file MRDOC.RRF
    BufferedReader in = new BufferedReader(new FileReader(new File(srcFullPath + File.separator + "MRDOC.RRF")));

    String fileLine = "";
    Set<String> relas = new HashSet<>();
    
    // cache relas
    while ((fileLine = in.readLine()) != null) {
      String[] fields = FieldedStringTokenizer.split(fileLine, "|");
      if (fields[0].equals("RELA")) {
        relas.add(fields[1]);
      }
    } 
    in.close();
    
    // read in file sources.src
    in = new BufferedReader(new FileReader(new File(srcFullPath + File.separator + "sources.src")));
    Map<String, String> sourcesToLatMap = new HashMap<>();
    Set<String> rootSources = new HashSet<>();
    
    // cache sources
    while ((fileLine = in.readLine()) != null) {
      String[] fields = FieldedStringTokenizer.split(fileLine, "|");
      sourcesToLatMap.put(fields[0], fields[15]);
      rootSources.add(fields[4]);
    } 
    in.close();
    
    final int ct =
        filterFileForCount(getSrcDirFile(), "relationships.src", null, null);
    logInfo("  Steps: " + ct + " relationship rows to process");
    
    // Set the number of steps to the number of lines to be processed
    setSteps(ct);

    
    // read in file contexts.src
    in = new BufferedReader(new FileReader(
        new File(srcFullPath + File.separator + "relationships.src")));


    // do field and line checks
    // initialize caches
    while ((fileLine = in.readLine()) != null) {

      String[] fields = FieldedStringTokenizer.split(fileLine, "|");

      // check each row has the correct number of fields
      if (checkNames.contains("#RELS_1")) {
        if (fields.length != 18) {
          if (underErrorTallyThreashold("#RELS_1")) {
            result.addError("RELS_1:" + fileLine);
          }
        }
      }

      // check VSAB equals the source of label
      if (checkNames.contains("#RELS_2")) {
        if (!fields[6].equals(fields[7])) {
          if (underErrorTallyThreashold("#RELS_2")) {
            result.addError("RELS_2:" + fields[6] + " : " + fields[7]);
          }
        }
      }

      // check self-referential relationships
      if (checkNames.contains("#RELS_3")) {
        if (fields[2].equals(fields[5]) && fields[12].equals(fields[14])
            && fields[13].equals(fields[15])) {
          if (underErrorTallyThreashold("#RELS_3")) {
            result.addError("RELS_3:" + fileLine);
          }
        }
      }

      // check conflicting rel/rela
      if (checkNames.contains("#RELS_4")) {
        // String pat3 =
        // "^(associated_with|consists_of|constitutes|contains|contained_in|ingredient_of|has_ingredient)$";
        // String pat3b =
        // "^(conceptual_part_of|form_of|isa|part_of|tradname_of)$";
        // String pat3c =
        // "^(has_conceptual_part|has_form|inverse_isa|has_part|has_tradname)$";

        if (!fields[3].equals("RT") && (fields[4].equals("associated_with")
            || fields[4].equals("consists_of")
            || fields[4].equals("constitutes") || fields[4].equals("contains")
            || fields[4].equals("contained_in")
            || fields[4].equals("ingredient_of")
            || fields[4].equals("has_ingredient"))) {
          if (underErrorTallyThreashold("#RELS_4")) {
            result.addError("RELS_4:" + fields[3] + ":" + fields[4]);
          }
        }
        if (!fields[3].equals("NT") && (fields[4].equals("conceptual_part_of")
            || fields[4].equals("form_of") || fields[4].equals("isa")
            || fields[4].equals("part_of")
            || fields[4].equals("tradename_of"))) {
          if (underErrorTallyThreashold("#RELS_4")) {
            result.addError("RELS_4:" + fields[3] + ":" + fields[4]);
          }
        }
        if (!fields[3].equals("BT") && (fields[4].equals("has_conceptual_part")
            || fields[4].equals("has_form") || fields[4].equals("inverse_isa")
            || fields[4].equals("has_part")
            || fields[4].equals("has_tradename"))) {
          if (underErrorTallyThreashold("#RELS_4")) {
            result.addError("RELS_4:" + fields[3] + ":" + fields[4]);
          }
        }

      }

      // check rui fields are unique
      if (checkNames.contains("#RELS_5")) {

        String id_1 = fields[2];
        String relationship_name = fields[3];
        String relationship_attribute = fields[4];
        String id_2 = fields[5];
        String source = fields[6];
        String source_of_label = fields[7];
        String id_qualifier_1 = fields[13];
        String id_type_2 = fields[14];
        String id_qualifier_2 = fields[15];
        String source_rui = fields[16];
        String relationship_group = fields[17];

        if (uniqueRuiFields.contains(id_1 + "|" + relationship_name + "|"
            + relationship_attribute + "|" + id_2 + "|" + source + "|"
            + source_of_label + "|" + id_qualifier_1 + "|" + id_type_2 + "|"
            + id_qualifier_2 + "|" + source_rui + "|" + relationship_group)) {
          if (underErrorTallyThreashold("#RELS_5")) {
            result.addError("RELS_5:" + id_1 + "|" + relationship_name + "|"
                + relationship_attribute + "|" + id_2 + "|" + source + "|"
                + source_of_label + "|" + id_qualifier_1 + "|" + id_type_2 + "|"
                + id_qualifier_2 + "|" + source_rui + "|" + relationship_group);
          }
        } else {
          uniqueRuiFields.add(id_1 + "|" + relationship_name + "|"
              + relationship_attribute + "|" + id_2 + "|" + source + "|"
              + source_of_label + "|" + id_qualifier_1 + "|" + id_type_2 + "|"
              + id_qualifier_2 + "|" + source_rui + "|" + relationship_group);
        }
      }

      // check SFO/LFO not connected to any atom
      /*
       * if ($IL[4] eq 'SFO/LFO' && ($IL[13] !~ /$pat5/ || $IL[15] !~ /$pat5/)
       * my $pat5 = qr{^(ROOT_SOURCE_AUI|SOURCE_AUI|SRC_ATOM_ID)$};
       */
      if (checkNames.contains("#RELS_6")) {

        if (fields[3].equals("SFO/LFO")
            && ((!fields[12].equals("ROOT_SOURCE_AUI")
                && !fields[12].equals("SOURCE_AUI")
                && !fields[12].equals("SRC_ATOM_ID"))
                || (!fields[14].equals("ROOT_SOURCE_AUI")
                    && !fields[14].equals("SOURCE_AUI") && !fields[14]
                        .equals("SRC_ATOM_ID")))) {
          if (underErrorTallyThreashold("#RELS_6")) {

            result.addError(
                "RELS_6:" + fields[3] + ":" + fields[12] + ":" + fields[14]);
          }
        }
      }

      // check inv sgs for translation_of and version rel
      /*
       * if ($IL[5] =~ /$pat8/ && $IL[7] eq 'SRC' && ($IL[13] ne 'CODE_SOURCE'
       * || $IL[15] ne 'CODE_SOURCE' || $IL[14] ne 'SRC' || $IL[16] ne 'SRC') my
       * $pat8 = qr{^(translation_of|version_of)$};
       */
      if (checkNames.contains("#RELS_7")) {

        if ((fields[4].equals("translation_of")
            || fields[4].equals("version_of"))
            && fields[6].equals("SRC")
            && (!fields[12].equals("CODE_SOURCE")
                || !fields[14].equals("CODE_SOURCE")
                || !fields[13].equals("SRC") || !fields[15].equals("SRC"))) {
          if (underErrorTallyThreashold("#RELS_7")) {
            result.addError("RELS_7:" + fields[4] + ":" + fields[12] + ":"
                + fields[13] + ":" + fields[14]);
          }
        }
      }

      // check for non unique sruis
      /*
       * if ("$IL[17]" !~ /^~DA/) { if (defined($unqSRuis{"$IL[17]"})) {
       */
      if (checkNames.contains("#RELS_8")) {
        if (!fields[16].startsWith("~DA") && !fields[16].equals("")) {
          if (uniqueSruis.contains(fields[16])) {
            if (underErrorTallyThreashold("#RELS_8")) {
              result.addError("RELS_8:" + fields[16]);
            }
          } else {
            uniqueSruis.add(fields[16]);
          }
        }
      }

      // check if RELA is in MRDOC.RRF file
      if (checkNames.contains("#RELS_9")) {
        if (!fields[4].equals("") && !relas.contains(fields[4])) {
          if (underErrorTallyThreashold("#RELS_9")) {
            result.addError("RELS_9:" + fields[4]);
          }
        }
      }

      // check if VSAB is not in sources.src file
      if (checkNames.contains("#RELS_10")) {
        if (!fields[6].equals("SRC")
            && !sourcesToLatMap.containsKey(fields[6])) {
          if (underErrorTallyThreashold("#RELS_10")) {
            result.addError("RELS_10:" + fields[6]);
          }
        }
      }
      
      // Update the progress
      updateProgress(); 
    }
    in.close();
    
    logInfo("");
    logInfo("QA REPORT");
    logInfo("");
    for (int index = 0; index < testCases.size(); index++) {
      TestCase tc = testCases.get(index);
      if (tc.getErrorCt() == 0) {
        logInfo("  PASSED: " + tc.getShortName() + " " + tc.getName());
      }
    }
    
    String prevTestCase = "";
    // print warnings and errors to log
    if (result.getWarnings().size() > 0) {
      logInfo("");
      logInfo("WARNINGS");
      List<String> sortedWarnings = new ArrayList<>(result.getWarnings());
      Collections.sort(sortedWarnings);
      for (String warning : sortedWarnings) {
        String currentTestCase = warning.substring(0, warning.indexOf(":"));
        if (!currentTestCase.equals(prevTestCase)) {
          int index = Integer.parseInt(currentTestCase.substring(currentTestCase.indexOf("_") + 1));
          logInfo("");
          logInfo(currentTestCase + " warning count: " + testCases.get(index - 1).getErrorCt() + " : " + testCases.get(index - 1).getFailureMsg());
        }
        prevTestCase = currentTestCase;
        logWarn(warning, "", "  ");
      }
    }
    if (result.getErrors().size() > 0) {
      logInfo("");
      logInfo("ERRORS");
      List<String> sortedErrors = new ArrayList<>(result.getErrors());
      Collections.sort(sortedErrors);
      for (String error : sortedErrors) {
        String currentTestCase = error.substring(0, error.indexOf(':'));
        if (!currentTestCase.equals(prevTestCase)) {
          int index = Integer.parseInt(currentTestCase.substring(currentTestCase.indexOf("_") + 1));
          logInfo("");
          logInfo(currentTestCase + " error count: " + testCases.get(index - 1).getErrorCt() + " : " + testCases.get(index - 1).getFailureMsg());
        }
        prevTestCase = currentTestCase;
        logError(error, "  ");
      }
      logInfo("");
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
    testCases = new ArrayList<>();
    testCases.add(new TestCase("RELS_1",
        "check each row has the correct number of fields",
        "incorrect number of fields in relationships.src row"));
    testCases.add(new TestCase("RELS_2",
        "check VSAB equals the source of label",
        "VSAB not equal to the source of label"));
    testCases.add(new TestCase("RELS_3",
        "check self-referential relationships",
        "self-referential relationship"));
    testCases.add(new TestCase("RELS_4",
        "check conflicting rel/rela",
        "conflicting rel/rela"));
    testCases.add(new TestCase("RELS_5",
        "check rui fields are unique",
        "Duplicate RUI fields"));
    testCases.add(new TestCase("RELS_6",
        "check SFO/LFO not connected to any atom",
        "SFO/LFO not connected to any atom"));
    testCases.add(new TestCase("RELS_7",
        "check inv sgs for translation_of and version rel",
        "inv sgs for translation_of and version rel"));
    testCases.add(new TestCase("RELS_8",
        "check for non unique sruis",
        "non-unique sruis"));
    testCases.add(new TestCase("RELS_9",
        "check if RELA is in MRDOC.RRF file",
        "RELA is not in the MRDOC.RRF file"));
    testCases.add(new TestCase("RELS_10",
        "check if VSAB is not in sources.src file",
        "VSAB is not in the sources.src file"));
 
  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters() throws Exception {
    final List<AlgorithmParameter> params = super.getParameters();

    // Integrity check names
    AlgorithmParameter param = new AlgorithmParameterJpa("Validation Checks", "checkNames",
        "The names of the validation checks to run", "e.g. #RELS_1", 200,
        AlgorithmParameter.Type.MULTI, "");

    validationChecks = new ArrayList<>();
    validationChecks.add("#RELS_1");
    validationChecks.add("#RELS_2");
    validationChecks.add("#RELS_3");
    validationChecks.add("#RELS_4");
    validationChecks.add("#RELS_5");
    validationChecks.add("#RELS_6");
    validationChecks.add("#RELS_7");
    validationChecks.add("#RELS_8");
    validationChecks.add("#RELS_9");
    validationChecks.add("#RELS_10");
    
    Collections.sort(validationChecks);
    param.setPossibleValues(validationChecks);
    params.add(param);
    
    return params;
  }

  @Override
  public String getDescription() {
    return "Validation checks related to relationships in the inversion files.";
  }
}