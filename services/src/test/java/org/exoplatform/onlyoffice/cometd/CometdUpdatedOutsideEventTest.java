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
package org.exoplatform.onlyoffice.cometd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * The message telling a document's open editors that its content was written
 * outside them carries the writing session's key, whose editors hold the
 * written content and show no reload banner.
 */
class CometdUpdatedOutsideEventTest {

  private static final String                    DOC_ID = "doc-uuid";

  private ServerChannel                          channel;

  private LocalSession                           localSession;

  private CometdOnlyofficeService.CometdService  cometdService;

  @BeforeEach
  void setUp() throws Exception {
    CometdOnlyofficeService service = new CometdOnlyofficeService(mock(EXoContinuationBayeux.class),
                                                                  mock(OnlyofficeEditorService.class),
                                                                  mock(RepositoryService.class));
    cometdService = service.service;
    BayeuxServer bayeux = mock(BayeuxServer.class);
    channel = mock(ServerChannel.class);
    when(bayeux.getChannel(CometdOnlyofficeService.CHANNEL_NAME + DOC_ID)).thenReturn(channel);
    localSession = mock(LocalSession.class);
    inject("bayeux", bayeux);
    inject("localSession", localSession);
  }

  @Test
  void writeByAnEditorSessionNamesItsKey() throws Exception {
    cometdService.publishUpdatedOutsideEvent(DOC_ID, "writing-key");

    JSONObject message = publishedMessage();
    assertEquals(CometdOnlyofficeService.DOCUMENT_UPDATED_OUTSIDE_EVENT, message.getString("type"));
    assertEquals(DOC_ID, message.getString("docId"));
    assertEquals("writing-key", message.getString("writingKey"));
  }

  @Test
  void writeOutsideAnyEditorNamesNoKey() throws Exception {
    cometdService.publishUpdatedOutsideEvent(DOC_ID, null);

    JSONObject message = publishedMessage();
    assertEquals(DOC_ID, message.getString("docId"));
    assertTrue(message.isNull("writingKey"));
  }

  private JSONObject publishedMessage() throws Exception {
    ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
    verify(channel).publish(eq(localSession), data.capture(), any());
    return new JSONObject(data.getValue().toString());
  }

  private void inject(String fieldName, Object value) throws Exception {
    Field field = CometdOnlyofficeService.CometdService.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(cometdService, value);
  }
}
