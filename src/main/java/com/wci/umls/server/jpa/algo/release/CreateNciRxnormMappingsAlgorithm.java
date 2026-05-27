/*
 * Copyright 2024 Wci Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of Wci Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * Wci Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package com.wci.umls.server.jpa.algo.release;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.QueryType;
import com.wci.umls.server.jpa.algo.AbstractAlgorithm;
import com.wci.umls.server.jpa.model.ValidationResultJpa;
import com.wci.umls.server.model.algo.ValidationResult;

/**
 * Algorithm for creating an NCI to RxNorm mapping workbook.
 */
public class CreateNciRxnormMappingsAlgorithm extends AbstractAlgorithm {

  /** The path meta. */
  private File pathMeta = null;

  /** The mappings dir. */
  private File mappingsDir = null;

  /** The output workbook file. */
  private File outputFile = null;

  /** The mappings worksheet name. */
  private static final String MAPPINGS_SHEET = "Mappings";

  /** The readme worksheet name. */
  private static final String README_SHEET = "README";

  /** The output file prefix. */
  private static final String OUTPUT_FILE_NAME =
      "NCI_RXNORM_PT_IN_Only_Mapping.xlsx";

  /** The mapping headers. */
  private static final String[] MAPPING_HEADERS = {
      "NCI Meta CUI", "NCI Meta Concept Name", "NCI Code", "NCI PT",
      "Source Atom Code", "Source Atom Name", "Source", "Version",
      "Source Term Type"
  };

  /** The mapping query. */
  private static final String MAPPINGS_QUERY =
      "select distinct c1.terminologyId 'NCI Meta CUI', "
          + "c1.name 'NCI Meta Concept Name', "
          + "a1.codeId 'NCI Code', "
          + "a1.name 'NCI PT', "
          + "a2.codeId 'Source Atom Code', "
          + "a2.name 'Source Atom Name', "
          + "a2.terminology Source, "
          + "a2.version 'Version', "
          + "a2.termType 'Source Term Type' "
          + "from concepts c1, "
          + "atoms a1, "
          + "concepts c2, "
          + "atoms a2, "
          + "concepts_atoms ca1, "
          + "concepts_atoms ca2 "
          + "where c1.terminology = 'NCIMTH' "
          + "and c2.terminology = 'NCIMTH' "
          + "and c1.id = ca1.concepts_id "
          + "and ca1.atoms_id = a1.id "
          + "and c2.id = ca2.concepts_id "
          + "and ca2.atoms_id = a2.id "
          + "and c1.id = c2.id "
          + "and a1.terminology = 'NCI' "
          + "and a1.termType = 'PT' "
          + "and a1.publishable = true "
          + "and a2.terminology = 'RXNORM' "
          + "and a2.termType = 'IN' "
          + "and a2.publishable = true "
          + "order by c1.terminologyId";

  /** The README query. */
  private static final String README_QUERY =
      "select distinct terminology 'Source', version 'Version' "
          + "from terminologies "
          + "where current and terminology in ('RXNORM') "
          + "order by terminology, version";

  /**
   * Instantiates an empty {@link CreateNciRxnormMappingsAlgorithm}.
   *
   * @throws Exception the exception
   */
  public CreateNciRxnormMappingsAlgorithm() throws Exception {
    super();
    setActivityId(UUID.randomUUID().toString());
    setWorkId("NCIMRXNORMMAPPINGS");
  }

  /* see superclass */
  @Override
  public ValidationResult checkPreconditions() throws Exception {

    final File path = new File(config.getProperty("source.data.dir") + "/"
        + getProcess().getInputPath());

    pathMeta = new File(path, "/" + getProcess().getVersion() + "/META");
    logInfo("  pathMeta " + pathMeta);

    return new ValidationResultJpa();
  }

  /* see superclass */
  @Override
  public void compute() throws Exception {
    logInfo("Starting " + getName());

    mappingsDir = new File(pathMeta, "mappings");
    if (!mappingsDir.exists()) {
      ConfigUtility.ensureDirectoryExists(mappingsDir);
    }
    logInfo("mappings dir:" + mappingsDir);

    outputFile = new File(mappingsDir, OUTPUT_FILE_NAME);

    SXSSFWorkbook workbook = new SXSSFWorkbook(100);
    workbook.setCompressTempFiles(true);
    try {
      final CellStyle headerStyle = createHeaderStyle(workbook);

      // README must be the first tab in the workbook.
      createReadmeSheet(workbook, headerStyle);
      createMappingsSheet(workbook, headerStyle);

      try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
        workbook.write(outputStream);
      }
    } finally {
      workbook.close();
      workbook.dispose();
    }

