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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import org.exoplatform.onlyoffice.constant.DocumentServerOperation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The tokens exchanged with ONLYOFFICE Docs 10.0. The claim shapes of the
 * Document Server's own tokens are copied from requests a 10.0.0.169 server
 * sent with its default settings: a save callback, a document fetch and a
 * Document Builder script fetch, each carrying its purpose in the root
 * {@code operation} claim and its subject in {@code payload}.
 */
public class DocumentServerTokensTest {

  private static final String    SECRET     = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  private static final SecretKey KEY        = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  private static final String    DOC_KEY    = "a1b2c3d4e5f6";

  private static final String    CONTENT    = "http://exo.example.org/rest/onlyoffice/editor/content/john/" + DOC_KEY;

  private final DocumentServerTokens tokens = new DocumentServerTokens(SECRET);

  @Test
  public void testACapturedSaveCallbackIsAcceptedAtTheCallbackEndpointOnly() {
    String token = documentServerToken(callbackPayload(DOC_KEY), "callback");

    assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
  }

  @Test
  public void testACapturedDocumentFetchIsAcceptedAtTheContentEndpointOnly() {
    String token = documentServerToken(Map.of("url", CONTENT), "download");

    assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testACapturedScriptFetchIsAcceptedAtTheContentEndpoint() {
    String scriptKey = "builder-script-key";
    String token = documentServerToken(Map.of("url",
                                              "http://10.0.0.12:8080/rest/onlyoffice/editor/content/john/" + scriptKey),
                                       "download");

    assertTrue(tokens.validate(token, scriptKey, DocumentServerOperation.DOWNLOAD));
  }

  /**
   * A download token naming the document by its key, as the addon's own asset
   * tokens do, matches the key and still cannot trigger a save: the purpose
   * alone refuses it.
   */
  @Test
  public void testADownloadTokenForTheSameKeyIsRefusedAsACallback() {
    String token = documentServerToken(Map.of("key", DOC_KEY), "download");

    assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  /**
   * A callback names its document in {@code payload.key}; a callback token
   * matching only by a URL ending with the key is refused.
   */
  @Test
  public void testACallbackMatchingOnlyByUrlIsRefused() {
    String token = documentServerToken(Map.of("url", CONTENT), "callback");

    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testATokenWithoutPurposeIsRefusedAtBothEndpoints() {
    String token = Jwts.builder().claim("payload", callbackPayload(DOC_KEY)).expiration(inFiveMinutes()).signWith(KEY).compact();

    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
  }

  /**
   * ONLYOFFICE Docs puts the purpose at the root of the token, beside
   * {@code payload}; a purpose inside {@code payload} is not one.
   */
  @Test
  public void testAPurposeInsideThePayloadIsNotRead() {
    Map<String, Object> payload = new LinkedHashMap<>(callbackPayload(DOC_KEY));
    payload.put("operation", "callback");
    String token = Jwts.builder().claim("payload", payload).expiration(inFiveMinutes()).signWith(KEY).compact();

    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testATokenForAnotherKeyIsRefused() {
    assertFalse(tokens.validate(documentServerToken(callbackPayload("another-key"), "callback"),
                                DOC_KEY,
                                DocumentServerOperation.CALLBACK));
    assertFalse(tokens.validate(documentServerToken(Map.of("url", CONTENT + "-other"), "download"),
                                DOC_KEY,
                                DocumentServerOperation.DOWNLOAD));
  }

  @Test
  public void testATokenSignedWithAnotherSecretIsRefused() {
    SecretKey other = Keys.hmacShaKeyFor("WRONG-SECRET-KEY-hwExSQoSe97tw8gyYNhqnM1biHb".getBytes(StandardCharsets.UTF_8));
    String token = Jwts.builder()
                       .claim("payload", callbackPayload(DOC_KEY))
                       .claim("operation", "callback")
                       .expiration(inFiveMinutes())
                       .signWith(other)
                       .compact();

    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testAnExpiredTokenIsRefused() {
    String token = Jwts.builder()
                       .claim("payload", callbackPayload(DOC_KEY))
                       .claim("operation", "callback")
                       .expiration(new Date(System.currentTimeMillis() - 60_000))
                       .signWith(KEY)
                       .compact();

    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testAMissingTokenOrKeyIsRefused() {
    String token = documentServerToken(callbackPayload(DOC_KEY), "callback");

    assertFalse(tokens.validate(null, DOC_KEY, DocumentServerOperation.CALLBACK));
    assertFalse(tokens.validate(token, null, DocumentServerOperation.CALLBACK));
    assertFalse(tokens.validate("not-a-token", DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  /**
   * Without a secret every token is refused, an unsigned one included, and
   * nothing is signed.
   */
  @Test
  public void testAnEmptySecretRefusesEveryTokenAndSignsNothing() {
    for (String secret : new String[] { null, "", "   " }) {
      DocumentServerTokens unconfigured = new DocumentServerTokens(secret);
      String unsigned = Jwts.builder().claim("payload", callbackPayload(DOC_KEY)).claim("operation", "callback").compact();

      assertNotNull(unconfigured.getSecretProblem());
      assertTrue(unconfigured.getSecretProblem().contains("is empty"), unconfigured.getSecretProblem());
      assertFalse(unconfigured.validate(unsigned, DOC_KEY, DocumentServerOperation.CALLBACK));
      assertFalse(unconfigured.validate(documentServerToken(callbackPayload(DOC_KEY), "callback"),
                                        DOC_KEY,
                                        DocumentServerOperation.CALLBACK));
      assertThrows(IllegalStateException.class, () -> unconfigured.sign(Map.of("c", "version"), DocumentServerOperation.COMMAND));
    }
  }

  /**
   * HMAC-SHA256 needs a 256-bit key: a 31-byte secret is reported, a 32-byte
   * one is used.
   */
  @Test
  public void testASecretShorterThan32BytesIsReported() {
    DocumentServerTokens shortSecret = new DocumentServerTokens("0123456789012345678901234567890");

    assertNotNull(shortSecret.getSecretProblem());
    assertTrue(shortSecret.getSecretProblem().contains("shorter than 32 bytes"), shortSecret.getSecretProblem());
    assertThrows(IllegalStateException.class, () -> shortSecret.sign(Map.of("c", "version"), DocumentServerOperation.COMMAND));

    DocumentServerTokens longEnough = new DocumentServerTokens("01234567890123456789012345678901");
    assertNull(longEnough.getSecretProblem());
    assertNull(tokens.getSecretProblem());
  }

  @Test
  public void testASignedTokenCarriesItsPurposeAtTheRootAndTheRequestClaims() {
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("c", "forcesave");
    claims.put("key", DOC_KEY);

    String token = tokens.sign(claims, DocumentServerOperation.COMMAND);

    Claims signed = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
    assertEquals("command", signed.get("operation"));
    assertEquals("forcesave", signed.get("c"));
    assertEquals(DOC_KEY, signed.get("key"));
    assertFalse(claims.containsKey("operation"), "the caller's claims are left unchanged");
  }

  private static Map<String, Object> callbackPayload(String key) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("key", key);
    payload.put("status", 1);
    payload.put("users", List.of("john"));
    payload.put("actions", List.of(Map.of("type", 1, "userid", "john")));
    return payload;
  }

  /**
   * @return a token shaped as ONLYOFFICE Docs 10.0 signs it: the request in
   *         {@code payload}, the purpose at the root, a five-minute expiry
   */
  private static String documentServerToken(Map<String, ?> payload, String operation) {
    return Jwts.builder()
               .claim("payload", payload)
               .claim("operation", operation)
               .issuedAt(new Date())
               .expiration(inFiveMinutes())
               .signWith(KEY)
               .compact();
  }

  private static Date inFiveMinutes() {
    return new Date(System.currentTimeMillis() + 300_000);
  }
}
