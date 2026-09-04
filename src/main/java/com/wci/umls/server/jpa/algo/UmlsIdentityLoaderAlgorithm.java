/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import com.wci.umls.server.model.algo.AlgorithmParameter;
import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.jpa.model.AlgorithmParameterJpa;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.jpa.model.meta.AtomIdentityJpa;
import com.wci.umls.server.jpa.model.meta.AttributeIdentityJpa;
import com.wci.umls.server.jpa.model.meta.LexicalClassIdentityJpa;
import com.wci.umls.server.jpa.model.meta.RelationshipIdentityJpa;
import com.wci.umls.server.jpa.model.meta.SemanticTypeComponentIdentityJpa;
import com.wci.umls.server.jpa.model.meta.StringClassIdentityJpa;
import com.wci.umls.server.jpa.services.UmlsIdentityServiceJpa;
import com.wci.umls.server.model.meta.AtomIdentity;
import com.wci.umls.server.model.meta.AttributeIdentity;
import com.wci.umls.server.model.meta.IdType;
import com.wci.umls.server.model.meta.LexicalClassIdentity;
import com.wci.umls.server.model.meta.RelationshipIdentity;
import com.wci.umls.server.model.meta.SemanticTypeComponentIdentity;
import com.wci.umls.server.model.meta.StringClassIdentity;
import com.wci.umls.server.services.ContentService;
import com.wci.umls.server.services.UmlsIdentityService;

/**
 * Implementation of an algorithm to compute transitive closure using the
 * {@link ContentService}.
 */
