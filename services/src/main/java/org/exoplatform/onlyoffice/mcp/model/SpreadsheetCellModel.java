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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * One non-empty cell.
 *
 * @param ref the cell position, in A1 notation
 * @param value the cell value: a number, a boolean, a text, a date as
 *          ISO-8601, or an error code; for a formula, its value when the file
 *          was last saved
 * @param text the value as the spreadsheet displays it, when it differs from
 *          the value
 * @param formula the cell formula, starting with '=', null for none
 */
@JsonInclude(Include.NON_NULL)
public record SpreadsheetCellModel(String ref, Object value, String text, String formula) {
}
