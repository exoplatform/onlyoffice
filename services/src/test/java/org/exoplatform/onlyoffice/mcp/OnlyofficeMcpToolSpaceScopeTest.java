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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.ObjectProvider;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.documents.service.DocumentFileService;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.mcp.OnlyofficeMcpTool.UserSession;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

import io.meeds.mcp.server.constant.Origin;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;
import io.meeds.mcp.server.tool.util.UploadToolUtils;
import io.meeds.mcp.server.util.McpToolUtils;

/**
 * Pins the {@code add_image} guard of the office edit tools: while the call is
 * limited to a space, an image is added only when it is a file already in that
 * space; without a space scope the tools behave as before.
 */
public class OnlyofficeMcpToolSpaceScopeTest {

  /** The user the tool acts as. */
  private static final String               USER          = "john";

  /** The space the calls are limited to. */
  private static final long                 SPACE_ID      = 12L;

  /** The document that is edited. */
  private static final String               DOC_ID        = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

  /** An image file of the call's space. */
  private static final String               IMAGE_IN      = "be0688cd7f00010134384ab7e1b15a48";

  /** An image file of another space. */
  private static final String               IMAGE_OUT     = "00000000000000000000000000000001";

  private OnlyofficeEditorService           editorService;

  private ObjectProvider<SpaceScopeResolver> resolvers;

  private SpaceScopeResolver                documentResolver;

  private OnlyofficeMcpTool                 tool;

  private Session                           session;

  private MockedStatic<McpToolUtils>        utils;

  /**
   * Builds the tool over mocks, with a {@code document} resolver that puts
   * {@code IMAGE_IN} in the call's space and {@code IMAGE_OUT} in another one,
   * and limits the call to the space.
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    editorService = mock(OnlyofficeEditorService.class);
    resolvers = mock(ObjectProvider.class);
    documentResolver = mock(SpaceScopeResolver.class);
    when(documentResolver.getName()).thenReturn("document");
    when(documentResolver.spaceOf(IMAGE_IN)).thenReturn(SPACE_ID);
    when(documentResolver.spaceOf(IMAGE_OUT)).thenReturn(13L);
    // an id that isn't 32 lowercase hexadecimal digits is refused by the tool, not by a resolver that happens to not know it
    when(documentResolver.spaceOf(IMAGE_IN.toUpperCase())).thenReturn(SPACE_ID);
    when(documentResolver.spaceOf(" " + IMAGE_IN)).thenReturn(SPACE_ID);
    when(documentResolver.spaceOf("7")).thenReturn(SPACE_ID);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(documentResolver));

    UserACL userAcl = mock(UserACL.class);
    when(userAcl.getUserIdentity(USER)).thenReturn(new Identity(USER));
    tool = spy(new OnlyofficeMcpTool(editorService,
                                     mock(DocumentFileService.class),
                                     mock(IdentityManager.class),
                                     userAcl,
                                     mock(RepositoryService.class),
                                     mock(UploadService.class),
                                     mock(AttachmentService.class),
                                     mock(FileService.class),
                                     resolvers));
    ConversationState.setCurrent(new ConversationState(new Identity(USER)));
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(socialIdentity.getIdentityId()).thenReturn(1L);
    IdentityManager identityManager = mock(IdentityManager.class);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(socialIdentity);

    session = mock(Session.class);
    doReturn(new UserSession(mock(SessionProvider.class), session)).when(tool).openUserSession();
    doReturn("upload-1").when(tool).materializeUpload(any(byte[].class), anyString());
    when(editorService.runDocBuilderScript(anyString(), any(), anyString(), anyString(), eq(USER))).thenReturn(new byte[] { 1 });
    stubNode(DOC_ID, "doc.docx");
    stubNode(IMAGE_IN, "in.png");
    stubNode(IMAGE_OUT, "out.png");

    utils = mockStatic(McpToolUtils.class);
    limitTo(SPACE_ID);
  }

  /**
   * Restores the static mock and the conversation state.
   */
  @After
  public void tearDown() {
    utils.close();
    ConversationState.setCurrent(null);
  }

