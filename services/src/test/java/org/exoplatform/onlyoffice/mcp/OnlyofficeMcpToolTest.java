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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.documents.model.AbstractNode;
import org.exoplatform.documents.model.FileNode;
import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.mcp.OnlyofficeMcpTool.UserSession;
import org.exoplatform.onlyoffice.mcp.model.OfficeDocumentModel;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

/**
 * Mockito unit tests for {@link OnlyofficeMcpTool}. The user JCR session, the
 * shared {@code runDocBuilderScript} primitive and the static upload helper are
 * stubbed through the tool's protected seams so every tool's orchestration, its
 * Document Builder script generation and its error mapping can be exercised
 * without a live Document Server or JCR.
 * <p>
 * <b>Golden-string tests.</b> The {@code goldenScript_*} tests below are
 * regression guards: they pin the <i>exact</i> Document Builder JavaScript the
 * generators emit for representative inputs (captured via an {@code
 * ArgumentCaptor} on the mocked {@code runDocBuilderScript}). They protect the
 * verified-correct emission — notably {@code Api.CreateTable(rowCount, colCount)}
 * (rows first) — from silent regressions. They do NOT prove the emitted API is
 * accepted by a Document Server: true API-correctness (arg order, method names,
 * signatures) was validated manually by running each generated script through a
 * live Document Server docbuilder endpoint and asserting a valid OOXML result
 * with the expected content. Re-run that DS integration check whenever a
 * generator's emitted API surface changes.
 */
public class OnlyofficeMcpToolTest {

  private static final String     USER = "john";

  private OnlyofficeEditorService editorService;

  private DocumentFileService     documentFileService;

  private IdentityManager         identityManager;

  private UserACL                 userAcl;

  private RepositoryService       repositoryService;

  private UploadService           uploadService;

  private AttachmentService       attachmentService;

  private FileService             fileService;

  private OnlyofficeMcpTool       tool;

  private Session                 session;

  @Before
  public void setUp() {
    editorService = mock(OnlyofficeEditorService.class);
    documentFileService = mock(DocumentFileService.class);
    identityManager = mock(IdentityManager.class);
    userAcl = mock(UserACL.class);
    repositoryService = mock(RepositoryService.class);
    uploadService = mock(UploadService.class);
    attachmentService = mock(AttachmentService.class);
    fileService = mock(FileService.class);

    tool = spy(new OnlyofficeMcpTool(editorService,
                                     documentFileService,
                                     identityManager,
                                     userAcl,
                                     repositoryService,
                                     uploadService,
                                     attachmentService,
                                     fileService));

    ConversationState.setCurrent(new ConversationState(new Identity(USER)));

    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(socialIdentity.getIdentityId()).thenReturn(1L);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(socialIdentity);

    session = mock(Session.class);
    SessionProvider provider = mock(SessionProvider.class);
    doReturn(new UserSession(provider, session)).when(tool).openUserSession();
    doReturn("upload-1").when(tool).materializeUpload(any(byte[].class), anyString());
  }

  @After
  public void tearDown() {
    ConversationState.setCurrent(null);
  }

  // ---------------------------------------------------------------------------
  // edit_spreadsheet
  // ---------------------------------------------------------------------------

