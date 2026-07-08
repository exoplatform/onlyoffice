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
package org.exoplatform.onlyoffice.mcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.documents.model.AbstractNode;
import org.exoplatform.documents.model.DocumentFolderFilter;
import org.exoplatform.documents.model.FileNode;
import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.onlyoffice.EditorLinkNotFoundException;
import org.exoplatform.onlyoffice.OfficeDocumentsThumbnailPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.mcp.model.OfficeDocumentModel;
import org.exoplatform.onlyoffice.mcp.model.OfficeEditorLinkModel;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.util.UploadToolUtils;

/**
 * MCP tools exposing the ONLYOFFICE editor add-on to the AI agent (EVA). The
 * core is a symmetric family of operations-based office tools that both CREATE
 * (no {@code document_id}) and MODIFY (a {@code document_id} is given, saved
 * back as a new version) native office files through the ONLYOFFICE Document
 * Server <b>Document Builder</b>:
 * <ul>
 * <li>{@code edit_spreadsheet} &rarr; xlsx</li>
 * <li>{@code edit_presentation} &rarr; pptx</li>
 * <li>{@code edit_document} &rarr; docx</li>
 * </ul>
 * plus {@code generate_document} (template + search/replace merge),
 * {@code convert_document} (format&rarr;format), {@code export_document_as_pdf}
 * and {@code get_document_editor_link}.
 * <p>
 * Every method acts as the current user: source / target JCR nodes are resolved
 * through a user {@link SessionProvider} (built from the user's ACL identity),
 * so document ACLs are enforced. The Document Builder script and its assets are
 * run through the shared, security-critical
 * {@link OnlyofficeEditorService#runDocBuilderScript} primitive (see its javadoc
 * for the trust model). A {@code null}/empty Document Server result is always
 * translated into an LLM-directed {@link IllegalStateException}.
 * <p>
 * <b>operations argument.</b> The nested operations array is accepted as a JSON
 * <i>string</i> (a JSON array of {@code {"type":...}} objects) because the MCP
 * argument binder does not reliably bind a nested array; it is parsed
 * server-side.
 */
@Service
@Profile("mcp-server")
public class OnlyofficeMcpTool implements McpToolPlugin {

  private static final Log         LOG                = ExoLogger.getExoLogger(OnlyofficeMcpTool.class);

  /** In-app preview URL of a stored file (same shape as the Documents add-on). */
  public static final String       FILE_URL_FORMAT    = "/portal/dw/documents?documentPreviewId=%s";

  /** EMU (English Metric Units) per point, used by the Document Builder shape/chart geometry. */
  private static final int         EMU                = 36000;

  /** Max bytes fetched for an image referenced by a URL. */
  private static final long        MAX_IMAGE_BYTES    = 15L * 1024 * 1024;

  private static final String      CONVERT_FAILED_MSG =
                                                      "The Document Server could not convert this file. "
                                                          + "Verify the ONLYOFFICE Document Server is installed and running, "
                                                          + "and that the source format is supported for this conversion.";

  private static final int         IMPORT_POLL_MAX_ATTEMPTS = 20;

  private static final long        IMPORT_POLL_INTERVAL_MS  = 500L;

  private static final int         IMPORT_POLL_PAGE_SIZE    = 200;

  private final OnlyofficeEditorService onlyofficeEditorService;

  private final DocumentFileService     documentFileService;

  private final IdentityManager         identityManager;

  private final UserACL                 userAcl;

  private final RepositoryService       repositoryService;

  private final UploadService           uploadService;

  private final AttachmentService       attachmentService;

  private final FileService             fileService;

  public OnlyofficeMcpTool(OnlyofficeEditorService onlyofficeEditorService,
                           DocumentFileService documentFileService,
                           IdentityManager identityManager,
                           UserACL userAcl,
                           RepositoryService repositoryService,
                           UploadService uploadService,
                           AttachmentService attachmentService,
                           FileService fileService) {
    this.onlyofficeEditorService = onlyofficeEditorService;
    this.documentFileService = documentFileService;
    this.identityManager = identityManager;
    this.userAcl = userAcl;
    this.repositoryService = repositoryService;
    this.uploadService = uploadService;
    this.attachmentService = attachmentService;
    this.fileService = fileService;
  }

  // ---------------------------------------------------------------------------
  // edit_spreadsheet / edit_presentation / edit_document
  // ---------------------------------------------------------------------------

  /**
   * Creates or modifies a spreadsheet (xlsx). Pass {@code document_id} to MODIFY
   * an existing spreadsheet (saved back as a new version), or
   * {@code parent_folder_id}
   * and {@code name} to CREATE a new one. The {@code operations} are applied via
   * the ONLYOFFICE Document Builder.
   * <p>
   * Supported operations (a JSON array string):
   * <ul>
   * <li>{@code {"type":"set_cells","start":"A1","values":[["Region","Sales"],["North",120]]}}</li>
   * <li>{@code {"type":"set_formula","cell":"B4","formula":"=SUM(B2:B3)"}}</li>
   * <li>{@code {"type":"add_chart","chart":"bar|line|pie","range":"Sheet1!$A$1:$B$3","title":"..."}}</li>
   * <li>{@code {"type":"add_sheet","name":"Q3"}} (subsequent ops target the new sheet)</li>
   * </ul>
   *
   * @param documentId optional JCR uuid of an existing spreadsheet to modify
   * @param parentFolderId folder id where a new spreadsheet is created (create mode)
   * @param name new spreadsheet name WITHOUT extension (create mode)
   * @param operations a JSON array string of spreadsheet operations
   * @return the created / modified document (id + name)
   */
  public OfficeDocumentModel editSpreadsheet(String documentId,
                                             String parentFolderId,
                                             String name,
                                             String operations) throws IllegalAccessException, ObjectNotFoundException {
    // Spreadsheets have no add_image operation, so no image-attachment source is plumbed.
    return runEdit("xlsx", documentId, parentFolderId, name, operations, null, null);
  }