  /**
   * A call limited to a space refuses an image fetched from a url, and never
   * fetches it.
   */
  @Test
  public void scoped_urlImageIsRefused() {
    try (MockedStatic<UploadToolUtils> upload = mockStatic(UploadToolUtils.class)) {
      assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"url\":\"https://example.com/a.png\"}]", null, null));
      assertRefused(() -> editPresentation("[{\"type\":\"add_image\",\"slide\":0,\"url\":\"https://example.com/a.png\"}]"));
      assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"url\":\"https://example.com/a.png\",\"document_id\":\""
          + IMAGE_IN + "\"}]", null, null));
      upload.verifyNoInteractions();
    }
  }

  /**
   * A call limited to a space refuses an image by an attachment, whether the
   * operation or the tool names it, and even next to an in-space image node.
   */
  @Test
  public void scoped_attachmentImageIsRefused() {
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"attachment_object_type\":\"activity\","
        + "\"attachment_object_id\":\"5\"}]", null, null));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"attachment_object_type\":\"activity\","
        + "\"document_id\":\"" + IMAGE_IN + "\"}]", null, null));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\"}]", "activity", "5"));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"document_id\":\"" + IMAGE_IN + "\"}]", "activity", "5"));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"document_id\":\"" + IMAGE_IN
        + "\",\"attachment_object_id\":\"5\"}]", null, null));
    verify(editorService, never()).runDocBuilderScript(anyString(), any(), anyString(), anyString(), anyString());
  }

  /**
   * A call limited to a space refuses an add_image that names no image node.
   */
  @Test
  public void scoped_imageWithNoNodeIsRefused() {
    assertRefused(() -> editDocument("[{\"type\":\"add_image\"}]", null, null));
  }

  /**
   * A call limited to a space refuses an image node the resolver puts in
   * another space, one it doesn't know, an id that isn't exactly 32 lowercase
   * hexadecimal digits, and a second id given under the other name.
   */
  @Test
  public void scoped_imageNodeOutsideTheSpaceIsRefused() {
    assertRefused(() -> editDocument(imageOp("document_id", IMAGE_OUT), null, null));
    assertRefused(() -> editDocument(imageOp("image_document_id", IMAGE_OUT), null, null));
    assertRefused(() -> editDocument(imageOp("document_id", "00000000000000000000000000000002"), null, null));
    assertRefused(() -> editDocument(imageOp("document_id", IMAGE_IN.toUpperCase()), null, null));
    assertRefused(() -> editDocument(imageOp("document_id", " " + IMAGE_IN), null, null));
    assertRefused(() -> editDocument(imageOp("document_id", "7"), null, null));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"document_id\":\"" + IMAGE_IN
        + "\",\"image_document_id\":\"" + IMAGE_OUT + "\"}]", null, null));
    assertRefused(() -> editPresentation(imageOp("document_id", IMAGE_OUT)));
    verify(editorService, never()).runDocBuilderScript(anyString(), any(), anyString(), anyString(), anyString());
  }

  /**
   * A call limited to a space accepts an image node of that space, in a
   * document and in a presentation.
   */
  @Test
  public void scoped_imageNodeOfTheSpacePasses() throws Exception {
    assertNotNull(editDocument(imageOp("document_id", IMAGE_IN), null, null));
    assertNotNull(editPresentation(imageOp("document_id", IMAGE_IN)));
    assertNotNull(editDocument(imageOp("image_document_id", IMAGE_IN), null, null));
    verify(documentResolver, org.mockito.Mockito.atLeastOnce()).spaceOf(IMAGE_IN);
  }

  /**
   * Operations other than add_image are not affected by the guard.
   */
  @Test
  public void scoped_otherOperationsPass() throws Exception {
    assertNotNull(editDocument("[{\"type\":\"add_paragraph\",\"text\":\"hi\"}]", null, null));
  }

  /**
   * An image is refused when the resolver is missing, ambiguous, fails, or
   * isn't named {@code document}; a missing one is refused as such, not
   * through a failed lookup. Mutant: the resolver count check loosened to
   * {@code size() > 1}.
   */
  @Test
  public void scoped_missingOrAmbiguousResolverRefuses() {
    String op = imageOp("document_id", IMAGE_IN);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.empty());
    IllegalArgumentException noResolver = assertThrows(IllegalArgumentException.class,
                                                       () -> editDocument(op, null, null));
    assertTrue(noResolver.getMessage(), noResolver.getMessage().contains("can't be told"));

    SpaceScopeResolver other = mock(SpaceScopeResolver.class);
    when(other.getName()).thenReturn("activity");
    when(other.spaceOf(IMAGE_IN)).thenReturn(SPACE_ID);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(other));
    assertRefused(() -> editDocument(op, null, null));

    SpaceScopeResolver twin = mock(SpaceScopeResolver.class);
    when(twin.getName()).thenReturn("document");
    when(twin.spaceOf(IMAGE_IN)).thenReturn(SPACE_ID);
    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(documentResolver, twin));
    assertRefused(() -> editDocument(op, null, null));

    when(resolvers.orderedStream()).thenAnswer(invocation -> Stream.of(documentResolver));
    doThrow(new IllegalStateException("down")).when(documentResolver).spaceOf(IMAGE_IN);
    assertRefused(() -> editDocument(op, null, null));

    doReturn(null).when(documentResolver).spaceOf(IMAGE_IN);
    assertRefused(() -> editDocument(op, null, null));
  }

  /**
   * A call whose acting identity can't be read is taken as limited, and
   * refuses every image, even one of the space.
   */
  @Test
  public void unreadableIdentity_refusesEveryImage() {
    utils.when(McpToolUtils::getCurrentActingIdentity).thenThrow(new IllegalStateException("refused"));
    assertRefused(() -> editDocument(imageOp("document_id", IMAGE_IN), null, null));
    assertRefused(() -> editDocument("[{\"type\":\"add_image\",\"url\":\"https://example.com/a.png\"}]", null, null));
  }

  /**
   * Without a space scope, an image by url, by an image node of any space and
   * by an attachment is added as before.
   */
  @Test
  public void notScoped_imagesAreAddedAsBefore() throws Exception {
    utils.when(McpToolUtils::getCurrentActingIdentity).thenReturn(ActingIdentity.person(USER, Origin.CHAT, null));
    assertNotNull(editDocument(imageOp("document_id", IMAGE_OUT), null, null));
    assertNotNull(editPresentation(imageOp("document_id", IMAGE_OUT)));
    try (MockedStatic<UploadToolUtils> upload = mockStatic(UploadToolUtils.class)) {
      UploadToolUtils.FetchedContent fetched = new UploadToolUtils.FetchedContent(new byte[] { 9 }, "image/png", "a.png");
      upload.when(() -> UploadToolUtils.fetchUrl(eq("https://example.com/a.png"), anyLong(), anyString())).thenReturn(fetched);
      upload.when(() -> UploadToolUtils.resolveImage(any(), any(), any(), any(), anyLong())).thenReturn(fetched);
      assertNotNull(editDocument("[{\"type\":\"add_image\",\"url\":\"https://example.com/a.png\"}]", null, null));
      assertNotNull(editDocument("[{\"type\":\"add_image\"}]", "activity", "5"));
      upload.verify(() -> UploadToolUtils.fetchUrl(eq("https://example.com/a.png"), anyLong(), anyString()));
      upload.verify(() -> UploadToolUtils.resolveImage(any(), any(), any(), any(), anyLong()));
    }
    verify(documentResolver, never()).spaceOf(anyString());
  }

  /**
   * No acting identity at all (no run in progress) is not a limited call.
   */
  @Test
  public void noIdentity_isNotScoped() {
    utils.when(McpToolUtils::getCurrentActingIdentity).thenReturn(null);
    assertTrue(!tool.isSpaceScoped());
    assertEquals(null, tool.getSpaceScope());
  }

  /**
   * @param key the operation's key naming the image
   * @param id  the image id
   * @return a one operation add_image array
   */
  private static String imageOp(String key, String id) {
    return "[{\"type\":\"add_image\",\"slide\":0,\"%s\":\"%s\"}]".formatted(key, id);
  }

  /**
   * @param spaceId the space the calls are limited to
   */
  private void limitTo(long spaceId) {
    utils.when(McpToolUtils::getCurrentActingIdentity)
         .thenReturn(ActingIdentity.agent("agent-docs", "DOCS", Origin.PROPOSAL, spaceId));
  }

  /**
   * @param operations the operations
   * @param type       the tool-level attachment type
   * @param id         the tool-level attachment id
   * @return the result of editing the document
   */
  private Object editDocument(String operations, String type, String id) throws Exception {
    return tool.editDocument(DOC_ID, null, null, operations, type, id);
  }

  /**
   * @param operations the operations
   * @return the result of editing the presentation
   */
  private Object editPresentation(String operations) throws Exception {
    return tool.editPresentation(DOC_ID, null, null, operations, null, null);
  }

  /**
   * Asserts the call is refused as an {@code IllegalArgumentException} the
   * model can act on.
   *
   * @param call the call
   */
  private static void assertRefused(ThrowingCall call) {
    IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, call::run);
    assertTrue(refused.getMessage(), refused.getMessage().startsWith("Not executed: while this run is limited to the space"));
  }

  /**
   * Stubs a file node resolvable by uuid with readable content and a title.
   *
   * @param id    the node uuid
   * @param title the file name
   */
  private void stubNode(String id, String title) throws Exception {
    Node node = mock(Node.class);
    when(session.getNodeByUUID(id)).thenReturn(node);
    when(node.hasProperty("exo:title")).thenReturn(true);
    Property titleProp = mock(Property.class);
    when(titleProp.getString()).thenReturn(title);
    when(node.getProperty("exo:title")).thenReturn(titleProp);
    when(node.getPath()).thenReturn("/Groups/spaces/x/Documents/" + title);
    Node content = mock(Node.class);
    when(node.getNode("jcr:content")).thenReturn(content);
    Property data = mock(Property.class);
    when(content.getProperty("jcr:data")).thenReturn(data);
    when(data.getStream()).thenAnswer(invocation -> new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
    Property mime = mock(Property.class);
    when(content.getProperty("jcr:mimeType")).thenReturn(mime);
    when(mime.getString()).thenReturn("image/png");
  }

  /** A call that may throw. */
  @FunctionalInterface
  private interface ThrowingCall {
    /**
     * @throws Exception whatever the call throws
     */
    void run() throws Exception;
  }

}
