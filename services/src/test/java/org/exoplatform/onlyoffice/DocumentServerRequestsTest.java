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
package org.exoplatform.onlyoffice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKey;
import javax.jcr.Node;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.onlyoffice.constant.DocumentServerOperation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Each request the addon sends to ONLYOFFICE Docs carries a token signed for
 * its service only: ONLYOFFICE Docs 10.0 refuses a token whose
 * {@code operation} claim names another service. The Document Server is a
 * local HTTP server answering as a 10.0.0.169 server answered the same
 * requests.
 */
public class DocumentServerRequestsTest {

  private static final String       SECRET   = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  private static final SecretKey    KEY      = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  private static final byte[]       OUTPUT   = { 80, 75, 3, 4 };

  private final List<Request>       requests = new CopyOnWriteArrayList<>();

  private HttpServer                server;

  private String                    base;

  private record Request(String path, String authorization, String body) {
  }

  @BeforeEach
  public void startDocumentServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    server.createContext(OnlyofficeEditorServiceImpl.COMMAND_SERVICE_PATH, exchange -> answer(exchange, "{\"key\":\"k\",\"error\":0}"));
    server.createContext("/converter", exchange -> answer(exchange, "{\"fileUrl\":\"" + base + "/output\",\"percent\":100,\"endConvert\":true}"));
    server.createContext("/docbuilder", exchange -> answer(exchange, "{\"key\":\"bld_1\",\"urls\":{\"result.docx\":\"" + base + "/output\"},\"end\":true}"));
    server.createContext("/output", exchange -> {
      exchange.sendResponseHeaders(200, OUTPUT.length);
      exchange.getResponseBody().write(OUTPUT);
      exchange.close();
    });
    server.start();
  }

  @AfterEach
  public void stopDocumentServer() {
    server.stop(0);
  }

  @Test
  public void testAForceSaveIsACommandSignedForTheCommandService() throws Exception {
    service(SECRET).forceSave("john", "doc-key", true, false, false, "a comment");

    Request request = single(OnlyofficeEditorServiceImpl.COMMAND_SERVICE_PATH);
    Claims claims = claimsOf(request.authorization());
    assertEquals("command", claims.get("operation"));
    assertEquals("forcesave", claims.get("c"));
    assertEquals("doc-key", claims.get("key"));
    assertEquals("exo-onlyoffice", claims.getSubject());
    assertEquals("john", new JSONObject((String) claims.get("userdata")).getString("userId"));
    assertEquals("forcesave", new JSONObject(request.body()).getString("c"));
  }

  @Test
  public void testAConversionIsSignedForTheConversionService() throws Exception {
    OnlyofficeEditorServiceImpl service = service(SECRET);
    Node node = mock(Node.class);
    Config config = mock(Config.class);
    Config.Document document = mock(Config.Document.class);
    when(node.getPath()).thenReturn("/Users/john/Documents/report.docx");
    when(config.getDocument()).thenReturn(document);
    when(document.getKey()).thenReturn("conversion-key");
    when(document.getUrl()).thenReturn("http://exo.example.org/rest/onlyoffice/editor/content/john/conversion-key");
    doReturn(config).when(service).createConfigForConversion(any(), any());

    byte[] converted = service.convertNodeContent(node, "pdf", "docx", "john");

    assertArrayEquals(OUTPUT, converted);
    Request request = single("/converter");
    Claims claims = claimsOf(request.authorization());
    assertEquals("converter", claims.get("operation"));
    assertEquals("conversion-key", claims.get("key"));
    assertEquals("docx", claims.get("filetype"));
    assertEquals("pdf", claims.get("outputtype"));
    assertEquals("http://exo.example.org/rest/onlyoffice/editor/content/john/conversion-key", claims.get("url"));
    assertEquals("conversion-key", new JSONObject(request.body()).getString("key"));
  }

  @Test
  public void testADocumentBuilderRunIsSignedForTheDocumentBuilderService() throws Exception {
    String scriptUrl = "http://10.0.0.12:8080/rest/onlyoffice/editor/content/john/script-key";

    byte[] built = service(SECRET).postDocBuilder(scriptUrl, "result.docx");

    assertArrayEquals(OUTPUT, built);
    Request request = single("/docbuilder");
    Claims claims = claimsOf(request.authorization());
    assertEquals("docbuilder", claims.get("operation"));
    assertEquals(scriptUrl, claims.get("url"));
    JSONObject body = new JSONObject(request.body());
    assertEquals(scriptUrl, body.getString("url"));
    assertEquals("Bearer " + body.getString("token"), request.authorization());
  }

  /**
   * Without a secret nothing reaches the Document Server, and the Document
   * Builder's caller is told why.
   */
  @Test
  public void testNothingIsSentWithoutASecret() throws Exception {
    OnlyofficeEditorServiceImpl service = service("");

    service.forceSave("john", "doc-key", true, false, false, null);
    IllegalStateException error = assertThrows(IllegalStateException.class,
                                               () -> service.postDocBuilder(base + "/script", "result.docx"));

    assertTrue(error.getMessage().contains("onlyoffice.documentserver.secret"), error.getMessage());
    assertEquals(List.of(), requests);
  }

  /**
   * The Document Builder fetches the files a script opens without an
   * Authorization header, so the addon signs each file's URL itself, as a
   * download of that file only.
   */
  @Test
  public void testAScriptAssetUrlCarriesADownloadTokenForThatAsset() throws Exception {
    String platformUrl = System.getProperty("exo.base.url");
    System.setProperty("exo.base.url", "https://platform.example.org/");
    try {
      OnlyofficeEditorServiceImpl service = service(SECRET);
      doReturn(false).when(service).isCluster();
      doReturn(OUTPUT).when(service).postDocBuilder(any(), any());

      service.runDocBuilderScript("builder.OpenFile(\"@@ASSET:in.docx@@\");", Map.of("in.docx", OUTPUT), "docx", "out.docx", "john");

      ArgumentCaptor<byte[]> staged = ArgumentCaptor.forClass(byte[].class);
      ArgumentCaptor<Long> expiresAt = ArgumentCaptor.forClass(Long.class);
      verify(service, times(2)).stageDocBuilderAsset(staged.capture(), any(), expiresAt.capture());
      String script = new String(staged.getAllValues().get(1), StandardCharsets.UTF_8);
      Matcher assetUrl = Pattern.compile("/content/john/([^?\"]+)\\?token=([^\"]+)").matcher(script);
      assertTrue(assetUrl.find(), script);
      DocumentServerTokens tokens = new DocumentServerTokens(SECRET);
      assertTrue(tokens.validate(assetUrl.group(2), assetUrl.group(1), DocumentServerOperation.DOWNLOAD));
      assertFalse(tokens.validate(assetUrl.group(2), assetUrl.group(1), DocumentServerOperation.CALLBACK));
      // the token expires with the staged asset
      Date tokenExpiry = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(assetUrl.group(2)).getPayload().getExpiration();
      assertNotNull(tokenExpiry);
      assertEquals(expiresAt.getAllValues().get(0) / 1000, tokenExpiry.getTime() / 1000);
    } finally {
      if (platformUrl == null) {
        System.clearProperty("exo.base.url");
      } else {
        System.setProperty("exo.base.url", platformUrl);
      }
    }
  }

  private Request single(String path) {
    assertEquals(1, requests.size(), requests.toString());
    assertEquals(path, requests.get(0).path());
    return requests.get(0);
  }

  private static Claims claimsOf(String authorization) {
    assertTrue(authorization != null && authorization.startsWith("Bearer "), String.valueOf(authorization));
    return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(authorization.substring(7)).getPayload();
  }

  private void answer(HttpExchange exchange, String response) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    requests.add(new Request(exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization"), body));
    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  /**
   * @return the service's own request methods, with no container behind it,
   *         pointed at the local Document Server
   */
  private OnlyofficeEditorServiceImpl service(String secret) throws Exception {
    OnlyofficeEditorServiceImpl service = mock(OnlyofficeEditorServiceImpl.class, CALLS_REAL_METHODS);
    set(service, "documentServerTokens", new DocumentServerTokens(secret));
    set(service, "commandServiceUrl", base + OnlyofficeEditorServiceImpl.COMMAND_SERVICE_PATH);
    set(service, "convertUrl", base + "/converter");
    set(service, "docbuilderUrl", base + "/docbuilder");
    set(service, "docBuilderAssets", new ConcurrentHashMap<>());
    return service;
  }

  private static void set(Object target, String name, Object value) throws Exception {
    Field field = OnlyofficeEditorServiceImpl.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