  /**
   * Creates or modifies a presentation (pptx). Pass {@code document_id} to MODIFY
   * (saved back as a new version) or {@code parent_folder_id} + {@code name} to
   * CREATE.
   * <p>
   * Supported operations (a JSON array string):
   * <ul>
   * <li>{@code {"type":"add_slide","layout":"title|content|closing","title":"...","bullets":["a","b"]}}</li>
   * <li>{@code {"type":"set_slide","index":0,"title":"...","bullets":[...]}} (replaces the slide content)</li>
   * <li>{@code {"type":"add_image","slide":1,"url":"https://.../pic.png"}} or {@code {"type":"add_image","slide":1,"document_id":"<uuid>"}}</li>
   * </ul>
   * <p>
   * When the user attaches an image in the EVA chat, the agent injects the
   * chat-attachment reference into the top-level {@code attachmentObjectType} /
   * {@code attachmentObjectId} parameters; any {@code add_image} operation that
   * has neither {@code url} nor {@code document_id} then resolves its bytes from
   * that attachment (see {@link #resolveImageBytes}).
   *
   * @param documentId optional JCR uuid of an existing presentation to modify
   * @param parentFolderId folder id where a new presentation is created (create mode)
   * @param name new presentation name WITHOUT extension (create mode)
   * @param operations a JSON array string of presentation operations
   * @param attachmentObjectType object type of the EVA chat-attachment image source (auto-filled by the agent)
   * @param attachmentObjectId object id of the EVA chat-attachment image source (auto-filled by the agent)
   * @return the created / modified document (id + name)
   */
  public OfficeDocumentModel editPresentation(String documentId,
                                              String parentFolderId,
                                              String name,
                                              String operations,
                                              String attachmentObjectType,
                                              String attachmentObjectId) throws IllegalAccessException,
                                                                         ObjectNotFoundException {
    return runEdit("pptx", documentId, parentFolderId, name, operations, attachmentObjectType, attachmentObjectId);
  }

  /**
   * Creates or modifies a text document (docx). Pass {@code document_id} to
   * MODIFY (saved back as a new version) or {@code parent_folder_id} +
   * {@code name} to CREATE.
   * <p>
   * Supported operations (a JSON array string):
   * <ul>
   * <li>{@code {"type":"add_heading","level":1,"text":"..."}}</li>
   * <li>{@code {"type":"add_paragraph","text":"...","bold":false,"italic":false}}</li>
   * <li>{@code {"type":"add_table","rows":[["A","B"],["1","2"]]}}</li>
   * <li>{@code {"type":"add_image","url":"https://.../pic.png"}} or {@code {"type":"add_image","document_id":"<uuid>"}}</li>
   * <li>{@code {"type":"replace_text","search":"{{name}}","replace":"ACME"}}</li>
   * <li>{@code {"type":"set_html","html":"<h1>..</h1>"}} (CREATE only, must be the
   * only operation; authored via the HTML conversion path, not the Builder)</li>
   * </ul>
   * <p>
   * When the user attaches an image in the EVA chat, the agent injects the
   * chat-attachment reference into the top-level {@code attachmentObjectType} /
   * {@code attachmentObjectId} parameters; any {@code add_image} operation that
   * has neither {@code url} nor {@code document_id} then resolves its bytes from
   * that attachment (see {@link #resolveImageBytes}).
   *
   * @param documentId optional JCR uuid of an existing document to modify
   * @param parentFolderId folder id where a new document is created (create mode)
   * @param name new document name WITHOUT extension (create mode)
   * @param operations a JSON array string of document operations
   * @param attachmentObjectType object type of the EVA chat-attachment image source (auto-filled by the agent)
   * @param attachmentObjectId object id of the EVA chat-attachment image source (auto-filled by the agent)
   * @return the created / modified document (id + name)
   */
  public OfficeDocumentModel editDocument(String documentId,
                                          String parentFolderId,
                                          String name,
                                          String operations,
                                          String attachmentObjectType,
                                          String attachmentObjectId) throws IllegalAccessException, ObjectNotFoundException {
    // set_html is authored through the HTML->docx conversion API, not the
    // Builder. It is only supported as the sole operation of a CREATE call.
    JSONArray ops = parseOperations(operations);
    if (containsHtmlOp(ops)) {
      return createDocumentFromHtml(documentId, parentFolderId, name, ops);
    }
    return runEdit("docx", documentId, parentFolderId, name, operations, attachmentObjectType, attachmentObjectId);
  }