    logInfo("Created workbook " + outputFile.getName());
    logInfo("Finished " + getName());
  }

  /**
   * Create the README sheet.
   *
   * @param workbook the workbook
   * @param headerStyle the header style
   * @throws Exception the exception
   */
  private void createReadmeSheet(Workbook workbook, CellStyle headerStyle)
    throws Exception {

    final Sheet sheet = workbook.createSheet(README_SHEET);
    int rowNum = 0;

    Row row = sheet.createRow(rowNum++);
    writeCell(row, 0, "NCIm version");
    writeCell(row, 1, getProcess().getVersion());

    rowNum++;

    row = sheet.createRow(rowNum++);
    writeCell(row, 0, "Source", headerStyle);
    writeCell(row, 1, "Version", headerStyle);

    final List<Object[]> results = executeQuery(README_QUERY, QueryType.SQL,
        getDefaultQueryParams(this.getProject()), false);

    for (final Object[] result : results) {
      row = sheet.createRow(rowNum++);
      writeCell(row, 0, safeString(result, 0));
      writeCell(row, 1, safeString(result, 1));
    }

    sheet.setColumnWidth(0, 24 * 256);
    sheet.setColumnWidth(1, 20 * 256);
  }

  /**
   * Create the mappings sheet.
   *
   * @param workbook the workbook
   * @param headerStyle the header style
   * @throws Exception the exception
   */
  private void createMappingsSheet(Workbook workbook, CellStyle headerStyle)
    throws Exception {

    final Sheet sheet = workbook.createSheet(MAPPINGS_SHEET);

    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < MAPPING_HEADERS.length; i++) {
      writeCell(headerRow, i, MAPPING_HEADERS[i], headerStyle);
    }

    final List<Object[]> results = executeQuery(MAPPINGS_QUERY, QueryType.SQL,
        getDefaultQueryParams(this.getProject()), false);

    int rowNum = 1;
    int ct = 0;
    for (final Object[] result : results) {
      final Row row = sheet.createRow(rowNum++);
      for (int i = 0; i < MAPPING_HEADERS.length; i++) {
        writeCell(row, i, safeString(result, i));
      }

      ct++;
      if (ct % 5000 == 0) {
        logInfo("mapping ct: " + ct);
      }
    }

    sheet.createFreezePane(0, 1);
    sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, MAPPING_HEADERS.length - 1));
    setMappingsColumnWidths(sheet);
  }

  /**
   * Create the header style.
   *
   * @param workbook the workbook
   * @return the cell style
   */
  private CellStyle createHeaderStyle(Workbook workbook) {
    final Font boldFont = workbook.createFont();
    boldFont.setBold(true);

    final CellStyle headerStyle = workbook.createCellStyle();
    headerStyle.setFont(boldFont);
    return headerStyle;
  }

  /**
   * Set workbook-friendly column widths.
   *
   * @param sheet the mappings sheet
   */
  private void setMappingsColumnWidths(Sheet sheet) {
    final int[] widths = {
        18, 38, 18, 38, 18, 38, 16, 18, 18
    };

    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i] * 256);
    }
  }

  /**
   * Write a string cell.
   *
   * @param row the row
   * @param column the column
   * @param value the value
   */
  private void writeCell(Row row, int column, String value) {
    writeCell(row, column, value, null);
  }

  /**
   * Write a string cell with style.
   *
   * @param row the row
   * @param column the column
   * @param value the value
   * @param style the style
   */
  private void writeCell(Row row, int column, String value, CellStyle style) {
    final Cell cell = row.createCell(column);
    cell.setCellValue(value == null ? "" : value);
    if (style != null) {
      cell.setCellStyle(style);
    }
  }

  /**
   * Safely convert an array value to string.
   *
   * @param result the result row
   * @param index the column index
   * @return the string value
   */
  private String safeString(Object[] result, int index) {
    if (result == null || index >= result.length || result[index] == null) {
      return "";
    }
    return result[index].toString();
  }

  /* see superclass */
  @Override
  public void reset() throws Exception {
    logInfo("Starting RESET " + getName());

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
