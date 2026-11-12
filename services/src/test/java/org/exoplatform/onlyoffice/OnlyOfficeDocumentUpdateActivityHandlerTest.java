/*
 * Copyright (C) 2026 eXo Platform SAS
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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
package org.exoplatform.onlyoffice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * A content write that reaches the document update handler closes the stale
 * editor sessions of the document; another property's write does not, and a
 * failure to close them never fails the write.
 */
class OnlyOfficeDocumentUpdateActivityHandlerTest {

  private static final String               DOC_ID = "doc-uuid";

  private MockedStatic<ExoContainerContext> containerContext;

  private MockedStatic<CommonsUtils>        commonsUtils;

  private OnlyofficeEditorService           editorService;

  private OnlyOfficeDocumentUpdateActivityHandler handler;

  @BeforeEach
  void setUp() {
    editorService = mock(OnlyofficeEditorService.class);
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(OnlyofficeEditorService.class)).thenReturn(editorService);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
    commonsUtils = mockStatic(CommonsUtils.class);
    commonsUtils.when(() -> CommonsUtils.getService(ImageThumbnailService.class)).thenReturn(mock(ImageThumbnailService.class));
    handler = new OnlyOfficeDocumentUpdateActivityHandler();
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    commonsUtils.close();
    containerContext.close();
  }

  @Test
  void contentWriteClosesTheStaleEditorSessions() throws Exception {
    handler.handleDocumentUpdateEvent(event("jcr:data"));

    verify(editorService).contentUpdatedOutsideEditor(DOC_ID, "mary");
  }

  @Test
  void anotherPropertyWriteClosesNoEditorSession() throws Exception {
    handler.handleDocumentUpdateEvent(event("dc:title"));

    verify(editorService, never()).contentUpdatedOutsideEditor(anyString(), any());
  }

  @Test
  void failureToCloseTheSessionsDoesNotFailTheWrite() throws Exception {
    doThrow(new IllegalStateException("storage down")).when(editorService).contentUpdatedOutsideEditor(DOC_ID, "mary");

    assertDoesNotThrow(() -> handler.handleDocumentUpdateEvent(event("jcr:data")));
  }

  private Event<Context, String> event(String propertyName) throws Exception {
    Node node = mock(Node.class, Answers.RETURNS_DEEP_STUBS);
    when(node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)).thenReturn(true);
    when(node.getUUID()).thenReturn(DOC_ID);
    when(node.hasProperty("eoo:commentId")).thenReturn(false);
    when(node.getProperty("exo:lastModifier").getString()).thenReturn("mary");
    Property property = mock(Property.class, Answers.RETURNS_DEEP_STUBS);
    when(property.getParent().getParent()).thenReturn(node);
    Context context = mock(Context.class);
    when(context.get(InvocationContext.CURRENT_ITEM)).thenReturn(property);
    return new Event<>("FileActivityNotify.event.PropertyUpdated", context, propertyName);
  }
}