  @Test
  public void editSpreadsheet_create_generatesCreateScript_andStoresFile() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "budget.xlsx", "new-xlsx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("xlsx"), eq("result.xlsx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1, 2, 3 });

    String ops = "[{\"type\":\"set_cells\",\"start\":\"A1\",\"values\":[[\"Region\",\"Sales\"],[\"North\",120]]},"
        + "{\"type\":\"set_formula\",\"cell\":\"B3\",\"formula\":\"=SUM(B2:B2)\"},"
        + "{\"type\":\"add_chart\",\"chart\":\"bar\",\"range\":\"Sheet1!$A$1:$B$2\",\"title\":\"Sales\"}]";
    OfficeDocumentModel result = tool.editSpreadsheet(null, "folder-1", "budget", ops);

    assertNotNull(result);
    assertEquals("new-xlsx-id", result.getId());
    assertEquals("budget.xlsx", result.getName());

    String script = captureScript("xlsx", "result.xlsx");
    assertTrue(script.contains("builder.CreateFile(\"xlsx\")"));
    assertTrue(script.contains("GetRangeByNumber(0, 0).SetValue(\"Region\")"));
    assertTrue(script.contains("SetValue(120)"));
    assertTrue(script.contains(".SetValue(\"=SUM(B2:B2)\")"));
    assertTrue(script.contains("AddChart(\"Sheet1!$A$1:$B$2\", true, \"bar\""));
    assertTrue(script.contains("builder.SaveFile(\"xlsx\", \"result.xlsx\")"));
  }

  @Test
  public void editSpreadsheet_modify_opensFile_andSavesNewVersion() throws Exception {
    Node node = stubExistingDocument("xlsx-1", "budget.xlsx",
                                     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("xlsx"), eq("result.xlsx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 9 });

    String ops = "[{\"type\":\"add_sheet\",\"name\":\"Q3\"}]";
    OfficeDocumentModel result = tool.editSpreadsheet("xlsx-1", null, null, ops);

    assertNotNull(result);
    assertEquals("xlsx-1", result.getId());
    verify(editorService).saveNewVersion(eq(node), any(byte[].class), eq(USER));

    String script = captureScript("xlsx", "result.xlsx");
    assertTrue(script.contains("builder.OpenFile(\"@@ASSET:input@@\", \"\")"));
    assertTrue(script.contains("Api.AddSheet(\"Q3\")"));
  }

  @Test
  public void editSpreadsheet_failedBuild_throwsIllegalState() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    when(editorService.runDocBuilderScript(anyString(), any(), anyString(), anyString(), eq(USER))).thenReturn(null);

    IllegalStateException e = assertThrows(IllegalStateException.class,
                                           () -> tool.editSpreadsheet(null,
                                                                      "folder-1",
                                                                      "budget",
                                                                      "[{\"type\":\"add_sheet\",\"name\":\"S\"}]"));
    assertTrue(e.getMessage().contains("Document Server"));
  }

  @Test
  public void editSpreadsheet_neitherIdNorFolder_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
                 () -> tool.editSpreadsheet(null, null, null, "[{\"type\":\"add_sheet\",\"name\":\"S\"}]"));
  }

  @Test
  public void editSpreadsheet_badOperationsJson_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> tool.editSpreadsheet(null, "folder-1", "b", "not json"));
  }

  // ---------------------------------------------------------------------------
  // edit_presentation
  // ---------------------------------------------------------------------------

  @Test
  public void editPresentation_create_generatesSlideScript() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "deck.pptx", "new-pptx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("pptx"), eq("result.pptx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 7 });

    String ops = "[{\"type\":\"add_slide\",\"title\":\"Review\",\"bullets\":[\"One\",\"Two\"]}]";
    OfficeDocumentModel result = tool.editPresentation(null, "folder-1", "deck", ops, null, null);

    assertEquals("new-pptx-id", result.getId());
    String script = captureScript("pptx", "result.pptx");
    assertTrue(script.contains("builder.CreateFile(\"pptx\")"));
    assertTrue(script.contains("Api.CreateSlide()"));
    assertTrue(script.contains("__setSlideText(oSlide, \"Review\", [\"One\", \"Two\"])"));
  }

  @Test
  public void editPresentation_modify_opensFile_andSavesNewVersion() throws Exception {
    Node node = stubExistingDocument("pptx-1", "deck.pptx",
                                     "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("pptx"), eq("result.pptx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 3 });

    OfficeDocumentModel result = tool.editPresentation("pptx-1", null, null,
                                                       "[{\"type\":\"set_slide\",\"index\":0,\"title\":\"Hi\"}]", null, null);

    assertEquals("pptx-1", result.getId());
    verify(editorService).saveNewVersion(eq(node), any(byte[].class), eq(USER));
    String script = captureScript("pptx", "result.pptx");
    assertTrue(script.contains("builder.OpenFile(\"@@ASSET:input@@\", \"\")"));
    assertTrue(script.contains("GetSlideByIndex(0)"));
  }

  // ---------------------------------------------------------------------------
  // edit_document
  // ---------------------------------------------------------------------------

  @Test
  public void editDocument_create_generatesStructuredScript() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "report.docx", "new-docx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("docx"), eq("result.docx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 5 });

    String ops = "[{\"type\":\"add_heading\",\"level\":1,\"text\":\"Title\"},"
        + "{\"type\":\"add_paragraph\",\"text\":\"Body\"},"
        + "{\"type\":\"add_table\",\"rows\":[[\"A\",\"B\"],[\"1\",\"2\"]]},"
        + "{\"type\":\"replace_text\",\"search\":\"{{x}}\",\"replace\":\"y\"}]";
    OfficeDocumentModel result = tool.editDocument(null, "folder-1", "report", ops, null, null);

    assertEquals("new-docx-id", result.getId());
    String script = captureScript("docx", "result.docx");
    assertTrue(script.contains("builder.CreateFile(\"docx\")"));
    assertTrue(script.contains("oR.AddText(\"Title\"); oR.SetBold(true)"));
    assertTrue(script.contains("Api.CreateTable(2, 2)"));
    assertTrue(script.contains("SearchAndReplace({\"searchString\":\"{{x}}\", \"replaceString\":\"y\"})"));
  }

  @Test
  public void editDocument_modify_opensFile_andSavesNewVersion() throws Exception {
    Node node = stubExistingDocument("docx-1", "report.docx",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("docx"), eq("result.docx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 2 });

    OfficeDocumentModel result = tool.editDocument("docx-1", null, null,
                                                   "[{\"type\":\"add_paragraph\",\"text\":\"More\"}]", null, null);

    assertEquals("docx-1", result.getId());
    verify(editorService).saveNewVersion(eq(node), any(byte[].class), eq(USER));
    String script = captureScript("docx", "result.docx");
    assertTrue(script.contains("builder.OpenFile(\"@@ASSET:input@@\", \"\")"));
  }

  @Test
  public void editDocument_setHtml_create_usesConversionPath() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubTempNodeCreation(parent);
    Node tmp = parent.addNode("x", "nt:file");
    when(editorService.convertNodeContent(eq(tmp), eq("docx"), eq("html"), eq(USER))).thenReturn(new byte[] { 1 });
    stubStorage("folder-1", "page.docx", "html-docx-id");

    OfficeDocumentModel result = tool.editDocument(null,
                                                   "folder-1",
                                                   "page",
                                                   "[{\"type\":\"set_html\",\"html\":\"<h1>Hi</h1>\"}]",
                                                   null,
                                                   null);

    assertEquals("html-docx-id", result.getId());
    verify(editorService).convertNodeContent(eq(tmp), eq("docx"), eq("html"), eq(USER));
  }

  @Test
  public void editDocument_setHtml_withDocumentId_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
                 () -> tool.editDocument("doc-1", null, null, "[{\"type\":\"set_html\",\"html\":\"<p>x</p>\"}]", null, null));
  }

  @Test
  public void editDocument_setHtml_mixedWithStructured_throwsIllegalArgument() {
    String ops = "[{\"type\":\"set_html\",\"html\":\"<p>x</p>\"},{\"type\":\"add_paragraph\",\"text\":\"y\"}]";
    assertThrows(IllegalArgumentException.class, () -> tool.editDocument(null, "folder-1", "p", ops, null, null));
  }

  @Test
  public void editDocument_unsupportedOperation_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
                 () -> tool.editDocument(null, "folder-1", "p", "[{\"type\":\"add_footnote\",\"text\":\"x\"}]", null, null));
  }

  // ---------------------------------------------------------------------------
  // generate_document
  // ---------------------------------------------------------------------------

  @Test
  public void generateDocument_fillsTemplate_andStoresNewFile() throws Exception {
    Node template = stubExistingDocument("tpl-1", "contract.docx",
                                         "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    Node templateParent = mock(Node.class);
    when(template.getParent()).thenReturn(templateParent);
    when(templateParent.isNodeType("mix:referenceable")).thenReturn(true);
    when(templateParent.getUUID()).thenReturn("tpl-folder");
    stubStorage("out-folder", "filled.docx", "gen-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("docx"), eq("result.docx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 4 });

    OfficeDocumentModel result = tool.generateDocument("tpl-1",
                                                       "out-folder",
                                                       "filled",
                                                       "{\"{{client}}\":\"ACME\",\"{{amount}}\":\"42000\"}");

    assertEquals("gen-id", result.getId());
    String script = captureScript("docx", "result.docx");
    assertTrue(script.contains("builder.OpenFile(\"@@ASSET:input@@\", \"\")"));
    assertTrue(script.contains("SearchAndReplace({\"searchString\":\"{{client}}\", \"replaceString\":\"ACME\"})"));
  }

  @Test
  public void generateDocument_blankData_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> tool.generateDocument("tpl-1", null, null, "{}"));
  }

  // ---------------------------------------------------------------------------
  // convert_document
  // ---------------------------------------------------------------------------

  @Test
  public void convertDocument_convertsAndStores() throws Exception {
    Node node = stubExistingDocument("doc-1", "report.docx",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    Node parent = mock(Node.class);
    when(node.getParent()).thenReturn(parent);
    when(parent.isNodeType("mix:referenceable")).thenReturn(true);
    when(parent.getUUID()).thenReturn("src-folder");
    stubStorage("src-folder", "report.odt", "odt-id");
    when(editorService.convertNodeContent(eq(node), eq("odt"), eq("docx"), eq(USER))).thenReturn(new byte[] { 8 });

    OfficeDocumentModel result = tool.convertDocument("doc-1", "odt", null);

    assertEquals("odt-id", result.getId());
    assertEquals("report.odt", result.getName());
  }

  @Test
  public void convertDocument_nullConversion_throwsIllegalState() throws Exception {
    Node node = stubExistingDocument("doc-1", "report.docx",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    Node parent = mock(Node.class);
    when(node.getParent()).thenReturn(parent);
    when(parent.isNodeType("mix:referenceable")).thenReturn(true);
    when(parent.getUUID()).thenReturn("src-folder");
    when(editorService.convertNodeContent(eq(node), eq("odt"), anyString(), eq(USER))).thenReturn(null);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> tool.convertDocument("doc-1", "odt", null));
    assertTrue(e.getMessage().contains("Document Server"));
  }

  // ---------------------------------------------------------------------------
  // export_document_as_pdf
  // ---------------------------------------------------------------------------

  @Test
  public void exportDocumentAsPdf_happyPath_storesPdf() throws Exception {
    Node node = stubExistingDocument("doc-1", "Quarter.docx",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    when(editorService.convertNodeContentToPdf(node, USER)).thenReturn(new byte[] { 9 });
    stubStorage("target-folder", "Quarter.pdf", "pdf-doc-id");

    OfficeDocumentModel result = tool.exportDocumentAsPdf("doc-1", "target-folder");

    assertNotNull(result);
    assertEquals("pdf-doc-id", result.getId());
    assertEquals("Quarter.pdf", result.getName());
  }

  @Test
  public void exportDocumentAsPdf_blankId_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> tool.exportDocumentAsPdf("  ", null));
  }

  // ---------------------------------------------------------------------------
  // Golden-string regression guards (exact emitted Document Builder script).
  // These pin the DS-verified emission; they run with NO Document Server.
  // ---------------------------------------------------------------------------

  /**
   * add_table with 3 data rows + a header (4 total) x 3 columns must emit
   * {@code Api.CreateTable(rowCount, colCount)} = {@code (4, 3)} (rows first) and
   * address the last cell as {@code GetRow(3).GetCell(2)}. This is the exact bug
   * the DS check caught: emitting {@code (colCount, rowCount)} built a 3-row table
   * and {@code GetRow(3)} threw "Row index out of bounds".
   */
  @Test
  public void goldenScript_addTable_rowsFirst_and_lastCellAddressing() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "report.docx", "docx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("docx"), eq("result.docx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1 });

    String ops = "[{\"type\":\"add_table\",\"rows\":["
        + "[\"H1\",\"H2\",\"H3\"],[\"a\",\"b\",\"c\"],[\"d\",\"e\",\"f\"],[\"g\",\"h\",\"i\"]]}]";
    tool.editDocument(null, "folder-1", "report", ops, null, null);

    String script = captureScript("docx", "result.docx");
    assertTrue("rows-first CreateTable(4, 3)", script.contains("Api.CreateTable(4, 3)"));
    assertTrue("last cell GetRow(3).GetCell(2)", script.contains("oTable.GetRow(3).GetCell(2)"));
    assertTrue("header cell text", script.contains("oTable.GetRow(0).GetCell(0).GetContent().GetElement(0).AddText(\"H1\")"));
    assertTrue("last cell text", script.contains("oTable.GetRow(3).GetCell(2).GetContent().GetElement(0).AddText(\"i\")"));
    assertTrue("table width", script.contains("oTable.SetWidth(\"percent\", 100)"));
    assertTrue("table pushed", script.contains("oDocument.Push(oTable)"));
  }

  /**
   * set_cells over a 2x2 range starting at A1 must emit 0-based
   * {@code GetRangeByNumber(row, col)} calls (row first), one per cell, with
   * numeric values unquoted.
   */
  @Test
  public void goldenScript_setCells_rangeByNumberRowColOrder() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "grid.xlsx", "xlsx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("xlsx"), eq("result.xlsx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1 });

    String ops = "[{\"type\":\"set_cells\",\"start\":\"A1\",\"values\":[[\"Region\",\"Sales\"],[\"North\",120]]}]";
    tool.editSpreadsheet(null, "folder-1", "grid", ops);

    String script = captureScript("xlsx", "result.xlsx");
    assertTrue(script.contains("oWorksheet.GetRangeByNumber(0, 0).SetValue(\"Region\")"));
    assertTrue(script.contains("oWorksheet.GetRangeByNumber(0, 1).SetValue(\"Sales\")"));
    assertTrue(script.contains("oWorksheet.GetRangeByNumber(1, 0).SetValue(\"North\")"));
    assertTrue(script.contains("oWorksheet.GetRangeByNumber(1, 1).SetValue(120)"));
  }

  /**
   * add_chart must emit the DS-verified 10-argument {@code AddChart} signature:
   * {@code (range, true, type, styleIndex=2, extX, extY, fromCol, colOff, fromRow,
   * rowOff)} where extX/extY are EMU (100*36000 / 70*36000).
   */
  @Test
  public void goldenScript_addChart_tenArgSignature() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "chart.xlsx", "xlsx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("xlsx"), eq("result.xlsx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1 });

    String ops = "[{\"type\":\"add_chart\",\"chart\":\"bar\",\"range\":\"Sheet1!$A$1:$B$2\",\"title\":\"Sales\"}]";
    tool.editSpreadsheet(null, "folder-1", "chart", ops);

    String script = captureScript("xlsx", "result.xlsx");
    assertTrue(script.contains("var oChart = oWorksheet.AddChart(\"Sheet1!$A$1:$B$2\", true, \"bar\", 2, 3600000, 2520000, 3, 0, 8, 0)"));
    assertTrue(script.contains("oChart.SetTitle(\"Sales\", 14)"));
  }

  /**
   * add_slide must create+append a slide and drive the {@code __setSlideText}
   * helper with the title and the bullets array literal.
   */
  @Test
  public void goldenScript_addSlide_createAndSetText() throws Exception {
    Node parent = mock(Node.class);
    when(session.getNodeByUUID("folder-1")).thenReturn(parent);
    stubStorage("folder-1", "deck.pptx", "pptx-id");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("pptx"), eq("result.pptx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1 });

    String ops = "[{\"type\":\"add_slide\",\"title\":\"Review\",\"bullets\":[\"One\",\"Two\"]}]";
    tool.editPresentation(null, "folder-1", "deck", ops, null, null);

    String script = captureScript("pptx", "result.pptx");
    assertTrue(script.contains("var oSlide = Api.CreateSlide(); oPresentation.AddSlide(oSlide);"));
    assertTrue(script.contains("__setSlideText(oSlide, \"Review\", [\"One\", \"Two\"]);"));
  }

  /**
   * The MODIFY path must open the source asset with
   * {@code builder.OpenFile("@@ASSET:input@@", "")} (not CreateFile) before
   * applying operations.
   */
  @Test
  public void goldenScript_modifyPath_opensAssetInsteadOfCreate() throws Exception {
    Node node = stubExistingDocument("docx-1", "report.docx",
                                     "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    when(editorService.runDocBuilderScript(anyString(), any(), eq("docx"), eq("result.docx"), eq(USER)))
                                                                                                        .thenReturn(new byte[] { 1 });

    tool.editDocument("docx-1", null, null, "[{\"type\":\"add_paragraph\",\"text\":\"More\"}]", null, null);

    verify(editorService).saveNewVersion(eq(node), any(byte[].class), eq(USER));
    String script = captureScript("docx", "result.docx");
    assertTrue(script.contains("builder.OpenFile(\"@@ASSET:input@@\", \"\");"));
    assertTrue("modify path must not CreateFile", !script.contains("builder.CreateFile"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private String captureScript(String format, String outputName) {
    ArgumentCaptor<String> scriptCaptor = ArgumentCaptor.forClass(String.class);
    verify(editorService).runDocBuilderScript(scriptCaptor.capture(), any(), eq(format), eq(outputName), eq(USER));
    return scriptCaptor.getValue();
  }

  /**
   * Stubs an existing document node resolvable by uuid, exposing a readable
   * jcr:content/jcr:data stream (for the MODIFY OpenFile path) and a title.
   */
  private Node stubExistingDocument(String id, String title, String mimeType) throws Exception {
    Node node = mock(Node.class);
    when(session.getNodeByUUID(id)).thenReturn(node);
    when(node.hasProperty("exo:title")).thenReturn(true);
    Property titleProp = mock(Property.class);
    when(titleProp.getString()).thenReturn(title);
    when(node.getProperty("exo:title")).thenReturn(titleProp);
    when(node.getPath()).thenReturn("/Users/john/" + title);

    Node content = mock(Node.class);
    when(node.getNode("jcr:content")).thenReturn(content);
    Property dataProp = mock(Property.class);
    when(content.getProperty("jcr:data")).thenReturn(dataProp);
    when(dataProp.getStream()).thenReturn(new ByteArrayInputStream(new byte[] { 1, 2, 3, 4 }));
    Property mimeProp = mock(Property.class);
    when(content.getProperty("jcr:mimeType")).thenReturn(mimeProp);
    when(mimeProp.getString()).thenReturn(mimeType);
    return node;
  }

  private void stubTempNodeCreation(Node parent) throws Exception {
    Node tmp = mock(Node.class);
    Node resource = mock(Node.class);
    ValueFactory valueFactory = mock(ValueFactory.class);
    Value value = mock(Value.class);
    when(parent.addNode(anyString(), eq("nt:file"))).thenReturn(tmp);
    when(tmp.isNodeType("mix:referenceable")).thenReturn(false);
    when(tmp.addNode("jcr:content", "nt:resource")).thenReturn(resource);
    when(tmp.getSession()).thenReturn(session);
    when(session.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.createValue(any(InputStream.class))).thenReturn(value);
  }

  /**
   * Stubs the Documents import path so it resolves the created file: the folder
   * is readable, and the child listing returns nothing before the import then the
   * new file afterwards (the poll).
   */
  private void stubStorage(String folderId, String expectedName, String newDocId) throws Exception {
    when(documentFileService.getDocumentById(folderId, USER)).thenReturn(mock(AbstractNode.class));
    when(documentFileService.getRootFolderOwnerId(folderId)).thenReturn(5L);

    FileNode created = mock(FileNode.class);
    when(created.getId()).thenReturn(newDocId);
    when(created.getName()).thenReturn(expectedName);
    when(created.getPath()).thenReturn("/Users/john/" + expectedName);
    when(created.getMimeType()).thenReturn("application/octet-stream");
    when(created.getParentFolderId()).thenReturn(folderId);
    List<AbstractNode> afterImport = new ArrayList<>();
    afterImport.add(created);

    when(documentFileService.getFolderChildNodes(any(), anyInt(), anyInt(), anyLong())).thenReturn(Collections.emptyList())
                                                                                       .thenReturn(afterImport);
  }

}
