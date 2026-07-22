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
 * Result returned by the office document creation / conversion MCP tools: the
 * newly stored file's id and name (mirrors the shape returned by the Documents
 * add-on {@code DocumentMcpTool}), plus its path, in-app URL and mime type.
 */
@JsonInclude(Include.NON_EMPTY)
public class OfficeDocumentModel {

  @JsonProperty("document_id")
  private final String id;

  private final String name;

  private final String path;

  private final String url;

  @JsonProperty("parent_folder_id")
  private final String parentFolderId;

  @JsonProperty("mime_type")
  private final String mimeType;

  public OfficeDocumentModel(String id, String name, String path, String url, String parentFolderId, String mimeType) {
    this.id = id;
    this.name = name;
    this.path = path;
    this.url = url;
    this.parentFolderId = parentFolderId;
    this.mimeType = mimeType;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getUrl() {
    return url;
  }

  public String getParentFolderId() {
    return parentFolderId;
  }

  public String getMimeType() {
    return mimeType;
  }

}
