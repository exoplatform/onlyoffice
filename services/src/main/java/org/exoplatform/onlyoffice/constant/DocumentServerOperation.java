/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.onlyoffice.constant;

/**
 * The purpose of a token exchanged with ONLYOFFICE Docs, carried in its root
 * {@code operation} claim. The Document Server refuses a token whose purpose
 * does not match the service it is sent to, and marks the tokens it sends
 * itself with {@link #CALLBACK} or {@link #DOWNLOAD}.
 */
public enum DocumentServerOperation {

  /** A request to the command service ({@code /command}). */
  COMMAND("command"),

  /** A request to the conversion service ({@code /converter}). */
  CONVERTER("converter"),

  /** A request to the Document Builder service ({@code /docbuilder}). */
  DOCBUILDER("docbuilder"),

  /** A save or status callback the Document Server posts to the addon. */
  CALLBACK("callback"),

  /** A file the Document Server downloads from the addon. */
  DOWNLOAD("download");

  /** The name of the claim that carries the purpose. */
  public static final String CLAIM = "operation";

  private final String claimValue;

  DocumentServerOperation(String claimValue) {
    this.claimValue = claimValue;
  }

  /**
   * @return the value of the {@code operation} claim for this purpose
   */
  public String getClaimValue() {
    return claimValue;
  }
}
