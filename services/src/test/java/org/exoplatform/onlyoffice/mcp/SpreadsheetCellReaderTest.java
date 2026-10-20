/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import org.exoplatform.onlyoffice.mcp.model.SpreadsheetCellsModel;
import org.exoplatform.onlyoffice.mcp.model.SpreadsheetCellsModel.CellModel;

/**
 * Reads workbooks built with POI, as Excel stores them: the cells with their
 * positions, the cached formula values, the dates, the sheets and the merged
 * ranges.
 */
public class SpreadsheetCellReaderTest {

  /**
   * Builds a buyer's grid: a "Grille" sheet holding a text, a number, a formula with its cached value, a date, a
   * percentage and a merged title, then an "Intro" sheet, the active one, so
   * the active sheet is not the first.
   *
   * @return the xlsx file
   * @throws IOException never
   */
  static byte[] buyerGrid() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet grid = workbook.createSheet("Grille");
      Row title = grid.createRow(1);
      title.createCell(1).setCellValue("Exigences");
      grid.addMergedRegion(new CellRangeAddress(1, 1, 1, 3));
      Row delay = grid.createRow(2);
      delay.createCell(1).setCellValue("Délai (jours)");
      delay.createCell(2).setCellValue(30);
      Row doubled = grid.createRow(3);
      doubled.createCell(2).setCellFormula("C3*2");
      Row date = grid.createRow(4);
      CellStyle dateStyle = workbook.createCellStyle();
      dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
      date.createCell(2).setCellValue(LocalDateTime.of(2026, 11, 3, 0, 0));
      date.getCell(2).setCellStyle(dateStyle);
      Row rate = grid.createRow(5);
      CellStyle percent = workbook.createCellStyle();
      percent.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0%"));
      rate.createCell(2).setCellValue(0.5);
      rate.getCell(2).setCellStyle(percent);
      rate.createCell(3).setCellValue("");
      workbook.createSheet("Intro").createRow(0).createCell(0).setCellValue("Read me");
      workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
      workbook.setActiveSheet(1);
      return toBytes(workbook);
    }
  }

  /**
   * Writes a workbook to its file.
   *
   * @param workbook the workbook
   * @return the file
   * @throws IOException never
   */
  private static byte[] toBytes(Workbook workbook) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out.toByteArray();
  }

  /**
   * Indexes the cells read by their position.
   *
   * @param model the cells read
   * @return the cells by position
   */
  private static Map<String, CellModel> byRef(SpreadsheetCellsModel model) {
    return model.cells().stream().collect(Collectors.toMap(CellModel::ref, Function.identity()));
  }

  /**
   * A range of a named sheet comes back cell by cell, empty cells left out:
   * the text, the whole number as a number, the formula with its cached value,
   * the date as ISO-8601, the percentage with its displayed text, and the
   * merged title's range; the sheets are listed with their used range and the
   * active one.
   * Mutants: empty cells kept, the formula's value recalculated or dropped,
   * the displayed text always or never given, the merged ranges dropped, the
   * active flag on the wrong sheet.
   *
   * @throws IOException never
   */
  @Test
  public void aRangeOfANamedSheetComesBackCellByCell() throws IOException {
    SpreadsheetCellsModel model = SpreadsheetCellReader.read("id", "grid.xlsx", buyerGrid(), "Grille", "B2:D6");

    assertEquals("Grille", model.sheet());
    assertEquals("B2:D6", model.range());
    Map<String, CellModel> cells = byRef(model);
    assertEquals(List.of("B2", "B3", "C3", "C4", "C5", "C6"), model.cells().stream().map(CellModel::ref).toList());
    assertEquals("Exigences", cells.get("B2").value());
    assertNull(cells.get("B2").text());
    assertEquals(30L, cells.get("C3").value());
    assertEquals("=C3*2", cells.get("C4").formula());
    assertEquals(60L, cells.get("C4").value());
    assertEquals("2026-11-03T00:00", cells.get("C5").value());
    assertEquals("2026-11-03", cells.get("C5").text());
    assertEquals(0.5, cells.get("C6").value());
    assertEquals("50%", cells.get("C6").text());
    assertEquals(List.of("B2:D2"), model.mergedRanges());
    assertFalse(model.truncated());
    assertEquals(2, model.sheets().size());
    assertFalse(model.sheets().get(0).active());
    assertTrue(model.sheets().get(1).active());
    assertEquals("B2:D6", model.sheets().get(0).usedRange());
  }

  /**
   * Without a sheet the active one is read, over its used range; a sheet
   * named with another case is found, and a 'Sheet!range' prefix names the
   * sheet.
   * Mutants: the first sheet read instead of the active one, the case
   * compared strictly, the prefix ignored.
   *
   * @throws IOException never
   */
  @Test
  public void theSheetIsTheActiveOneOrTheOneNamed() throws IOException {
    byte[] grid = buyerGrid();

    SpreadsheetCellsModel intro = SpreadsheetCellReader.read("id", "grid.xlsx", grid, null, null);
    assertEquals("Intro", intro.sheet());
    assertEquals("A1", intro.range());
    assertEquals("Read me", intro.cells().get(0).value());

    assertEquals("Grille", SpreadsheetCellReader.read("id", "grid.xlsx", grid, "grille", "C3").sheet());
    SpreadsheetCellsModel prefixed = SpreadsheetCellReader.read("id", "grid.xlsx", grid, null, "'Grille'!C3");
    assertEquals("Grille", prefixed.sheet());
    assertEquals(30L, prefixed.cells().get(0).value());
  }

  /**
   * A sheet the workbook lacks is refused with the sheets it has; a range
   * that isn't A1 notation, and a file that isn't a workbook, are refused
   * with what to do instead.
   * Mutants: a missing sheet read as the active one, a bad range read as the
   * used range, a non-workbook read as empty.
   *
   * @throws IOException never
   */
  @Test
  public void aMissingSheetABadRangeAndANonWorkbookAreRefused() throws IOException {
    byte[] grid = buyerGrid();

    IllegalArgumentException missing = assertThrows(IllegalArgumentException.class,
                                                    () -> SpreadsheetCellReader.read("id", "grid.xlsx", grid, "Prix", null));
    assertTrue(missing.getMessage().contains("Grille, Intro"));
    IllegalArgumentException range = assertThrows(IllegalArgumentException.class,
                                                  () -> SpreadsheetCellReader.read("id", "grid.xlsx", grid, "Grille", "row 3"));
    assertTrue(range.getMessage().contains("A1 notation"));
    IllegalArgumentException notExcel = assertThrows(IllegalArgumentException.class,
                                                     () -> SpreadsheetCellReader.read("id", "notes.txt", "hello".getBytes(), null, null));
    assertTrue(notExcel.getMessage().contains("convert_document"));
  }

  /**
   * A range holding more cells than one call returns is cut at a row
   * boundary, says so, and names the rest of the range to read next; the
   * legacy xls format is read the same way.
   * Mutants: the cap dropped, the cut inside a row, the next range starting
   * at the wrong row or losing its columns.
   *
   * @throws IOException never
   */
  @Test
  public void aLargeRangeIsCutAtARowAndNamesTheNextRange() throws IOException {
    byte[] file;
    try (Workbook workbook = new HSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Data");
      for (int r = 0; r < 1000; r++) {
        Row row = sheet.createRow(r);
        for (int c = 0; c < 3; c++) {
          row.createCell(c).setCellValue(r * 3 + c);
        }
      }
      file = toBytes(workbook);
    }

    SpreadsheetCellsModel first = SpreadsheetCellReader.read("id", "data.xls", file, null, "A1:C1000");

    assertTrue(first.truncated());
    assertEquals(SpreadsheetCellReader.MAX_CELLS / 3 * 3, first.cells().size());
    assertEquals("C666", first.cells().get(first.cells().size() - 1).ref());
    assertEquals("A667:C1000", first.nextRange());
    SpreadsheetCellsModel rest = SpreadsheetCellReader.read("id", "data.xls", file, null, first.nextRange());
    assertFalse(rest.truncated());
    assertEquals(334 * 3, rest.cells().size());
  }

  /**
   * The sheet names come back in order, and a sheet is found by its name
   * exactly first, then ignoring the case.
   * Mutants: the order changed, a case-insensitive match preferred over an
   * exact one.
   *
   * @throws IOException never
   */
  @Test
  public void sheetNamesAreListedInOrderAndFoundExactlyFirst() throws IOException {
    assertEquals(List.of("Grille", "Intro"), SpreadsheetCellReader.sheetNames(buyerGrid()));
    assertEquals("grille", SpreadsheetCellReader.findSheetName(List.of("Grille", "grille"), "grille"));
    assertEquals("Grille", SpreadsheetCellReader.findSheetName(List.of("Grille"), " GRILLE "));
    assertNull(SpreadsheetCellReader.findSheetName(List.of("Grille"), "Prix"));
  }

  /**
   * A range larger than what the sheet holds, a whole sheet included, is
   * read over the cells the sheet holds only, and named so; a range holding
   * none of them gives no cell.
   * Mutants: the range not clamped, the clamp losing a cell, a range outside
   * the cells read as the used range.
   *
   * @throws IOException never
   */
  @Test
  public void aRangeIsReadOverTheCellsTheSheetHoldsOnly() throws IOException {
    byte[] grid = buyerGrid();

    SpreadsheetCellsModel whole = SpreadsheetCellReader.read("id", "grid.xlsx", grid, "Grille", "A1:XFD1048576");
    SpreadsheetCellsModel outside = SpreadsheetCellReader.read("id", "grid.xlsx", grid, "Grille", "Z100:Z200");

    assertEquals("B2:D6", whole.range());
    assertEquals(List.of("B2", "B3", "C3", "C4", "C5", "C6"), whole.cells().stream().map(CellModel::ref).toList());
    assertNull(outside.range());
    assertTrue(outside.cells().isEmpty());
  }

  /**
   * The sheet names of a legacy xls are read too, by opening it.
   * Mutants: the fallback dropped.
   *
   * @throws IOException never
   */
  @Test
  public void theSheetNamesOfAnXlsAreReadToo() throws IOException {
    byte[] file;
    try (Workbook workbook = new HSSFWorkbook()) {
      workbook.createSheet("One");
      workbook.createSheet("Two");
      file = toBytes(workbook);
    }

    assertEquals(List.of("One", "Two"), SpreadsheetCellReader.sheetNames(file));
  }

  /**
   * Builds an xlsx holding a "Data" sheet and a "Macro1" sheet whose part is
   * turned into another kind of sheet, as Excel stores one, optionally padded
   * with a stored entry of the given size.
   *
   * @param contentType the second sheet's content type
   * @param relationType the second sheet's relationship type
   * @param padding the size of the padding entry, 0 for none
   * @return the file
   * @throws IOException never
   */
  static byte[] xlsxWithSecondSheetAs(String contentType, String relationType, int padding) throws IOException {
    byte[] plain;
    try (Workbook workbook = new XSSFWorkbook()) {
      workbook.createSheet("Data");
      workbook.createSheet("Macro1");
      plain = toBytes(workbook);
    }
    ByteArrayOutputStream rewritten = new ByteArrayOutputStream();
    try (java.util.zip.ZipInputStream in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(plain));
         java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(rewritten)) {
      java.util.zip.ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (entry.getName().equals("[Content_Types].xml")) {
          content = content.replace("<Override ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\" PartName=\"/xl/worksheets/sheet2.xml\"/>",
                                    "<Override ContentType=\"" + contentType + "\" PartName=\"/xl/worksheets/sheet2.xml\"/>");
          // a package part needs a content type, the padding's included
          content = content.replaceFirst("<Default ", "<Default ContentType=\"application/octet-stream\" Extension=\"bin\"/><Default ");
        } else if (entry.getName().equals("xl/_rels/workbook.xml.rels")) {
          content = content.replaceAll("Target=\"worksheets/sheet2.xml\" Type=\"[^\"]+\"",
                                       "Target=\"worksheets/sheet2.xml\" Type=\"" + relationType + "\"");
        }
        out.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
        out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        out.closeEntry();
      }
      if (padding > 0) {
        byte[] noise = new byte[padding];
        new java.util.Random(1).nextBytes(noise);
        java.util.zip.ZipEntry pad = new java.util.zip.ZipEntry("docProps/padding.bin");
        pad.setMethod(java.util.zip.ZipEntry.STORED);
        pad.setSize(noise.length);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(noise);
        pad.setCrc(crc.getValue());
        out.putNextEntry(pad);
        out.write(noise);
        out.closeEntry();
      }
    }
    return rewritten.toByteArray();
  }

  /**
   * An xlsx's Excel 4 macro sheet and dialog sheet are not among its sheet
   * names, as a workbook loaded whole, the one get_spreadsheet_cells reads,
   * leaves them out; and an xlsx over the size a workbook is loaded at still
   * gives its names, its sheets never parsed.
   * Mutants: a macro or dialog sheet kept, an xlsx over the size refused.
   *
   * @throws IOException never
   */
  @Test
  public void aMacroOrDialogSheetIsNotASheetNameAndALargeXlsxStillGivesItsNames() throws IOException {
    byte[] macro = xlsxWithSecondSheetAs("application/vnd.ms-excel.macrosheet+xml",
                                         "http://schemas.microsoft.com/office/2006/relationships/xlMacrosheet",
                                         0);
    byte[] dialog = xlsxWithSecondSheetAs("application/vnd.openxmlformats-officedocument.spreadsheetml.dialogsheet+xml",
                                          "http://schemas.openxmlformats.org/officeDocument/2006/relationships/dialogsheet",
                                          0);
    byte[] large = xlsxWithSecondSheetAs("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml",
                                         "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet",
                                         (int) SpreadsheetCellReader.MAX_WORKBOOK_BYTES + 1);

    assertEquals(List.of("Data"), SpreadsheetCellReader.sheetNames(macro));
    assertEquals(List.of("Data"), SpreadsheetCellReader.sheetNames(dialog));
    assertTrue(large.length > SpreadsheetCellReader.MAX_WORKBOOK_BYTES);
    assertEquals(List.of("Data", "Macro1"), SpreadsheetCellReader.sheetNames(large));
  }

}
