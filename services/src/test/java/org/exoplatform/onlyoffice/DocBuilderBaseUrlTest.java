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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * The assets of a Document Builder run stay in the memory of the node that
 * staged them, so the Document Server fetches them at that node's own URL.
 */
public class DocBuilderBaseUrlTest {

  private String baseUrl;

  private String nodeUrl;

  @Before
  public void saveProperties() {
    baseUrl = System.getProperty("exo.base.url");
    nodeUrl = System.getProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY);
    System.setProperty("exo.base.url", "https://platform.example.org/");
  }

  @After
  public void restoreProperties() {
    restore("exo.base.url", baseUrl);
    restore(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY, nodeUrl);
  }

  @Test
  public void testTheAssetsAreFetchedAtTheNodeUrlWhenSet() {
    System.setProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY, "http://10.0.0.12:8080/");

    assertEquals("http://10.0.0.12:8080", OnlyofficeEditorServiceImpl.getDocBuilderBaseUrl(true));
  }

  @Test
  public void testTheAssetsAreFetchedAtThePlatformUrlOutsideACluster() {
    System.clearProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY);

    assertEquals("https://platform.example.org", OnlyofficeEditorServiceImpl.getDocBuilderBaseUrl(false));
  }

  @Test
  public void testARunFailsInAClusterWhenTheNodeUrlIsNotSet() throws Exception {
    System.clearProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY);
    OnlyofficeEditorServiceImpl service = service(true);

    IllegalStateException error = assertThrows(IllegalStateException.class,
                                               () -> service.runDocBuilderScript("builder.CreateFile(\"docx\");",
                                                                                 null,
                                                                                 "docx",
                                                                                 "result.docx",
                                                                                 "john"));
    assertTrue(error.getMessage().contains(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY));
  }

  @Test
  public void testARunHandsTheDocumentServerTheNodeUrl() throws Exception {
    System.setProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY, "http://10.0.0.12:8080/");
    OnlyofficeEditorServiceImpl service = service(true);
    ArgumentCaptor<String> scriptUrl = ArgumentCaptor.forClass(String.class);
    doReturn(new byte[] { 1 }).when(service).postDocBuilder(scriptUrl.capture(), any());

    service.runDocBuilderScript("builder.CreateFile(\"docx\");", null, "docx", "result.docx", "john");

    verify(service).postDocBuilder(any(), any());
    assertTrue(scriptUrl.getValue(), scriptUrl.getValue().startsWith("http://10.0.0.12:8080/"));
  }

  /**
   * @param cluster whether the service runs as a cluster
   * @return the service's own methods, with no container behind it
   */
  private static OnlyofficeEditorServiceImpl service(boolean cluster) throws Exception {
    OnlyofficeEditorServiceImpl service = mock(OnlyofficeEditorServiceImpl.class, CALLS_REAL_METHODS);
    Field assets = OnlyofficeEditorServiceImpl.class.getDeclaredField("docBuilderAssets");
    assets.setAccessible(true);
    assets.set(service, new ConcurrentHashMap<>());
    doReturn(cluster).when(service).isCluster();
    return service;
  }

  private static void restore(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }
}
