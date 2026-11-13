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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    assertEquals("http://10.0.0.12:8080", OnlyofficeEditorServiceImpl.getDocBuilderBaseUrl());
  }

  @Test
  public void testTheAssetsAreFetchedAtThePlatformUrlOtherwise() {
    System.clearProperty(OnlyofficeEditorServiceImpl.DOCBUILDER_NODE_URL_PROPERTY);

    assertEquals("https://platform.example.org", OnlyofficeEditorServiceImpl.getDocBuilderBaseUrl());
  }

  private static void restore(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }
}
