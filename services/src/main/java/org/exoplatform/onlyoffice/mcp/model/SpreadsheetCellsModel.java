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
package org.exoplatform.onlyoffice.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The cells of a range of one sheet, as {@code get_spreadsheet_cells} returns
 * them: every sheet of the workbook, then the non-empty cells of the range read,
 * each with its A1 position.
 *
 * @param id the spreadsheet id
 * @param name the spreadsheet title
 * @param sheet the name of the sheet read
 * @param range the range read, in A1 notation
 * @param sheets every sheet of the workbook, in order
 * @param cells the non-empty cells of the range, row by row
 * @param mergedRanges the merged ranges touching the range read
 * @param truncated true when the range holds more cells than one call returns
 * @param nextRange the range to read next when truncated, null otherwise
 */
@JsonInclude(Include.NON_EMPTY)
public record SpreadsheetCellsModel(@JsonProperty("document_id") String id,
                                    String name,
                                    String sheet,
                                    String range,
                                    List<SpreadsheetSheetModel> sheets,
                                    List<SpreadsheetCellModel> cells,
                                    @JsonProperty("merged_ranges") List<String> mergedRanges,
                                    boolean truncated,
                                    @JsonProperty("next_range") String nextRange) {

}
