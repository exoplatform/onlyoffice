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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.Node;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.onlyoffice.constant.DocumentServerOperation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The addon's requests against a real ONLYOFFICE Docs server, with its default
 * token settings. Skipped unless the server is named; run with
 * {@code mvn -pl services verify -Prun-its -Donlyoffice.it.documentserver=http://localhost:8088
 * -Donlyoffice.it.host=<address the Document Server reaches this JVM at>} and
 * the shared secret in the {@code ONLYOFFICE_IT_SECRET} environment variable.
 * Each Document Server fetch of a file is checked against the addon's own
 * validation of download tokens.
 */
public class DocumentServerIT {

  private static final String DOCUMENT_SERVER = System.getProperty("onlyoffice.it.documentserver");

  private static final String HOST            = System.getProperty("onlyoffice.it.host");

  private static final String SECRET          = System.getenv("ONLYOFFICE_IT_SECRET");

  private final List<String>  refusedFetches  = new CopyOnWriteArrayList<>();

  private final List<String>  commandAnswers  = new CopyOnWriteArrayList<>();

  private DocumentServerTokens tokens;

  private HttpServer          server;

  private String              base;

  @BeforeEach
  public void start() throws IOException {
    assumeTrue(DOCUMENT_SERVER != null && HOST != null && SECRET != null,
               "set onlyoffice.it.documentserver, onlyoffice.it.host and ONLYOFFICE_IT_SECRET to run");
    tokens = new DocumentServerTokens(SECRET);
    server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
    base = "http://" + HOST + ":" + server.getAddress().getPort();
    byte[] docx;
    try (InputStream in = getClass().getResourceAsStream("/files/template.docx")) {
      assertNotNull(in, "the template document is on the classpath");
      docx = in.readAllBytes();
    }
    server.createContext("/content/john/doc-key", exchange -> serve(exchange, "doc-key", docx));
    server.createContext("/content/john/script-key",
                         exchange -> serve(exchange,
                                           "script-key",
                                           ("builder.CreateFile(\"docx\");\nbuilder.SaveFile(\"docx\", \"result.docx\");\n"
                                               + "builder.CloseFile();\n").getBytes(StandardCharsets.UTF_8)));
    server.createContext("/command", this::forwardCommand);
    server.start();
  }

  @AfterEach
  public void stop() {
    if (server != null) {
      server.stop(0);
    }
  }

  /**
   * The command service authenticates a force-save and answers on the key:
   * error 1, no such document, rather than a refused token.
   */
  @Test
  public void testTheCommandServiceAcceptsAForceSave() throws Exception {
    service().forceSave("john", "unknown-key-" + System.currentTimeMillis(), true, false, false, null);

    assertEquals(1, commandAnswers.size(), commandAnswers.toString());
    assertEquals(1, new JSONObject(commandAnswers.get(0)).getInt("error"), commandAnswers.get(0));
  }

  @Test
  public void testTheConversionServiceConvertsADocument() throws Exception {
    OnlyofficeEditorServiceImpl service = service();
    Node node = mock(Node.class);
    Config config = mock(Config.class);
    Config.Document document = mock(Config.Document.class);
    when(node.getPath()).thenReturn("/Users/john/Documents/template.docx");
    when(config.getDocument()).thenReturn(document);
    when(document.getKey()).thenReturn("doc-key-" + System.currentTimeMillis());
    when(document.getUrl()).thenReturn(base + "/content/john/doc-key");
    doReturn(config).when(service).createConfigForConversion(any(), any());

    byte[] pdf = service.convertNodeContent(node, "pdf", "docx", "john");

    assertNotNull(pdf, "the Document Server converted the document");
    assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    assertTrue(refusedFetches.isEmpty(), refusedFetches.toString());
  }

  @Test
  public void testTheDocumentBuilderRunsAScript() throws Exception {
    byte[] built = service().postDocBuilder(base + "/content/john/script-key", "result.docx");

    assertNotNull(built);
    assertEquals("PK", new String(built, 0, 2, StandardCharsets.US_ASCII));
    assertTrue(refusedFetches.isEmpty(), refusedFetches.toString());
  }

  /**
   * Serves a file the Document Server fetches, if the addon's content endpoint
   * would accept its token, and records the fetch otherwise.
   */
  private void serve(HttpExchange exchange, String key, byte[] data) throws IOException {
    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
    String token = authorization == null ? null : authorization.replace("Bearer", "").trim();
    if (!tokens.validate(token, key, DocumentServerOperation.DOWNLOAD)) {
      refusedFetches.add(exchange.getRequestURI().getPath());
      exchange.sendResponseHeaders(401, -1);
      exchange.close();
      return;
    }
    exchange.sendResponseHeaders(200, data.length);
    exchange.getResponseBody().write(data);
    exchange.close();
  }

  /**
   * Forwards the addon's command request to the Document Server unchanged and
   * records its answer, which the addon only logs.
   */
  private void forwardCommand(HttpExchange exchange) throws IOException {
    byte[] body = exchange.getRequestBody().readAllBytes();
    HttpURLConnection connection = (HttpURLConnection) URI.create(DOCUMENT_SERVER + "/command").toURL().openConnection();
    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setRequestProperty("Authorization", exchange.getRequestHeaders().getFirst("Authorization"));
    connection.getOutputStream().write(body);
    byte[] answer;
    try (InputStream in = connection.getInputStream()) {
      answer = in.readAllBytes();
    }
    commandAnswers.add(new String(answer, StandardCharsets.UTF_8));
    exchange.sendResponseHeaders(connection.getResponseCode(), answer.length);
    exchange.getResponseBody().write(answer);
    exchange.close();
  }

  private OnlyofficeEditorServiceImpl service() throws Exception {
    OnlyofficeEditorServiceImpl service = mock(OnlyofficeEditorServiceImpl.class, CALLS_REAL_METHODS);
    set(service, "documentServerTokens", tokens);
    set(service, "commandServiceUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/command");
    set(service, "convertUrl", DOCUMENT_SERVER + "/converter");
    set(service, "docbuilderUrl", DOCUMENT_SERVER + "/docbuilder");
    set(service, "docBuilderAssets", new ConcurrentHashMap<>());
    return service;
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = OnlyofficeEditorServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
