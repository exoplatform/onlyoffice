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
package org.exoplatform.onlyoffice.cometd;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.Test;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.jcr.RepositoryService;

/**
 * A document's events are published by the node that handles the change,
 * while its editors are connected to any node of a cluster.
 */
public class CometdOnlyofficeServiceTest {

  @Test
  public void testTheEditorChannelsAreRelayedToEveryNode() {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdOnlyofficeService cometdService = new CometdOnlyofficeService(exoBayeux,
                                                                        mock(OnlyofficeEditorService.class),
                                                                        mock(RepositoryService.class));

    cometdService.start();

    verify(exoBayeux).observeChannel("/eXo/Application/Onlyoffice/editor/**");
  }

  @Test
  public void testTheCommandsRunOnDaemonThreadsThatEndWithTheService() throws Exception {
    CometdOnlyofficeService cometdService = new CometdOnlyofficeService(mock(EXoContinuationBayeux.class),
                                                                        mock(OnlyofficeEditorService.class),
                                                                        mock(RepositoryService.class));
    Future<Boolean> daemon = cometdService.eventsHandlers.submit(() -> Thread.currentThread().isDaemon());
    assertTrue(daemon.get(5, TimeUnit.SECONDS));

    cometdService.stop();

    assertTrue(cometdService.eventsHandlers.isTerminated());
  }

  @Test
  public void testTheStopWaitsForACommandAlreadySubmitted() throws Exception {
    CometdOnlyofficeService cometdService = new CometdOnlyofficeService(mock(EXoContinuationBayeux.class),
                                                                        mock(OnlyofficeEditorService.class),
                                                                        mock(RepositoryService.class));
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean ended = new AtomicBoolean();
    cometdService.eventsHandlers.submit(() -> {
      started.countDown();
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      ended.set(true);
    });
    assertTrue(started.await(5, TimeUnit.SECONDS));

    cometdService.stop();

    assertTrue(ended.get());
    assertTrue(cometdService.eventsHandlers.isTerminated());
  }

  @Test
  public void testAnEventIsPublishedWithNoSessionOfThisNodeSubscribed() throws Exception {
    CometdOnlyofficeService cometdService = new CometdOnlyofficeService(mock(EXoContinuationBayeux.class),
                                                                        mock(OnlyofficeEditorService.class),
                                                                        mock(RepositoryService.class));
    BayeuxServer bayeux = mock(BayeuxServer.class);
    ServerChannel channel = mock(ServerChannel.class);
    // No local subscriber: the channel does not exist until created
    when(bayeux.getChannel(anyString())).thenReturn(null);
    when(bayeux.createChannelIfAbsent(eq(CometdOnlyofficeService.CHANNEL_NAME + "doc1"))).thenReturn(new MarkedReference<>(channel,
                                                                                                                           true));
    Field bayeuxField = CometdOnlyofficeService.CometdService.class.getDeclaredField("bayeux");
    bayeuxField.setAccessible(true);
    bayeuxField.set(cometdService.service, bayeux);

    cometdService.service.publishDeletedEvent("doc1");

    verify(channel).publish(any(), anyString(), any());
  }

  @Test
  public void testAnEditorEventRelayedFromAnotherNodeIsLeftToThatNode() throws Exception {
    EXoContinuationBayeux exoBayeux = mock(EXoContinuationBayeux.class);
    CometdOnlyofficeService cometdService = new CometdOnlyofficeService(exoBayeux,
                                                                        mock(OnlyofficeEditorService.class),
                                                                        mock(RepositoryService.class));
    ServerSession oortSession = mock(ServerSession.class);
    when(exoBayeux.isRelayed(oortSession)).thenReturn(true);
    ServerMessage.Mutable relayed = mock(ServerMessage.Mutable.class);
    ServerMessage.Mutable published = mock(ServerMessage.Mutable.class);

    cometdService.service.subscribeDocuments(oortSession, relayed, "doc1");
    cometdService.service.subscribeDocuments(mock(ServerSession.class), published, "doc1");

    // Read, then handled, only on the node the editor published it to
    verify(relayed, never()).getData();
    verify(published).getData();
  }
}