  /**
   * Shared CREATE-or-MODIFY orchestration for the three structured office tools:
   * builds the format-specific Document Builder script (CreateFile or OpenFile,
   * apply operations, SaveFile), gathers the referenced assets, runs the shared
   * {@code runDocBuilderScript} primitive, then stores a new file (create) or a
   * new version of the source file (modify).
   */
  private OfficeDocumentModel runEdit(String format,
                                      String documentId,
                                      String parentFolderId,
                                      String name,
                                      String operations,
                                      String attachmentObjectType,
                                      String attachmentObjectId) throws IllegalAccessException, ObjectNotFoundException {
    boolean modify = StringUtils.isNotBlank(documentId);
    if (!modify) {
      if (StringUtils.isBlank(parentFolderId)) {
        throw new IllegalArgumentException("Provide either 'document_id' to modify an existing file, or 'parent_folder_id' (and "
            + "'name') to create a new one. Call get_root_folder_for_user / get_root_folder_by_space / "
            + "get_documents_by_folder_id to obtain a folder id.");
      }
      if (StringUtils.isBlank(name)) {
        throw new IllegalArgumentException("The 'name' parameter is mandatory when creating a new document.");
      }
    }
    JSONArray ops = parseOperations(operations);

    UserSession userSession = openUserSession();
    try {
      Map<String, byte[]> assets = new LinkedHashMap<>();
      StringBuilder script = new StringBuilder();
      Node target = null;
      if (modify) {
        target = resolveNode(userSession.session(), documentId);
        assets.put("input", readNodeBytes(target, documentId));
        script.append("builder.OpenFile(\"@@ASSET:input@@\", \"\");\n");
      } else {
        script.append("builder.CreateFile(\"").append(format).append("\");\n");
      }

      switch (format) {
        case "xlsx":
          appendSpreadsheetOps(ops, script);
          break;
        case "pptx":
          appendPresentationOps(ops, script, assets, userSession.session(), !modify, attachmentObjectType, attachmentObjectId);
          break;
        default:
          appendDocumentOps(ops, script, assets, userSession.session(), attachmentObjectType, attachmentObjectId);
          break;
      }

      String outputName = "result." + format;
      script.append("builder.SaveFile(\"").append(format).append("\", \"").append(outputName).append("\");\n");
      script.append("builder.CloseFile();\n");

      byte[] out = onlyofficeEditorService.runDocBuilderScript(script.toString(),
                                                               assets,
                                                               format,
                                                               outputName,
                                                               getCurrentUserName());
      if (out == null || out.length == 0) {
        throw new IllegalStateException(CONVERT_FAILED_MSG);
      }
      if (modify) {
        try {
          onlyofficeEditorService.saveNewVersion(target, out, getCurrentUserName());
        } catch (RepositoryException e) {
          throw new IllegalStateException("Could not save the modified document %s as a new version: %s".formatted(documentId,
                                                                                                                   e.getMessage()));
        }
        return toModelFromNode(target, documentId);
      }
      return importContent(parentFolderId, appendExtension(name, format), out);
    } finally {
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // generate_document (template + search/replace merge)
  // ---------------------------------------------------------------------------

  /**
   * Generates a new document from a template by replacing every placeholder with
   * its value (a search/replace merge run through the Document Builder). Unlike
   * {@code edit_document}'s {@code replace_text} (which edits a document in place
   * for one-off substitutions), this is the template-fill workflow: it always
   * CREATES a new file from a template document and takes the whole placeholder
   * map at once.
   *
   * @param templateDocumentId the JCR uuid of the template document (e.g. a docx
   *          with {@code {{client}}} placeholders)
   * @param folderId optional destination folder; defaults to the template's folder
   * @param name the produced document name WITHOUT extension; defaults to the
   *          template name
   * @param data a JSON object string mapping each placeholder to its replacement,
   *          e.g. {@code {"{{client}}":"ACME","{{amount}}":"42,000 EUR"}}
   * @return the generated document (id + name)
   */
  public OfficeDocumentModel generateDocument(String templateDocumentId,
                                              String folderId,
                                              String name,
                                              String data) throws IllegalAccessException, ObjectNotFoundException {
    if (StringUtils.isBlank(templateDocumentId)) {
      throw new IllegalArgumentException("The 'template_document_id' parameter is mandatory (the JCR uuid of the template).");
    }
    JSONObject dataObject = parseObject(data, "data");
    if (dataObject.length() == 0) {
      throw new IllegalArgumentException("The 'data' parameter must be a non-empty JSON object mapping each placeholder to "
          + "its replacement value, e.g. {\"{{client}}\":\"ACME\"}.");
    }
    UserSession userSession = openUserSession();
    try {
      Node template = resolveNode(userSession.session(), templateDocumentId);
      Map<String, byte[]> assets = new LinkedHashMap<>();
      assets.put("input", readNodeBytes(template, templateDocumentId));

      StringBuilder script = new StringBuilder();
      script.append("builder.OpenFile(\"@@ASSET:input@@\", \"\");\n");
      script.append("var oDocument = Api.GetDocument();\n");
      for (String search : dataObject.keySet()) {
        String replace = String.valueOf(dataObject.get(search));
        script.append("oDocument.SearchAndReplace({\"searchString\":")
              .append(jsString(search))
              .append(", \"replaceString\":")
              .append(jsString(replace))
              .append("});\n");
      }
      script.append("builder.SaveFile(\"docx\", \"result.docx\");\n");
      script.append("builder.CloseFile();\n");

      byte[] out = onlyofficeEditorService.runDocBuilderScript(script.toString(),
                                                               assets,
                                                               "docx",
                                                               "result.docx",
                                                               getCurrentUserName());
      if (out == null || out.length == 0) {
        throw new IllegalStateException(CONVERT_FAILED_MSG);
      }
      String baseName = StringUtils.isNotBlank(name) ? name : stripExtension(nodeTitleOrName(template));
      String targetFolder = StringUtils.isNotBlank(folderId) ? folderId : parentFolderIdOf(template, templateDocumentId);
      return importContent(targetFolder, appendExtension(baseName, "docx"), out);
    } finally {
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // convert_document (format -> format)
  // ---------------------------------------------------------------------------

  /**
   * Converts an existing office document to another format using the ONLYOFFICE
   * Document Server, storing the result as {@code <originalName>.<target_format>}.
   *
   * @param documentId the JCR uuid of the source document
   * @param targetFormat the produced format, e.g. docx, xlsx, pptx, odt, pdf, csv
   * @param folderId optional destination folder; defaults to the source folder
   * @return the created document (id + name)
   */
  public OfficeDocumentModel convertDocument(String documentId,
                                             String targetFormat,
                                             String folderId) throws IllegalAccessException, ObjectNotFoundException {
    if (StringUtils.isBlank(documentId)) {
      throw new IllegalArgumentException("The 'document_id' parameter is mandatory (the JCR uuid of the document to convert).");
    }
    if (StringUtils.isBlank(targetFormat)) {
      throw new IllegalArgumentException("The 'target_format' parameter is mandatory, e.g. docx, xlsx, pptx, odt, pdf, csv.");
    }
    String target = normalizeExtension(targetFormat);
    UserSession userSession = openUserSession();
    try {
      Node node = resolveNode(userSession.session(), documentId);
      String baseName = stripExtension(nodeTitleOrName(node));
      String originalType = extensionOf(nodeTitleOrName(node));
      String targetFolderId = StringUtils.isNotBlank(folderId) ? folderId : parentFolderIdOf(node, documentId);
      byte[] converted = onlyofficeEditorService.convertNodeContent(node, target, originalType, getCurrentUserName());
      if (converted == null || converted.length == 0) {
        throw new IllegalStateException(CONVERT_FAILED_MSG);
      }
      return importContent(targetFolderId, baseName + "." + target, converted);
    } finally {
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // export_document_as_pdf
  // ---------------------------------------------------------------------------

  /**
   * Exports an existing office document as a PDF, stored as {@code <name>.pdf}.
   *
   * @param documentId the JCR uuid of the source document
   * @param parentFolderId optional destination folder; defaults to the source folder
   * @return the created PDF document (id + name)
   */
  public OfficeDocumentModel exportDocumentAsPdf(String documentId,
                                                 String parentFolderId) throws IllegalAccessException,
                                                                        ObjectNotFoundException {
    if (StringUtils.isBlank(documentId)) {
      throw new IllegalArgumentException("The 'document_id' parameter is mandatory (the JCR uuid of the document to export).");
    }
    UserSession userSession = openUserSession();
    try {
      Node node = resolveNode(userSession.session(), documentId);
      String baseName = stripExtension(nodeTitleOrName(node));
      String targetFolderId = StringUtils.isNotBlank(parentFolderId) ? parentFolderId : parentFolderIdOf(node, documentId);
      byte[] pdf;
      try {
        pdf = onlyofficeEditorService.convertNodeContentToPdf(node, getCurrentUserName());
      } catch (RepositoryException e) {
        throw new IllegalStateException("Could not export the document %s as PDF: %s".formatted(documentId, e.getMessage()));
      }
      if (pdf == null || pdf.length == 0) {
        throw new IllegalStateException(CONVERT_FAILED_MSG);
      }
      return importContent(targetFolderId, baseName + ".pdf", pdf);
    } finally {
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // get_document_editor_link
  // ---------------------------------------------------------------------------

  /**
   * Returns the ONLYOFFICE online editor URL of a document, so it can be opened
   * for editing. The user must have edit permission on the document.
   *
   * @param documentId the JCR uuid of the document
   * @return the document id, name and editor URL
   */
  public OfficeEditorLinkModel getDocumentEditorLink(String documentId) throws IllegalAccessException,
                                                                        ObjectNotFoundException {
    if (StringUtils.isBlank(documentId)) {
      throw new IllegalArgumentException("The 'document_id' parameter is mandatory (the JCR uuid of the document).");
    }
    UserSession userSession = openUserSession();
    try {
      Node node = resolveNode(userSession.session(), documentId);
      String baseUrl = System.getProperty("exo.base.url");
      if (StringUtils.isBlank(baseUrl)) {
        throw new IllegalStateException("The platform base URL (exo.base.url) is not configured, so no editor link can be built.");
      }
      URI uri = URI.create(baseUrl);
      String link;
      try {
        link = onlyofficeEditorService.getEditorLink(node, uri.getScheme(), uri.getHost(), uri.getPort());
      } catch (EditorLinkNotFoundException e) {
        throw new IllegalAccessException(("The document %s cannot be opened in the online editor: it is not an editable office "
            + "document, or you do not have edit permission on it.").formatted(documentId));
      } catch (RepositoryException e) {
        throw new IllegalStateException("Could not build the editor link for document %s: %s".formatted(documentId, e.getMessage()));
      }
      return new OfficeEditorLinkModel(documentId, nodeTitleOrName(node), link);
    } finally {
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // Document Builder script generators (lean, per format)
  // ---------------------------------------------------------------------------

  private void appendSpreadsheetOps(JSONArray ops, StringBuilder s) {
    s.append("var oWorksheet = Api.GetActiveSheet();\n");
    boolean sheetAdded = false;
    for (int i = 0; i < ops.length(); i++) {
      JSONObject op = ops.getJSONObject(i);
      String type = opType(op);
      switch (type) {
        case "set_cells": {
          String start = op.optString("start", "A1");
          int[] rc = cellToRowCol(start);
          JSONArray rows = op.optJSONArray("values");
          if (rows == null) {
            throw new IllegalArgumentException("set_cells requires a 'values' 2D array, e.g. [[\"A\",\"B\"],[1,2]].");
          }
          for (int r = 0; r < rows.length(); r++) {
            JSONArray cols = rows.optJSONArray(r);
            if (cols == null) {
              continue;
            }
            for (int c = 0; c < cols.length(); c++) {
              Object value = cols.get(c);
              s.append("oWorksheet.GetRangeByNumber(")
               .append(rc[0] + r)
               .append(", ")
               .append(rc[1] + c)
               .append(").SetValue(")
               .append(jsValue(value))
               .append(");\n");
            }
          }
          break;
        }
        case "set_formula": {
          String cell = requireString(op, "cell", "set_formula");
          String formula = requireString(op, "formula", "set_formula");
          s.append("oWorksheet.GetRange(").append(jsString(cell)).append(").SetValue(").append(jsString(formula)).append(");\n");
          break;
        }
        case "add_chart": {
          String chartType = normalizeChartType(op.optString("chart", op.optString("type_chart", "bar")));
          String range = requireString(op, "range", "add_chart");
          String title = op.optString("title", "");
          s.append("var oChart = oWorksheet.AddChart(")
           .append(jsString(range))
           .append(", true, ")
           .append(jsString(chartType))
           .append(", 2, ")
           .append(100 * EMU)
           .append(", ")
           .append(70 * EMU)
           .append(", 3, 0, 8, 0);\n");
          if (StringUtils.isNotBlank(title)) {
            s.append("oChart.SetTitle(").append(jsString(title)).append(", 14);\n");
          }
          break;
        }
        case "add_sheet": {
          String sheetName = requireString(op, "name", "add_sheet");
          s.append("Api.AddSheet(").append(jsString(sheetName)).append(");\n");
          s.append("oWorksheet = Api.GetActiveSheet();\n");
          sheetAdded = true;
          break;
        }
        default:
          throw unsupportedOp(type, "edit_spreadsheet");
      }
    }
    // ONLYOFFICE xlsx->PDF conversion (convert_document / export_document_as_pdf via the
    // Document Server /converter) exports ONLY the ACTIVE sheet. Api.AddSheet leaves the
    // newly-added (usually empty) sheet active, so exporting such a spreadsheet produced a
    // blank ~2KB PDF. Re-activating the first (data) sheet before SaveFile makes the primary
    // sheet the one exported. Verified against the live Document Server: with the empty added
    // sheet left active the PDF was 1936 bytes (blank); re-activating Sheet1 yielded a 26KB PDF
    // with the full table + chart. (SetPrintArea is NOT available on the builder Api here — it
    // fails with docbuilder error -3 — so activating the data sheet is the working fix.)
    if (sheetAdded) {
      s.append("Api.GetSheets()[0].SetActive();\n");
      s.append("oWorksheet = Api.GetActiveSheet();\n");
    }
  }

  private void appendPresentationOps(JSONArray ops,
                                     StringBuilder s,
                                     Map<String, byte[]> assets,
                                     Session session,
                                     boolean create,
                                     String attachmentObjectType,
                                     String attachmentObjectId)
                                                                                                                  throws IllegalAccessException,
                                                                                                                  ObjectNotFoundException {
    s.append("var oPresentation = Api.GetPresentation();\n");
    s.append("var __fill = Api.CreateSolidFill(Api.CreateRGBColor(255, 255, 255));\n");
    s.append("var __stroke = Api.CreateStroke(0, Api.CreateNoFill());\n");
    // Explicit BLACK text fill. Api.CreateShape gives the shape a default style whose
    // fontRef is the "lt1" (light-1 = white) theme colour, meant for a colour-filled
    // shape. Because we override the shape fill to white, a run WITHOUT an explicit
    // colour renders as white-on-white and is invisible (the slides looked empty even
    // though the text was present in the XML). Every run below sets this fill so the
    // title + bullets are actually visible. Verified against the live Document Server:
    // without SetFill the pptx->PDF page was blank; with it the text renders in black.
    s.append("var __textFill = Api.CreateSolidFill(Api.CreateRGBColor(0, 0, 0));\n");
    // Helper: (re)build a slide's text with a title + bullet lines.
    s.append("function __setSlideText(oSlide, title, bullets) {\n")
     .append("  oSlide.RemoveAllObjects();\n")
     .append("  if (title) {\n")
     .append("    var t = Api.CreateShape(\"rect\", 300*").append(EMU).append(", 50*").append(EMU).append(", __fill, __stroke);\n")
     .append("    t.SetPosition(20*").append(EMU).append(", 20*").append(EMU).append(");\n")
     .append("    var tp = t.GetContent().GetElement(0);\n")
     .append("    var tr = Api.CreateRun(); tr.AddText(title); tr.SetBold(true); tr.SetFontSize(32); tr.SetFill(__textFill); tp.AddElement(tr);\n")
     .append("    oSlide.AddObject(t);\n")
     .append("  }\n")
     .append("  if (bullets && bullets.length) {\n")
     .append("    var b = Api.CreateShape(\"rect\", 500*").append(EMU).append(", 300*").append(EMU).append(", __fill, __stroke);\n")
     .append("    b.SetPosition(20*").append(EMU).append(", 90*").append(EMU).append(");\n")
     .append("    var doc = b.GetContent();\n")
     .append("    for (var i = 0; i < bullets.length; i++) {\n")
     .append("      var p = (i === 0) ? doc.GetElement(0) : Api.CreateParagraph();\n")
     .append("      if (i !== 0) doc.Push(p);\n")
     .append("      var r = Api.CreateRun(); r.AddText(\"\\u2022 \" + bullets[i]); r.SetFontSize(18); r.SetFill(__textFill); p.AddElement(r);\n")
     .append("    }\n")
     .append("    oSlide.AddObject(b);\n")
     .append("  }\n")
     .append("}\n");

    int imageIndex = 0;
    // On CREATE, builder.CreateFile("pptx") starts the presentation with one blank
    // default slide (index 0). The first add_slide must POPULATE that default slide
    // instead of appending a new one, otherwise every created presentation opens on a
    // leading blank slide. Consumed by the first add_slide (or cancelled by a set_slide,
    // which already targets an existing slide, so a later add_slide appends normally).
    boolean reuseDefaultSlide = create;
    for (int i = 0; i < ops.length(); i++) {
      JSONObject op = ops.getJSONObject(i);
      String type = opType(op);
      switch (type) {
        case "add_slide": {
          if (reuseDefaultSlide) {
            s.append("var oSlide = oPresentation.GetSlideByIndex(0);\n");
            reuseDefaultSlide = false;
          } else {
            s.append("var oSlide = Api.CreateSlide(); oPresentation.AddSlide(oSlide);\n");
          }
          s.append("__setSlideText(oSlide, ")
           .append(jsString(op.optString("title", "")))
           .append(", ")
           .append(jsStringArray(op.optJSONArray("bullets")))
           .append(");\n");
          break;
        }
        case "set_slide": {
          reuseDefaultSlide = false;
          int index = op.optInt("index", 0);
          s.append("var oSlide = oPresentation.GetSlideByIndex(").append(index).append(");\n");
          s.append("if (oSlide) __setSlideText(oSlide, ")
           .append(jsString(op.optString("title", "")))
           .append(", ")
           .append(jsStringArray(op.optJSONArray("bullets")))
           .append(");\n");
          break;
        }
        case "add_image": {
          int index = op.optInt("slide", op.optInt("index", 0));
          String assetName = "img" + (imageIndex++);
          assets.put(assetName + ".png", resolveImageBytes(op, session, attachmentObjectType, attachmentObjectId));
          s.append("var oImgSlide = oPresentation.GetSlideByIndex(").append(index).append(");\n");
          s.append("if (oImgSlide) { var oImage = Api.CreateImage(\"@@ASSET:")
           .append(assetName)
           .append(".png@@\", 120*").append(EMU).append(", 90*").append(EMU).append("); oImage.SetPosition(30*")
           .append(EMU).append(", 100*").append(EMU).append("); oImgSlide.AddObject(oImage); }\n");
          break;
        }
        default:
          throw unsupportedOp(type, "edit_presentation");
      }
    }
  }

  private void appendDocumentOps(JSONArray ops,
                                 StringBuilder s,
                                 Map<String, byte[]> assets,
                                 Session session,
                                 String attachmentObjectType,
                                 String attachmentObjectId) throws IllegalAccessException, ObjectNotFoundException {
    s.append("var oDocument = Api.GetDocument();\n");
    int imageIndex = 0;
    for (int i = 0; i < ops.length(); i++) {
      JSONObject op = ops.getJSONObject(i);
      String type = opType(op);
      switch (type) {
        case "add_heading": {
          int level = clamp(op.optInt("level", 1), 1, 6);
          String text = requireString(op, "text", "add_heading");
          int size = 32 - (level - 1) * 4;
          s.append("var oP = Api.CreateParagraph(); var oR = Api.CreateRun(); oR.AddText(")
           .append(jsString(text))
           .append("); oR.SetBold(true); oR.SetFontSize(")
           .append(Math.max(size, 16))
           .append("); oP.AddElement(oR); oDocument.Push(oP);\n");
          break;
        }
        case "add_paragraph": {
          String text = requireString(op, "text", "add_paragraph");
          boolean bold = op.optBoolean("bold", false);
          boolean italic = op.optBoolean("italic", false);
          s.append("var oP = Api.CreateParagraph(); var oR = Api.CreateRun(); oR.AddText(").append(jsString(text)).append(");");
          if (bold) {
            s.append(" oR.SetBold(true);");
          }
          if (italic) {
            s.append(" oR.SetItalic(true);");
          }
          s.append(" oP.AddElement(oR); oDocument.Push(oP);\n");
          break;
        }
        case "add_table": {
          JSONArray rows = op.optJSONArray("rows");
          if (rows == null || rows.length() == 0) {
            throw new IllegalArgumentException("add_table requires a non-empty 'rows' 2D array.");
          }
          int rowCount = rows.length();
          int colCount = 0;
          for (int r = 0; r < rowCount; r++) {
            JSONArray cols = rows.optJSONArray(r);
            if (cols != null) {
              colCount = Math.max(colCount, cols.length());
            }
          }
          if (colCount == 0) {
            throw new IllegalArgumentException("add_table rows must contain at least one column.");
          }
          // ONLYOFFICE Document Builder's Api.CreateTable is (nRows, nCols) — rows first.
          // Emitting (colCount, rowCount) built a table with too few rows, so GetRow(r) for
          // the later rows threw "Row index out of bounds" (docbuilder ExitCode error:-80).
          // Verified against the live Document Server: CreateTable(rowCount, colCount) is correct.
          s.append("var oTable = Api.CreateTable(").append(rowCount).append(", ").append(colCount).append(");\n");
          s.append("oTable.SetWidth(\"percent\", 100);\n");
          for (int r = 0; r < rowCount; r++) {
            JSONArray cols = rows.optJSONArray(r);
            if (cols == null) {
              continue;
            }
            for (int c = 0; c < cols.length() && c < colCount; c++) {
              s.append("oTable.GetRow(").append(r).append(").GetCell(").append(c)
               .append(").GetContent().GetElement(0).AddText(").append(jsString(String.valueOf(cols.get(c)))).append(");\n");
            }
          }
          s.append("oDocument.Push(oTable);\n");
          break;
        }
        case "add_image": {
          String assetName = "img" + (imageIndex++);
          assets.put(assetName + ".png", resolveImageBytes(op, session, attachmentObjectType, attachmentObjectId));
          s.append("var oImg = Api.CreateImage(\"@@ASSET:")
           .append(assetName)
           .append(".png@@\", 120*").append(EMU).append(", 90*").append(EMU).append(");\n");
          s.append("var oImgP = Api.CreateParagraph(); oImgP.AddDrawing(oImg); oDocument.Push(oImgP);\n");
          break;
        }
        case "replace_text": {
          String search = requireString(op, "search", "replace_text");
          String replace = op.optString("replace", "");
          s.append("oDocument.SearchAndReplace({\"searchString\":")
           .append(jsString(search))
           .append(", \"replaceString\":")
           .append(jsString(replace))
           .append("});\n");
          break;
        }
        default:
          throw unsupportedOp(type, "edit_document");
      }
    }
  }

  // ---------------------------------------------------------------------------
  // HTML -> docx (set_html), preserved from the former create_office_document
  // ---------------------------------------------------------------------------

  private OfficeDocumentModel createDocumentFromHtml(String documentId,
                                                     String parentFolderId,
                                                     String name,
                                                     JSONArray ops) throws IllegalAccessException, ObjectNotFoundException {
    if (StringUtils.isNotBlank(documentId)) {
      throw new IllegalArgumentException("The 'set_html' operation authors a brand-new document from HTML and cannot modify an "
          + "existing one. Omit 'document_id', or use structured operations (add_heading / add_paragraph / ...) to edit a document.");
    }
    if (ops.length() != 1) {
      throw new IllegalArgumentException("The 'set_html' operation cannot be combined with other operations in one call. "
          + "Send it alone to author a document from HTML, or use only structured operations.");
    }
    if (StringUtils.isBlank(parentFolderId)) {
      throw new IllegalArgumentException("The 'parent_folder_id' parameter is mandatory to create a document from HTML.");
    }
    if (StringUtils.isBlank(name)) {
      throw new IllegalArgumentException("The 'name' parameter is mandatory to create a document from HTML.");
    }
    String html = ops.getJSONObject(0).optString("html", "");
    if (StringUtils.isBlank(html)) {
      throw new IllegalArgumentException("The 'set_html' operation requires a non-empty 'html' value.");
    }
    UserSession userSession = openUserSession();
    Node tmpNode = null;
    try {
      Node parent = resolveNode(userSession.session(), parentFolderId);
      byte[] converted;
      try {
        tmpNode = createTempHtmlNode(parent, html, userSession.session());
        converted = onlyofficeEditorService.convertNodeContent(tmpNode, "docx", "html", getCurrentUserName());
      } catch (RepositoryException e) {
        throw new IllegalStateException("Could not stage the HTML content for conversion: " + e.getMessage());
      }
      if (converted == null || converted.length == 0) {
        throw new IllegalStateException(CONVERT_FAILED_MSG);
      }
      return importContent(parentFolderId, appendExtension(name, "docx"), converted);
    } finally {
      deleteQuietly(tmpNode);
      userSession.close();
    }
  }

  // ---------------------------------------------------------------------------
  // Operations parsing / JS emission helpers
  // ---------------------------------------------------------------------------

  private JSONArray parseOperations(String operations) {
    if (StringUtils.isBlank(operations)) {
      throw new IllegalArgumentException("The 'operations' parameter is mandatory: a JSON array string of operations, e.g. "
          + "[{\"type\":\"add_paragraph\",\"text\":\"Hello\"}].");
    }
    String trimmed = operations.trim();
    try {
      if (trimmed.startsWith("[")) {
        return new JSONArray(trimmed);
      }
      if (trimmed.startsWith("{")) {
        JSONObject obj = new JSONObject(trimmed);
        if (obj.has("operations")) {
          return obj.getJSONArray("operations");
        }
        JSONArray single = new JSONArray();
        single.put(obj);
        return single;
      }
    } catch (JSONException e) {
      throw new IllegalArgumentException("The 'operations' parameter is not valid JSON: " + e.getMessage());
    }
    throw new IllegalArgumentException("The 'operations' parameter must be a JSON array (or object) string.");
  }

  private JSONObject parseObject(String value, String paramName) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("The '%s' parameter is mandatory: a JSON object string.".formatted(paramName));
    }
    try {
      return new JSONObject(value.trim());
    } catch (JSONException e) {
      throw new IllegalArgumentException("The '%s' parameter is not a valid JSON object: %s".formatted(paramName, e.getMessage()));
    }
  }

  private boolean containsHtmlOp(JSONArray ops) {
    for (int i = 0; i < ops.length(); i++) {
      String type = ops.getJSONObject(i).optString("type", "");
      if ("set_html".equals(type) || "append_html".equals(type)) {
        return true;
      }
    }
    return false;
  }

  private static String opType(JSONObject op) {
    String type = op.optString("type", "");
    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("Every operation must have a 'type'. Got: " + op);
    }
    return type;
  }

  private static IllegalArgumentException unsupportedOp(String type, String tool) {
    return new IllegalArgumentException("Unsupported operation '%s' for %s.".formatted(type, tool));
  }

  private static String requireString(JSONObject op, String field, String opName) {
    String value = op.optString(field, "");
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("The '%s' operation requires a '%s' value.".formatted(opName, field));
    }
    return value;
  }

  /** Emits a JS string literal, escaping the value safely. */
  private static String jsString(String value) {
    String v = value == null ? "" : value;
    StringBuilder sb = new StringBuilder(v.length() + 2);
    sb.append('"');
    for (int i = 0; i < v.length(); i++) {
      char c = v.charAt(i);
      switch (c) {
        case '\\':
          sb.append("\\\\");
          break;
        case '"':
          sb.append("\\\"");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '<':
          sb.append("\\u003c");
          break;
        case '>':
          sb.append("\\u003e");
          break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
    return sb.toString();
  }

  /** Emits a numeric literal for numbers, otherwise a JS string literal. */
  private static String jsValue(Object value) {
    if (value instanceof Number) {
      return value.toString();
    }
    if (value instanceof Boolean) {
      return value.toString();
    }
    return jsString(String.valueOf(value));
  }

  private static String jsStringArray(JSONArray array) {
    if (array == null || array.length() == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < array.length(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(jsString(String.valueOf(array.get(i))));
    }
    sb.append(']');
    return sb.toString();
  }

  private static String normalizeChartType(String chart) {
    String c = StringUtils.lowerCase(StringUtils.trimToEmpty(chart));
    switch (c) {
      case "line":
        return "line";
      case "pie":
        return "pie";
      case "column":
      case "bar":
      case "":
        return "bar";
      default:
        return c;
    }
  }

  /** Parses an A1 cell reference into a 0-based {row, col}. */
  private static int[] cellToRowCol(String cell) {
    String c = StringUtils.trimToEmpty(cell).toUpperCase(java.util.Locale.ROOT);
    int i = 0;
    int col = 0;
    while (i < c.length() && Character.isLetter(c.charAt(i))) {
      col = col * 26 + (c.charAt(i) - 'A' + 1);
      i++;
    }
    int row = 0;
    if (i < c.length()) {
      try {
        row = Integer.parseInt(c.substring(i));
      } catch (NumberFormatException e) {
        row = 1;
      }
    }
    return new int[] { Math.max(row - 1, 0), Math.max(col - 1, 0) };
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  // ---------------------------------------------------------------------------
  // Image resolution (url via SSRF-guarded fetch, or a DMS document_id)
  // ---------------------------------------------------------------------------

  // Resolves the bytes of an image referenced by an add_image operation, in a
  // clear precedence order: (1) the op's explicit http(s) 'url', (2) the op's
  // explicit DMS 'document_id', (3) a per-op attachment reference, then (4) the
  // tool-level chat-attachment reference EVA injects (attachmentObjectType +
  // attachmentObjectId) when the user attaches an image in the chat. Sources 3
  // and 4 are read as the current user through the shared UploadToolUtils
  // resolver, so platform ACLs are enforced (an unreadable object throws).
  private byte[] resolveImageBytes(JSONObject op,
                                   Session session,
                                   String attachmentObjectType,
                                   String attachmentObjectId) throws IllegalAccessException, ObjectNotFoundException {
    String url = op.optString("url", "");
    String imageDocumentId = op.optString("document_id", op.optString("image_document_id", ""));
    if (StringUtils.isNotBlank(url)) {
      UploadToolUtils.FetchedContent fetched = UploadToolUtils.fetchUrl(url, MAX_IMAGE_BYTES, "image.png");
      return fetched.bytes();
    }
    if (StringUtils.isNotBlank(imageDocumentId)) {
      Node imageNode = resolveNode(session, imageDocumentId);
      return readNodeBytes(imageNode, imageDocumentId);
    }
    // A per-op attachment reference overrides the tool-level one; otherwise fall
    // back to the chat attachment EVA injected at the top level of the call.
    String objectType = StringUtils.defaultIfBlank(op.optString("attachment_object_type", ""), attachmentObjectType);
    String objectId = StringUtils.defaultIfBlank(op.optString("attachment_object_id", ""), attachmentObjectId);
    if (StringUtils.isNotBlank(objectId)) {
      Identity aclIdentity = userAcl.getUserIdentity(getCurrentUserName());
      UploadToolUtils.FetchedContent fetched = UploadToolUtils.resolveImage(attachmentService,
                                                                            fileService,
                                                                            aclIdentity,
                                                                            null,
                                                                            null,
                                                                            objectType,
                                                                            objectId,
                                                                            MAX_IMAGE_BYTES);
      return fetched.bytes();
    }
    throw new IllegalArgumentException("An 'add_image' operation requires either a 'url' (http/https image), a "
        + "'document_id' (a DMS image file), or an attached image (attachment_object_type + attachment_object_id).");
  }

  private byte[] readNodeBytes(Node node, String documentId) throws IllegalAccessException {
    try {
      Node content = node.getNode("jcr:content");
      try (InputStream in = content.getProperty("jcr:data").getStream()) {
        return in.readAllBytes();
      }
    } catch (AccessDeniedException e) {
      throw new IllegalAccessException("You are not allowed to read the content of document %s.".formatted(documentId));
    } catch (RepositoryException | IOException e) {
      throw new IllegalStateException("Could not read the content of document %s: %s".formatted(documentId, e.getMessage()));
    }
  }

  // ---------------------------------------------------------------------------
  // User session / node resolution (acts as the user, ACL enforced)
  // ---------------------------------------------------------------------------

  protected UserSession openUserSession() {
    Identity aclIdentity = userAcl.getUserIdentity(getCurrentUserName());
    if (aclIdentity == null) {
      throw new IllegalStateException("Cannot resolve the current user identity, so no document can be accessed.");
    }
    SessionProvider provider = OfficeDocumentsThumbnailPlugin.getUserSessionProvider(repositoryService, aclIdentity);
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      String workspace = repository.getConfiguration().getDefaultWorkspaceName();
      Session session = provider.getSession(workspace, repository);
      return new UserSession(provider, session);
    } catch (RepositoryException e) {
      provider.close();
      throw new IllegalStateException("Could not open a JCR session for the current user: " + e.getMessage());
    }
  }

  private Node resolveNode(Session session, String documentId) throws IllegalAccessException, ObjectNotFoundException {
    try {
      return session.getNodeByUUID(documentId);
    } catch (ItemNotFoundException e) {
      throw new ObjectNotFoundException("No document found with id %s. Verify the id (it must be a JCR uuid)."
          .formatted(documentId));
    } catch (AccessDeniedException e) {
      throw new IllegalAccessException("You are not allowed to access the document %s.".formatted(documentId));
    } catch (RepositoryException e) {
      throw new IllegalStateException("Could not resolve the document %s: %s".formatted(documentId, e.getMessage()));
    }
  }

  private Node createTempHtmlNode(Node parent, String content, Session session) throws RepositoryException {
    String tmpName = "eva-onlyoffice-tmp-" + UUID.randomUUID() + ".html";
    Node file = parent.addNode(tmpName, "nt:file");
    if (!file.isNodeType("mix:referenceable")) {
      file.addMixin("mix:referenceable");
    }
    Node resource = file.addNode("jcr:content", "nt:resource");
    resource.setProperty("jcr:mimeType", "text/html");
    resource.setProperty("jcr:data",
                         session.getValueFactory().createValue(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    resource.setProperty("jcr:lastModified", Calendar.getInstance());
    session.save();
    return file;
  }

  private void deleteQuietly(Node node) {
    if (node == null) {
      return;
    }
    try {
      Session session = node.getSession();
      node.remove();
      session.save();
    } catch (Exception e) {
      LOG.warn("Could not delete the temporary conversion node", e);
    }
  }

  private String nodeTitleOrName(Node node) {
    try {
      if (node.hasProperty("exo:title")) {
        return node.getProperty("exo:title").getString();
      }
      return URLDecoder.decode(node.getName(), StandardCharsets.UTF_8);
    } catch (RepositoryException e) {
      try {
        return node.getName();
      } catch (RepositoryException re) {
        return "document";
      }
    }
  }

  private String parentFolderIdOf(Node node, String documentId) {
    try {
      Node parent = node.getParent();
      if (parent.isNodeType("mix:referenceable")) {
        return parent.getUUID();
      }
    } catch (RepositoryException e) {
      LOG.debug("Could not resolve the parent folder of node {}", documentId, e);
    }
    throw new IllegalArgumentException(("Could not determine the source folder of document %s; pass 'folder_id' "
        + "explicitly to choose where the result is stored.").formatted(documentId));
  }

  private OfficeDocumentModel toModelFromNode(Node node, String documentId) {
    String path = "";
    String mimeType = "application/octet-stream";
    String parentFolderId = null;
    try {
      path = node.getPath();
    } catch (RepositoryException e) {
      LOG.debug("Could not read path of node {}", documentId, e);
    }
    try {
      mimeType = node.getNode("jcr:content").getProperty("jcr:mimeType").getString();
    } catch (RepositoryException e) {
      LOG.debug("Could not read mime type of node {}", documentId, e);
    }
    try {
      Node parent = node.getParent();
      if (parent != null && parent.isNodeType("mix:referenceable")) {
        parentFolderId = parent.getUUID();
      }
    } catch (Exception e) {
      LOG.debug("Could not read parent of node {}", documentId, e);
    }
    return new OfficeDocumentModel(documentId,
                                   nodeTitleOrName(node),
                                   path,
                                   FILE_URL_FORMAT.formatted(documentId),
                                   parentFolderId,
                                   mimeType);
  }

  // ---------------------------------------------------------------------------
  // Storage (Documents import path, reused from the Documents add-on)
  // ---------------------------------------------------------------------------

  private OfficeDocumentModel importContent(String parentFolderId,
                                            String fileName,
                                            byte[] bytes) throws IllegalAccessException, ObjectNotFoundException {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("The produced document is empty; there is nothing to store.");
    }
    getNode(parentFolderId);
    String entryName = sanitizeFileName(fileName);
    String ownerId = String.valueOf(getOwnerId(parentFolderId));
    long userIdentityId = getCurrentUserIdentityId();
    Set<String> beforeFileIds = currentChildFileIds(parentFolderId);
    String uploadId = null;
    try {
      uploadId = materializeUpload(zipSingleEntry(entryName, bytes), entryName + ".zip");
      documentFileService.importFiles(ownerId,
                                      parentFolderId,
                                      null,
                                      uploadId,
                                      "duplicate",
                                      getCurrentUserAclIdentity(),
                                      userIdentityId);
      uploadId = null;
    } catch (IllegalAccessException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Could not store the document '%s': %s".formatted(entryName, e.getMessage()));
    } finally {
      if (uploadId != null) {
        releaseUpload(uploadId);
      }
    }
    return findImportedDocument(parentFolderId, entryName, beforeFileIds);
  }

  private OfficeDocumentModel findImportedDocument(String parentFolderId,
                                                   String fileName,
                                                   Set<String> beforeFileIds) throws ObjectNotFoundException,
                                                                              IllegalAccessException {
    for (int attempt = 0; attempt < IMPORT_POLL_MAX_ATTEMPTS; attempt++) {
      OfficeDocumentModel match = resolveAddedChildFile(parentFolderId, beforeFileIds);
      if (match != null) {
        return match;
      }
      sleepQuietly(IMPORT_POLL_INTERVAL_MS);
    }
    throw new IllegalStateException(("The document '%s' was created and is being stored in the background. "
        + "Call get_documents_by_folder_id on folder %s in a few seconds to retrieve it.")
        .formatted(fileName, parentFolderId));
  }

  private Set<String> currentChildFileIds(String folderId) throws ObjectNotFoundException, IllegalAccessException {
    DocumentFolderFilter filter = new DocumentFolderFilter(folderId, null, null, null);
    return documentFileService.getFolderChildNodes(filter, 0, IMPORT_POLL_PAGE_SIZE, getCurrentUserIdentityId())
                              .stream()
                              .filter(FileNode.class::isInstance)
                              .map(AbstractNode::getId)
                              .collect(Collectors.toSet());
  }

  private OfficeDocumentModel resolveAddedChildFile(String folderId,
                                                    Set<String> beforeFileIds) throws ObjectNotFoundException,
                                                                               IllegalAccessException {
    DocumentFolderFilter filter = new DocumentFolderFilter(folderId, null, null, null);
    List<AbstractNode> children = documentFileService.getFolderChildNodes(filter,
                                                                          0,
                                                                          IMPORT_POLL_PAGE_SIZE,
                                                                          getCurrentUserIdentityId());
    return children.stream()
                   .filter(FileNode.class::isInstance)
                   .map(FileNode.class::cast)
                   .filter(file -> !beforeFileIds.contains(file.getId()))
                   .reduce((first, second) -> second)
                   .map(this::toModel)
                   .orElse(null);
  }

  private OfficeDocumentModel toModel(FileNode fileNode) {
    return new OfficeDocumentModel(fileNode.getId(),
                                   fileNode.getName(),
                                   fileNode.getPath(),
                                   FILE_URL_FORMAT.formatted(fileNode.getId()),
                                   fileNode.getParentFolderId(),
                                   fileNode.getMimeType());
  }

  protected String materializeUpload(byte[] zipBytes, String zipName) {
    return UploadToolUtils.materialize(uploadService, zipBytes, zipName, "application/zip");
  }

  protected void releaseUpload(String uploadId) {
    UploadToolUtils.release(uploadService, uploadId);
  }

  private AbstractNode getNode(String documentId) throws IllegalAccessException, ObjectNotFoundException {
    return documentFileService.getDocumentById(documentId, getCurrentUserName());
  }

  private long getOwnerId(String folderId) {
    return documentFileService.getRootFolderOwnerId(folderId);
  }

  private long getCurrentUserIdentityId() {
    return identityManager.getOrCreateUserIdentity(getCurrentUserName()).getIdentityId();
  }

  // ---------------------------------------------------------------------------
  // Small helpers
  // ---------------------------------------------------------------------------

  private static String appendExtension(String name, String type) {
    String base = StringUtils.trimToEmpty(name);
    String suffix = "." + type;
    if (StringUtils.endsWithIgnoreCase(base, suffix)) {
      base = base.substring(0, base.length() - suffix.length());
    }
    return base + suffix;
  }

  private static String stripExtension(String name) {
    if (StringUtils.isBlank(name)) {
      return "document";
    }
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

  private static String extensionOf(String name) {
    if (StringUtils.isBlank(name)) {
      return "docx";
    }
    int dot = name.lastIndexOf('.');
    return dot > 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "docx";
  }

  private static String normalizeExtension(String type) {
    String t = StringUtils.lowerCase(StringUtils.trimToEmpty(type));
    if (t.startsWith(".")) {
      t = t.substring(1);
    }
    if (StringUtils.isBlank(t)) {
      throw new IllegalArgumentException("The target format is empty.");
    }
    return t;
  }

  private static byte[] zipSingleEntry(String entryName, byte[] content) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write(content);
      zip.closeEntry();
    } catch (IOException e) {
      throw new IllegalStateException("Could not package the document content: " + e.getMessage());
    }
    return output.toByteArray();
  }

  private static String sanitizeFileName(String fileName) {
    String cleaned = StringUtils.trimToEmpty(fileName).replaceAll("[/\\\\]", "_");
    return cleaned.isEmpty() ? "document" : cleaned;
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Holds a user {@link SessionProvider} and its opened {@link Session}; closing
   * it releases the provider.
   */
  protected static final class UserSession {

    private final SessionProvider provider;

    private final Session         session;

    protected UserSession(SessionProvider provider, Session session) {
      this.provider = provider;
      this.session = session;
    }

    protected Session session() {
      return session;
    }

    protected void close() {
      if (provider != null) {
        provider.close();
      }
    }
  }

}
