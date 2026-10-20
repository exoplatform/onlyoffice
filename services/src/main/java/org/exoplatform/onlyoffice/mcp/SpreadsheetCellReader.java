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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

import org.exoplatform.onlyoffice.mcp.model.SpreadsheetCellsModel;
import org.exoplatform.onlyoffice.mcp.model.SpreadsheetCellsModel.CellModel;
import org.exoplatform.onlyoffice.mcp.model.SpreadsheetCellsModel.SheetModel;

/**
 * Reads the cells of an Excel workbook (xlsx or xls) with their A1 positions,
 * as stored in the file: a formula's value is the one computed when the file
 * was last saved, nothing is recalculated.
 */
final class SpreadsheetCellReader {

  /** The most non-empty cells one call returns. */
  static final int MAX_CELLS = 2000;

  private SpreadsheetCellReader() {
  }

  /**
   * Lists the sheet names of a workbook, in order.
   *
   * @param bytes the workbook file
   * @return the sheet names
   * @throws IllegalArgumentException when the file isn't an Excel workbook
   */
  static List<String> sheetNames(byte[] bytes) {
    try (Workbook workbook = open(bytes)) {
      List<String> names = new ArrayList<>();
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        names.add(workbook.getSheetName(i));
      }
      return names;
    } catch (IOException e) {
      throw new IllegalStateException("The spreadsheet could not be read: " + e.getMessage());
    }
  }

  /**
   * Finds a sheet name among a workbook's, exactly, else ignoring the case as
   * Excel does.
   *
   * @param names the workbook's sheet names
   * @param name the name asked
   * @return the name as the workbook spells it, null when none matches
   */
  static String findSheetName(List<String> names, String name) {
    String trimmed = StringUtils.trim(name);
    return names.stream()
                .filter(n -> n.equals(trimmed))
                .findFirst()
                .orElse(names.stream().filter(n -> n.equalsIgnoreCase(trimmed)).findFirst().orElse(null));
  }

  /**
   * Reads the non-empty cells of a range of one sheet, row by row, at most
   * {@link #MAX_CELLS}; past that, the answer is truncated at a row boundary
   * and names the range to read next.
   *
   * @param id the spreadsheet id
   * @param title the spreadsheet title
   * @param bytes the workbook file
   * @param sheetName the sheet to read, blank for the active one; a range
   *          prefixed with a sheet ({@code Grille!B2:F40}) names it too
   * @param range the range in A1 notation, blank for the sheet's used range
   * @return the cells read
   * @throws IllegalArgumentException when the file isn't an Excel workbook, or
   *           the sheet or the range can't be found or read
   */
  static SpreadsheetCellsModel read(String id, String title, byte[] bytes, String sheetName, String range) {
    String rangeRef = StringUtils.trimToNull(range);
    String requestedSheet = StringUtils.trimToNull(sheetName);
    if (rangeRef != null && rangeRef.contains("!")) {
      requestedSheet = StringUtils.removeEnd(StringUtils.removeStart(StringUtils.substringBeforeLast(rangeRef, "!"), "'"), "'");
      rangeRef = StringUtils.substringAfterLast(rangeRef, "!");
    }
    try (Workbook workbook = open(bytes)) {
      List<SheetModel> sheets = new ArrayList<>();
      List<String> names = new ArrayList<>();
      int activeIndex = workbook.getActiveSheetIndex();
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        Sheet sheet = workbook.getSheetAt(i);
        CellRangeAddress used = usedRange(sheet);
        names.add(sheet.getSheetName());
        sheets.add(new SheetModel(sheet.getSheetName(), used == null ? null : used.formatAsString(), i == activeIndex));
      }
      Sheet sheet;
      if (requestedSheet == null) {
        sheet = workbook.getSheetAt(activeIndex);
      } else {
        String actualName = findSheetName(names, requestedSheet);
        if (actualName == null) {
          throw new IllegalArgumentException("The spreadsheet has no sheet named '%s'. Its sheets are: %s."
                                                                                                           .formatted(requestedSheet,
                                                                                                                      String.join(", ",
                                                                                                                                  names)));
        }
        sheet = workbook.getSheet(actualName);
      }
      CellRangeAddress area = rangeRef == null ? usedRange(sheet) : parseRange(rangeRef);
      if (area == null) {
        return new SpreadsheetCellsModel(id, title, sheet.getSheetName(), null, sheets, List.of(), List.of(), false, null);
      }
      DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
      formatter.setUseCachedValuesForFormulaCells(true);
      List<CellModel> cells = new ArrayList<>();
      String nextRange = null;
      for (int r = area.getFirstRow(); r <= area.getLastRow() && nextRange == null; r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        List<CellModel> rowCells = new ArrayList<>();
        for (int c = area.getFirstColumn(); c <= area.getLastColumn(); c++) {
          CellModel cell = toCellModel(row.getCell(c), formatter);
          if (cell != null) {
            rowCells.add(cell);
          }
        }
        if (cells.size() + rowCells.size() > MAX_CELLS && !cells.isEmpty()) {
          nextRange = new CellRangeAddress(r, area.getLastRow(), area.getFirstColumn(), area.getLastColumn()).formatAsString();
        } else {
          cells.addAll(rowCells);
        }
      }
      List<String> merged = sheet.getMergedRegions()
                                 .stream()
                                 .filter(region -> region.intersects(area))
                                 .map(CellRangeAddress::formatAsString)
                                 .toList();
      return new SpreadsheetCellsModel(id,
                                       title,
                                       sheet.getSheetName(),
                                       area.formatAsString(),
                                       sheets,
                                       cells,
                                       merged,
                                       nextRange != null,
                                       nextRange);
    } catch (IOException e) {
      throw new IllegalStateException("The spreadsheet could not be read: " + e.getMessage());
    }
  }

  /**
   * Opens a workbook from its file.
   *
   * @param bytes the file
   * @return the workbook
   * @throws IOException when the file can't be read
   * @throws IllegalArgumentException when the file isn't an Excel workbook
   */
  private static Workbook open(byte[] bytes) throws IOException {
    try {
      return WorkbookFactory.create(new ByteArrayInputStream(bytes));
    } catch (IOException | RuntimeException e) {
      throw new IllegalArgumentException("The document isn't an Excel spreadsheet (xlsx or xls), so its cells can't be read. "
          + "Convert it to xlsx first with convert_document.");
    }
  }

  /**
   * Computes the range holding a sheet's cells.
   *
   * @param sheet the sheet
   * @return the range, null when the sheet has no cell
   */
  private static CellRangeAddress usedRange(Sheet sheet) {
    int firstRow = sheet.getFirstRowNum();
    int lastRow = sheet.getLastRowNum();
    if (firstRow < 0 || sheet.getPhysicalNumberOfRows() == 0) {
      return null;
    }
    int firstColumn = Integer.MAX_VALUE;
    int lastColumn = -1;
    for (int r = firstRow; r <= lastRow; r++) {
      Row row = sheet.getRow(r);
      if (row != null && row.getFirstCellNum() >= 0) {
        firstColumn = Math.min(firstColumn, row.getFirstCellNum());
        lastColumn = Math.max(lastColumn, row.getLastCellNum() - 1);
      }
    }
    return lastColumn < 0 ? null : new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
  }

  /**
   * Reads a range in A1 notation, a single cell included.
   *
   * @param range the range
   * @return the range
   * @throws IllegalArgumentException when the range can't be read
   */
  private static CellRangeAddress parseRange(String range) {
    try {
      CellRangeAddress area = CellRangeAddress.valueOf(range.replace("$", "").toUpperCase(Locale.ROOT));
      if (area.getFirstRow() < 0 || area.getFirstColumn() < 0) {
        throw new IllegalArgumentException(range);
      }
      return area;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("The range '%s' can't be read. Pass it in A1 notation, such as B2:F40 or B2.".formatted(range));
    }
  }

  /**
   * Maps a cell to its model.
   *
   * @param cell the cell, null for none
   * @param formatter the formatter giving the displayed text
   * @return the model, null for a missing or empty cell
   */
  private static CellModel toCellModel(Cell cell, DataFormatter formatter) {
    if (cell == null) {
      return null;
    }
    CellType type = cell.getCellType();
    String formula = null;
    if (type == CellType.FORMULA) {
      formula = "=" + cell.getCellFormula();
      type = cell.getCachedFormulaResultType();
    }
    Object value = switch (type) {
    case NUMERIC -> numericValue(cell);
    case STRING -> cell.getStringCellValue();
    case BOOLEAN -> cell.getBooleanCellValue();
    case ERROR -> FormulaError.forInt(cell.getErrorCellValue()).getString();
    default -> null;
    };
    if (formula == null && (value == null || (value instanceof String text && text.isEmpty()))) {
      return null;
    }
    String text = formatter.formatCellValue(cell);
    String ref = new CellReference(cell.getRowIndex(), cell.getColumnIndex()).formatAsString(false);
    return new CellModel(ref, value, StringUtils.equals(text, String.valueOf(value)) ? null : text, formula);
  }

  /**
   * Reads a numeric cell: a date as ISO-8601, a whole number as a long, else a
   * double.
   *
   * @param cell the cell
   * @return the value
   */
  private static Object numericValue(Cell cell) {
    if (DateUtil.isCellDateFormatted(cell)) {
      return cell.getLocalDateTimeCellValue().toString();
    }
    double number = cell.getNumericCellValue();
    return number == Math.rint(number) && Math.abs(number) < 1e15 ? (Object) (long) number : (Object) number;
  }

}
