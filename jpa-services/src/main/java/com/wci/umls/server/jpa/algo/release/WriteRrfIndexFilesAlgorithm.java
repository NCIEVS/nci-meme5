/*
 *    Copyright 2017 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Query;

import com.google.common.io.Files;
import com.wci.umls.server.ValidationResult;
import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.FieldedStringTokenizer;
import com.wci.umls.server.jpa.ValidationResultJpa;
import com.wci.umls.server.jpa.algo.AbstractInsertMaintReleaseAlgorithm;
import com.wci.umls.server.jpa.algo.FileSorter;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.meta.Language;
import com.wci.umls.server.services.handlers.NormalizedStringHandler;

/**
 * Algorithm to write the RRF index files.
 */
public class WriteRrfIndexFilesAlgorithm
    extends AbstractInsertMaintReleaseAlgorithm {

  /** The writer map. */
  private Map<String, PrintWriter> writerMap = new HashMap<>();

  /** The dir. */
  private File dir = null;

  /**
   * Instantiates an empty {@link WriteRrfIndexFilesAlgorithm}.
   *
   * @throws Exception the exception
   */
  public WriteRrfIndexFilesAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("WRITERRFINDEXES");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {
    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    openWriters();

    // initialize progress monitoring
    Query query = manager.createQuery("select count(*) from ConceptJpa c "
        + "where c.publishable = true and terminology = :terminology");
    query.setParameter("terminology", getProject().getTerminology());
    setSteps(Integer.parseInt(query.getSingleResult().toString()));

    final NormalizedStringHandler handler = getNormalizedStringHandler();

    // process one concept at a time
    query = getEntityManager().createQuery(
        "select a.id from ConceptJpa a WHERE a.publishable = true and "
            + "terminology = :terminology order by a.terminologyId");
    query.setParameter("terminology", getProject().getTerminology());
    @SuppressWarnings("unchecked")
    final List<Long> conceptIds = query.getResultList();
    for (final Long conceptId : conceptIds) {
      final Concept c = getConcept(conceptId);

      // caching to support only unique rows in output
      final Set<String> seen = new HashSet<>();
      final Set<String> wordsSeen = new HashSet<>();

      for (final Atom atom : c.getAtoms()) {
        if (atom.isPublishable()) {

          // MRXNS_ENG.RRF

          // 0 LAT Abbreviation of language of the string (always ENG in this
          // edition of the Metathesaurus)
          // 1 NSTR Normalized string in lowercase
          // 2 CUI Concept identifier
          // 3 LUI Term identifier
          // 4 SUI String identifier
          final String normalizedString =
              handler.getNormalizedString(atom.getName());

          if (atom.getLanguage().equals("ENG")
              && !seen.contains("MRXNS" + atom.getStringClassId())
              && !ConfigUtility.isEmpty(normalizedString)) {

            final StringBuilder sb = new StringBuilder();
            // 0 LAT
            sb.append("ENG").append("|");
            // 1 NSTR
            sb.append(normalizedString).append("|");
            // 2 CUI
            sb.append(c.getTerminologyId()).append("|");
            // 3 LUI
            sb.append(atom.getLexicalClassId()).append("|");
            // 4 SUI
            sb.append(atom.getStringClassId()).append("|");
            sb.append("\n");
            writerMap.get("MRXNS_ENG.RRF").write(sb.toString());
            seen.add("MRXNS" + atom.getStringClassId());

            // MRXNW_ENG.RRF
            for (final String word : FieldedStringTokenizer
                .split(normalizedString, ConfigUtility.PUNCTUATION)) {
              
              if (!ConfigUtility.isEmpty(word) && !wordsSeen
                  .contains("MRXNW" + word + atom.getStringClassId())) {
                final StringBuilder sb2 = new StringBuilder();
                // 0 LAT
                sb2.append("ENG").append("|");
                // 1 WORD
                sb2.append(word).append("|");
                // 2 CUI
                sb2.append(c.getTerminologyId()).append("|");
                // 3 LUI
                sb2.append(atom.getLexicalClassId()).append("|");
                // 4 SUI
                sb2.append(atom.getStringClassId()).append("|");
                sb2.append("\n");
                writerMap.get("MRXNW_ENG.RRF").write(sb2.toString());
                wordsSeen.add("MRXNW" + word + atom.getStringClassId());
              }
            }
          }

          if (!seen.contains("MRXW" + atom.getStringClassId())) {
            // for all languages write MRXW_<language>.RRF
            for (final String word : FieldedStringTokenizer
                .split(atom.getName(), ConfigUtility.PUNCTUATION)) {
              if (!ConfigUtility.isEmpty(word) && !wordsSeen.
                  contains("MRXW_" + atom.getLanguage() + word
                  + atom.getStringClassId())) {
                final StringBuilder sb = new StringBuilder();
                // 0 LAT
                sb.append(atom.getLanguage()).append("|");
                // 1 WORD
                sb.append(word).append("|");
                // 2 CUI
                sb.append(c.getTerminologyId()).append("|");
                // 3 LUI
                sb.append(atom.getLexicalClassId()).append("|");
                // 4 SUI
                sb.append(atom.getStringClassId()).append("|");
                sb.append("\n");
                writerMap.get("MRXW_" + atom.getLanguage() + ".RRF")
                    .write(sb.toString());
                wordsSeen.add("MRXW_" + atom.getLanguage() + word
                    + atom.getStringClassId());
              }
            }
            seen.add("MRXW" + atom.getStringClassId());
          }
        }
      }
      updateProgress();
    }
    closeWriters();
    logInfo("Finished " + getName());
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
    // n/a
  }

  /**
   * Open writers.
   *
   * @throws Exception the exception
   */
  private void openWriters() throws Exception {
    dir = new File(config.getProperty("source.data.dir") + "/"
        + getProcess().getInputPath() + "/" + getProcess().getVersion() + "/"
        + "META");

    writerMap.put("MRXNS_ENG.RRF",
        new PrintWriter(new FileWriter(new File(dir, "MRXNS_ENG.RRF"))));

    writerMap.put("MRXNW_ENG.RRF",
        new PrintWriter(new FileWriter(new File(dir, "MRXNW_ENG.RRF"))));
    for (Language lat : getLanguages(getProject().getTerminology(),
        getProject().getVersion()).getObjects()) {
      writerMap.put("MRXW_" + lat.getAbbreviation() + ".RRF",
          new PrintWriter(new FileWriter(
              new File(dir, "MRXW_" + lat.getAbbreviation() + ".RRF"))));
    }
  }

  /**
   * Close writers.
   *
   * @throws Exception the exception
   */
  private void closeWriters() throws Exception {
    // close writers
    for (PrintWriter writer : writerMap.values()) {
      writer.close();
    }

    // sort files
    for (String writerName : writerMap.keySet()) {
      File inputFile = new File(dir, writerName);
      File outputFile = new File(dir, writerName + ".sorted");
      FileSorter.sortFile(inputFile.getAbsolutePath(),
          outputFile.getAbsolutePath(), ConfigUtility.getByteComparator());
    }

    // move sorted files into orig files
    for (String writerName : writerMap.keySet()) {

      File inputFile = new File(dir, writerName);
      File outputFile = new File(dir, writerName + ".sorted");
      inputFile.delete();
      Files.move(outputFile.getAbsoluteFile(), inputFile.getAbsoluteFile());
    }

  }

  /* see superclass */
  @Override
  public String getDescription() {
    return ConfigUtility.getNameFromClass(getClass());
  }
}
