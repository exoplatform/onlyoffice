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
package org.exoplatform.onlyoffice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedConstruction;

import org.exoplatform.onlyoffice.jpa.storage.cache.CachedEditorConfigStorage;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

/**
 * The session provider the editor service opens for a user is closed once the
 * caller's provider and conversation state are back in place.
 */
public class EditorSessionProviderReleaseTest {

  private static final String                     USER     = "john";

  private final AtomicReference<SessionProvider>  slot     = new AtomicReference<>();

  private SessionProvider                         callerProvider;

  private ConversationState                       callerState;

  private OnlyofficeEditorServiceImpl             service;

  private MockedConstruction<SessionProvider>     created;

  @Before
  public void setUp() throws Exception {
    callerProvider = mock(SessionProvider.class);
    callerState = new ConversationState(new Identity("caller"));
    ConversationState.setCurrent(callerState);
    slot.set(callerProvider);

    SessionProviderService providers = mock(SessionProviderService.class);
    when(providers.getSessionProvider(isNull())).thenAnswer(i -> slot.get());
    doAnswer(i -> {
      slot.set(i.getArgument(1));
      return null;
    }).when(providers).setSessionProvider(isNull(), any());

    service = mock(OnlyofficeEditorServiceImpl.class, CALLS_REAL_METHODS);
    set("sessionProviders", providers);
    set("docBuilderAssets", new ConcurrentHashMap<>());
    doReturn(new Identity(USER)).when(service).userIdentity(USER);
    created = mockConstruction(SessionProvider.class);
  }

  @After
  public void tearDown() {
    created.close();
    ConversationState.setCurrent(null);
  }

  @Test
  public void testTheTitleUpdateClosesTheUserProvider() {
    // a blank title returns early, still under the user's provider
    service.updateTitle("collaboration", "docId", " ", USER);

    assertUserProviderClosedAndCallersRestored();
  }

  @Test
  public void testARenameClosesTheUserProviderOnceSaved() throws Exception {
    NodeImpl node = mock(NodeImpl.class);
    NodeImpl parent = mock(NodeImpl.class);
    SessionImpl session = mock(SessionImpl.class);
    when(node.getPath()).thenReturn("/Users/john/Documents/old.docx");
    when(node.getParent()).thenReturn(parent);
    when(parent.getPath()).thenReturn("/Users/john/Documents");
    when(parent.getSession()).thenReturn(session);
    doReturn(node).when(service).getDocumentById("collaboration", "docId");
    doReturn(true).when(service).canRenameDocument(node);

    service.updateTitle("collaboration", "docId", "new.docx", USER);

    verify(session).move("/Users/john/Documents/old.docx", "/Users/john/Documents/new.docx");
    verify(session).save();
    assertUserProviderClosedAndCallersRestored();
  }

  @Test
  public void testAFailedRenameClosesTheUserProvider() throws Exception {
    doThrow(new RepositoryException("gone")).when(service).getDocumentById("collaboration", "docId");

    service.updateTitle("collaboration", "docId", "new.docx", USER);

    assertUserProviderClosedAndCallersRestored();
  }

  @Test
  public void testAFailedSaveClosesTheUserProvider() throws Exception {
    Config config = mock(Config.class);
    Config.Document document = mock(Config.Document.class);
    when(document.getKey()).thenReturn("key");
    when(config.getWorkspace()).thenReturn("collaboration");
    when(config.getDocId()).thenReturn("uuid");
    when(config.getDocument()).thenReturn(document);
    DocumentStatus status = mock(DocumentStatus.class);
    when(status.getConfig()).thenReturn(config);
    when(status.getUserId()).thenReturn(USER);
    doNothing().when(service).validateUser(eq(USER), any());
    doReturn(false).when(service).isSupersededKey(config);
    doThrow(new RepositoryException("gone")).when(service).getDocumentById("collaboration", "uuid");

    assertThrows(RepositoryException.class, () -> service.download(status));

    assertUserProviderClosedAndCallersRestored();
  }

  @Test
  public void testTheContentKeepsTheUserProviderOpenUntilItsStreamIsRead() throws Exception {
    Node node = mock(Node.class);
    Node content = mock(Node.class);
    Property mimeType = mock(Property.class);
    when(mimeType.getString()).thenReturn("text/plain");
    Property data = mock(Property.class);
    when(data.getStream()).thenReturn(new ByteArrayInputStream("abc".getBytes()));
    when(content.getProperty("jcr:mimeType")).thenReturn(mimeType);
    when(content.getProperty("jcr:data")).thenReturn(data);
    contentConfig(node, content);

    DocumentContent result = service.getContent(USER, "key");

    // the caller's provider is back and the stream still reads from the user's
    assertSame(callerProvider, slot.get());
    assertEquals(callerState, ConversationState.getCurrent());
    SessionProvider userProvider = created.constructed().get(0);
    verify(userProvider, never()).close();
    assertEquals(3, result.getData().readAllBytes().length);
    verify(userProvider).close();
    verify(callerProvider, never()).close();
  }

  @Test
  public void testTheContentClosesTheUserProviderWhenItCannotBeRead() throws Exception {
    contentConfig(mock(Node.class), null);
    doThrow(new RepositoryException("gone")).when(service).nodeByUUID(any(), any());

    assertThrows(RepositoryException.class, () -> service.getContent(USER, "key"));

    assertUserProviderClosedAndCallersRestored();
  }

  private void assertUserProviderClosedAndCallersRestored() {
    assertEquals(1, created.constructed().size());
    verify(created.constructed().get(0)).close();
    verify(callerProvider, never()).close();
    assertSame(callerProvider, slot.get());
    assertEquals(callerState, ConversationState.getCurrent());
  }

  private void contentConfig(Node node, Node content) throws Exception {
    CachedEditorConfigStorage storage = mock(CachedEditorConfigStorage.class);
    Config config = mock(Config.class);
    when(config.getWorkspace()).thenReturn("collaboration");
    when(config.getDocId()).thenReturn("uuid");
    ConcurrentHashMap<String, Config> byUser = new ConcurrentHashMap<>();
    byUser.put(USER, config);
    when(storage.getConfigsByKey("key")).thenReturn(byUser);
    set("cachedEditorConfigStorage", storage);
    doNothing().when(service).validateUser(eq(USER), any());
    doReturn(node).when(service).nodeByUUID(any(), any());
    if (content != null) {
      doReturn(content).when(service).nodeContent(node);
    }
  }

  private void set(String name, Object value) throws Exception {
    Field field = OnlyofficeEditorServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(service, value);
  }
}
