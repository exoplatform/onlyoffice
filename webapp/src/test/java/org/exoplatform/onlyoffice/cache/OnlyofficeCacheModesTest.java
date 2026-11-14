/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.onlyoffice.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * In a cluster, the caches of the add-on are invalidated, as a shared store holds what they cache. The viewer
 * configurations are the one exception: no store keeps them, so the other nodes could not load them and the cache is
 * replicated, one of the few caches of the platform kept replicated for state no store holds. This pins that choice, so
 * that no later change flips it, nor replicates another cache of the add-on.
 */
class OnlyofficeCacheModesTest
{
   private static final String CONFIGURATION = "src/main/webapp/WEB-INF/conf/onlyoffice/configuration.xml";

   private static final String VIEWER_CACHE = "onlyoffice.ViewerCache";

   @Test
   void testTheViewerCacheIsReplicated() throws Exception
   {
      Map<String, String> modes = getClusterModes();

      assertEquals("replication", modes.get(VIEWER_CACHE));
   }

   @Test
   void testNoOtherCacheIsReplicated() throws Exception
   {
      Map<String, String> modes = getClusterModes();

      assertTrue(modes.size() > 1, modes.toString());
      modes.forEach((name, mode) -> {
         if (!VIEWER_CACHE.equals(name))
         {
            assertFalse(mode.toLowerCase().endsWith("replication"), name + " is " + mode);
         }
      });
   }

   /**
    * @return the default mode of each cache in the cluster profile, by cache name
    */
   private static Map<String, String> getClusterModes() throws Exception
   {
      File file = new File(System.getProperty("basedir", "."), CONFIGURATION);
      NodeList fields = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("field");
      Map<String, String> modes = new TreeMap<>();
      for (int i = 0; i < fields.getLength(); i++)
      {
         Element field = (Element)fields.item(i);
         String profiles = field.getAttribute("profiles");
         if ("cacheMode".equals(field.getAttribute("name")) && (profiles.isEmpty() || profiles.contains("cluster")))
         {
            modes.put(getCacheName(field), getDefaultValue(field.getElementsByTagName("string").item(0).getTextContent()));
         }
      }
      return modes;
   }

   private static String getCacheName(Element field)
   {
      Node node = field.getParentNode();
      while (!"object-param".equals(node.getNodeName()))
      {
         node = node.getParentNode();
      }
      return ((Element)node).getElementsByTagName("name").item(0).getTextContent().trim();
   }

   private static String getDefaultValue(String value)
   {
      String trimmed = value.trim();
      int separator = trimmed.indexOf(':');
      return trimmed.startsWith("${") && separator > 0 ? trimmed.substring(separator + 1, trimmed.length() - 1) : trimmed;
   }
}