public class UmlsIdentityLoaderAlgorithm
    extends AbstractTerminologyLoaderAlgorithm {

  /**
   * Instantiates an empty {@link UmlsIdentityLoaderAlgorithm}.
   * @throws Exception if anything goes wrong
   */
  public UmlsIdentityLoaderAlgorithm() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {

    logInfo("Umls Identity Loader");
    logInfo("  terminology = " + getTerminology());
    logInfo("  inputPath = " + getInputPath());
    fireProgressEvent(0, "Starting...");

    final UmlsIdentityService service = new UmlsIdentityServiceJpa();
    try {
      service.setTransactionPerOperation(false);
      service.beginTransaction();

      //
      // Handle AttributeIdentity
      // id|terminologyId|terminology|componentId|componentType|componentTerminology|name|hashcode
      //
      final File attributeIdentityFile =
          getInputPathFile("attributeIdentity.txt");
      if (attributeIdentityFile.exists()) {
        logInfo("  Load attribute identity");

        final BufferedReader in = new BufferedReader(
            new FileReader(attributeIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }
          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final AttributeIdentity identity = new AttributeIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setTerminologyId(fields[1]);
          identity.setTerminology(fields[2]);
          identity.setComponentId(fields[3]);
          identity.setComponentType(IdType.valueOf(fields[4]));
          identity.setComponentTerminology(fields[5]);
          identity.setName(fields[6]);
          identity.setHashcode(fields[7]);
          service.addAttributeIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        in.close();
        service.commitClearBegin();
        logInfo("    count = " + ct);
      }

      //
      // Handle SemanticTypeIdentity
      // id|conceptTerminologyId|terminology|semanticType
      //
      final File semanticTypeIdentityFile =
          getInputPathFile("semanticTypeComponentIdentity.txt");
      if (semanticTypeIdentityFile.exists()) {
        logInfo("  Load semanticType identity");

        final BufferedReader in = new BufferedReader(new FileReader(
            semanticTypeIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }
          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final SemanticTypeComponentIdentity identity =
              new SemanticTypeComponentIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setConceptTerminologyId(fields[1]);
          identity.setTerminology(fields[2]);
          identity.setSemanticType(fields[3]);
          service.addSemanticTypeComponentIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        service.commitClearBegin();
        in.close();
        logInfo("    count = " + ct);
      }

      //
      // Handle AtomIdentity
      // id|stringClassId|terminology|terminologyId|termType|codeId|conceptId|descriptorId
      //
      final File atomIdentityFile = getInputPathFile("atomIdentity.txt");
      if (atomIdentityFile.exists()) {
        logInfo("  Load atom identity");

        final BufferedReader in = new BufferedReader(
            new FileReader(atomIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }
          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final AtomIdentity identity = new AtomIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setStringClassId(fields[1]);
          identity.setTerminology(fields[2]);
          identity.setTerminologyId(fields[3]);
          identity.setTermType(fields[4]);
          identity.setCodeId(fields[5]);
          identity.setConceptId(fields[6]);
          identity.setDescriptorId(fields[7]);
          service.addAtomIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        in.close();
        service.commitClearBegin();
        logInfo("    count = " + ct);
      }

      //
      // Handle StringClassIdentity
      // id|language|name
      //
      final File stringClassIdentityFile =
          getInputPathFile("stringClassIdentity.txt");
      if (stringClassIdentityFile.exists()) {
        logInfo("  Load string identity");

        final BufferedReader in = new BufferedReader(new FileReader(
            stringClassIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }
          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final StringClassIdentity identity = new StringClassIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setLanguage(fields[1]);
          identity.setName(fields[2]);
          service.addStringClassIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        in.close();
        service.commitClearBegin();
        logInfo("    count = " + ct);
      }

      //
      // Handle LexicalClassIdentity
      // id|normalizedName
      //
      final File lexicalClassIdentityFile =
          getInputPathFile("lexicalClassIdentity.txt");
      if (lexicalClassIdentityFile.exists()) {
        logInfo("  Load lexicalClass identity");

        final BufferedReader in = new BufferedReader(new FileReader(
            lexicalClassIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }
          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final LexicalClassIdentity identity = new LexicalClassIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setLanguage(fields[1]);
          identity.setNormalizedName(fields[2]);
          service.addLexicalClassIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        in.close();
        service.commitClearBegin();
        logInfo("    count = " + ct);
      }

      //
      // Handle RelationshipIdentity
      // id|terminology|terminologyId|type|additionalType|fromId|fromType|fromTerminology|toId|toType|toTerminology|inverseId
      //
      final File relationshipIdentityFile =
          getInputPathFile("relationshipIdentity.txt");
      if (relationshipIdentityFile.exists()) {
        logInfo("  Load relationship identity");

        final BufferedReader in = new BufferedReader(new FileReader(
            relationshipIdentityFile, StandardCharsets.UTF_8));
        String line;
        int ct = 0;
        while ((line = in.readLine()) != null) {
          if (isCancelled()) {
            in.close();
            return;
          }

          final String[] fields = FieldedStringTokenizer.split(line, "|");
          final RelationshipIdentity identity = new RelationshipIdentityJpa();
          identity.setId(Long.valueOf(fields[0]));
          identity.setTerminology(fields[1]);
          identity.setTerminologyId(fields[2]);
          identity.setRelationshipType(fields[3]);
          identity.setAdditionalRelationshipType(fields[4]);
          identity.setFromId(fields[5]);
          identity.setFromType(IdType.valueOf(fields[6]));
          identity.setFromTerminology(fields[7]);
          identity.setToId(fields[8]);
          identity.setToType(IdType.valueOf(fields[9]));
          identity.setToTerminology(fields[10]);
          identity.setInverseId(Long.valueOf(fields[11]));
          service.addRelationshipIdentity(identity);
          if (++ct % commitCt == 0) {
            service.commitClearBegin();
          }
        }
        in.close();
        service.commitClearBegin();
        logInfo("    count = " + ct);
      }

      service.commit();
      fireProgressEvent(0, "Finished...");
    } catch (Exception e) {
      logError("FAILED to assign identity");
      throw e;
    } finally {
      service.close();
    }
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    throw new UnsupportedOperationException();
  }

  /* see superclass */
  @Override
  public String getFileVersion() throws Exception {
    return new RrfFileSorter().getFileVersion(getInputPathDirectory());
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void checkProperties(Properties p) throws Exception {
    checkRequiredProperties(new String[] {
        "inputFile"
    }, p);
  }

  /* see superclass */
  @Override
  public void setProperties(Properties p) throws Exception {

    if (p.getProperty("inputDir") != null) {
      setInputPath(p.getProperty("inputDir"));
    }

    if (p.getProperty("inputDir") != null) {
      setInputPath(p.getProperty("inputDir"));
    }

  }

  /* see superclass */
  @Override
  public List<AlgorithmParameter> getParameters()  throws Exception{
    final List<AlgorithmParameter> params = super.getParameters();
    AlgorithmParameter param = new AlgorithmParameterJpa("Input Dir",
        "inputDir", "Input UMLS UI Files directory to load", "", 255,
        AlgorithmParameter.Type.DIRECTORY, "");
    params.add(param);
    return params;

  }
}
