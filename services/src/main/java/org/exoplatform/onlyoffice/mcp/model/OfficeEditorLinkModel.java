/*
 * Copyright (C) 2003-2026 eXo Platform SAS.
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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result returned by {@code get_document_editor_link}: the ONLYOFFICE online
 * editor URL for a document, together with its id and name.
 */
@JsonInclude(Include.NON_EMPTY)
public class OfficeEditorLinkModel {

  @JsonProperty("document_id")
  private final String id;

  private final String name;

  @JsonProperty("editor_url")
  private final String editorUrl;

  public OfficeEditorLinkModel(String id, String name, String editorUrl) {
    this.id = id;
    this.name = name;
    this.editorUrl = editorUrl;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEditorUrl() {
    return editorUrl;
  }

}
