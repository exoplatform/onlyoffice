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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.onlyoffice.constant.DocumentServerOperation;
import org.exoplatform.services.log.Log;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The tokens exchanged with ONLYOFFICE Docs 10.0. The Document Server's own
 * tokens are the requests a 10.0.0.169 server sent with its default settings
 * ({@link DocumentServerFixture}): save callbacks, a document fetch and a
 * Document Builder script fetch. The variations a server does not send are
 * built by {@link #documentServerToken}, in the same shape.
 */
public class DocumentServerTokensTest {

  private static final String    SECRET     = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  private static final SecretKey KEY        = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  private static final String    DOC_KEY    = "a1b2c3d4e5f6";

  private static final String    CONTENT    = "http://exo.example.org/rest/onlyoffice/editor/content/john/" + DOC_KEY;

  private static final String[]  CALLBACKS  = { DocumentServerFixture.CALLBACK_EDITING, DocumentServerFixture.CALLBACK_CLOSED,
      DocumentServerFixture.CALLBACK_FORCESAVED, DocumentServerFixture.CALLBACK_SAVED };

  private final DocumentServerTokens tokens = new DocumentServerTokens(SECRET);

  /**
   * ONLYOFFICE Docs 10.0 puts the purpose at the root of its tokens, beside
   * {@code payload}, which holds the request.
   */
  @Test
  public void testTheCapturedTokensCarryTheirPurposeAtTheRoot() {
    Map<String, String> expected = Map.of(DocumentServerFixture.CALLBACK_EDITING, "callback",
                                          DocumentServerFixture.CALLBACK_CLOSED, "callback",
                                          DocumentServerFixture.CALLBACK_FORCESAVED, "callback",
                                          DocumentServerFixture.CALLBACK_SAVED, "callback",
                                          DocumentServerFixture.DOWNLOAD_DOCUMENT, "download",
                                          DocumentServerFixture.DOWNLOAD_BUILDER_SCRIPT, "download");
    expected.forEach((fixture, operation) -> {
      Map<String, Object> claims = DocumentServerFixture.load(fixture).claims();
      assertEquals(operation, claims.get("operation"), fixture);
      assertFalse(((Map<?, ?>) claims.get("payload")).containsKey("operation"), fixture);
    });
  }

  @Test
  public void testACapturedSaveCallbackIsAcceptedAtTheCallbackEndpointOnly() {
    for (String fixture : new String[] { DocumentServerFixture.CALLBACK_EDITING, DocumentServerFixture.CALLBACK_CLOSED }) {
      String token = DocumentServerFixture.load(fixture).callbackToken(DOC_KEY, "john", SECRET);

      assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK), fixture);
      assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD), fixture);
    }
  }

  @Test
  public void testACapturedDocumentFetchIsAcceptedAtTheContentEndpointOnly() {
    String token = DocumentServerFixture.load(DocumentServerFixture.DOWNLOAD_DOCUMENT).downloadToken(CONTENT, SECRET);

    assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
    assertFalse(tokens.validate(token, DOC_KEY, DocumentServerOperation.CALLBACK));
  }

  @Test
  public void testACapturedScriptFetchIsAcceptedAtTheContentEndpoint() {
    String scriptKey = "builder-script-key";
    String token = DocumentServerFixture.load(DocumentServerFixture.DOWNLOAD_BUILDER_SCRIPT)
                                        .downloadToken("http://10.0.0.12:8080/rest/onlyoffice/editor/content/john/" + scriptKey,
                                                       SECRET);

    assertTrue(tokens.validate(token, scriptKey, DocumentServerOperation.DOWNLOAD));
  }

  /**
   * ONLYOFFICE Docs 10.0 signs the whole callback: the body it posts is the
   * token's payload, plus the token itself.
   */
  @Test
  public void testTheCapturedCallbackBodyIsItsSignedPayload() {
    for (String fixture : CALLBACKS) {
      DocumentServerFixture callback = DocumentServerFixture.load(fixture);

      assertEquals(callback.claims().get("payload"), callback.body(), fixture);
    }
  }

  /**
   * Every callback ONLYOFFICE Docs 10.0 sent is accepted as it was sent: its
   * body, the token field the Document Server adds to it aside, is its signed
   * payload, the saves (status 2 and 6) and their history included. Without
   * that field, the body is accepted the same.
   */
  @Test
  public void testEveryCapturedCallbackIsAcceptedAsSigned() {
    for (String fixture : CALLBACKS) {
      DocumentServerFixture callback = DocumentServerFixture.load(fixture);
      String token = callback.callbackToken(DOC_KEY, "john", SECRET);

      assertTrue(tokens.validateCallback(token, DOC_KEY, callback.callbackBodyAsSent(DOC_KEY, "john", SECRET)), fixture);
      assertTrue(tokens.validateCallback(token, DOC_KEY, callback.callbackBody(DOC_KEY, "john")), fixture);
    }
  }

  /**
   * The addon acts on a callback's body only when the body is the payload the
   * Document Server signed for the document of the endpoint.
   */
  @Test
  public void testACallbackIsActedOnOnlyWhenItsBodyIsTheSignedPayload() {
    DocumentServerFixture captured = DocumentServerFixture.load(DocumentServerFixture.CALLBACK_EDITING);
    String token = captured.callbackToken(DOC_KEY, "john", SECRET);
    JSONObject body = new JSONObject(captured.callbackBody(DOC_KEY, "john"));

    assertTrue(tokens.validateCallback(token, DOC_KEY, body.toString()));
    assertTrue(tokens.validateCallback(token, DOC_KEY, new JSONObject(body.toString()).put("token", token).toString()),
               "the body's own token field is not part of the payload");

    assertFalse(tokens.validateCallback(token, DOC_KEY, new JSONObject(body.toString()).put("key", "another-key").toString()));
    assertFalse(tokens.validateCallback(token,
                                        DOC_KEY,
                                        new JSONObject(body.toString()).put("url", "http://attacker.example.org/x.docx").toString()));
    assertFalse(tokens.validateCallback(token, DOC_KEY, new JSONObject(body.toString()).put("status", 2).toString()));
    assertFalse(tokens.validateCallback(token, DOC_KEY, new JSONObject(body.toString()).put("users", List.of("mary")).toString()));
    assertFalse(tokens.validateCallback(token, DOC_KEY, "not json"));
    assertFalse(tokens.validateCallback(token, DOC_KEY, null));
    assertFalse(tokens.validateCallback(captured.callbackToken("another-key", "john", SECRET), DOC_KEY, body.toString()),
                "a callback signed for another document");
    assertFalse(tokens.validateCallback(captured.callbackToken("another-key", "john", SECRET),
                                        DOC_KEY,
                                        captured.callbackBody("another-key", "john")),
                "another document's callback, body and token alike, posted for this document");
    assertFalse(tokens.validateCallback(documentServerToken(Map.of("key", DOC_KEY), "download"), DOC_KEY, "{\"key\":\"" + DOC_KEY + "\"}"),
                "a download token");
  }

  /**
   * The status endpoint's check refuses what the token check refuses, even
   * when the body is the token's payload: a callback naming its document by
   * URL only, a token without purpose, an expired one, one signed with another
   * secret, and any token without a usable secret.
   */
  @Test
  public void testACallbackIsRefusedOnItsTokenWhateverItsBody() {
    String urlOnly = "{\"url\":\"" + CONTENT + "\"}";
    assertFalse(tokens.validateCallback(documentServerToken(Map.of("url", CONTENT), "callback"), DOC_KEY, urlOnly),
                "a callback naming its document by URL only");

    Map<String, Object> payload = callbackPayload(DOC_KEY);
    String body = new JSONObject(payload).toString();
    assertTrue(tokens.validateCallback(documentServerToken(payload, "callback"), DOC_KEY, body));
    assertFalse(tokens.validateCallback(Jwts.builder().claim("payload", payload).expiration(inFiveMinutes()).signWith(KEY).compact(),
                                        DOC_KEY,
                                        body),
                "a token without purpose");
    assertFalse(tokens.validateCallback(Jwts.builder()
                                            .claim("payload", payload)
                                            .claim("operation", "callback")
                                            .expiration(new Date(System.currentTimeMillis() - 60_000))
                                            .signWith(KEY)
                                            .compact(),
                                        DOC_KEY,
                                        body),
                "an expired token");
    SecretKey other = Keys.hmacShaKeyFor("WRONG-SECRET-KEY-hwExSQoSe97tw8gyYNhqnM1biHb".getBytes(StandardCharsets.UTF_8));
    assertFalse(tokens.validateCallback(Jwts.builder()
                                            .claim("payload", payload)
                                            .claim("operation", "callback")
                                            .expiration(inFiveMinutes())
                                            .signWith(other)
                                            .compact(),
                                        DOC_KEY,
                                        body),
                "a token signed with another secret");
    for (String secret : new String[] { null, "" }) {
      assertFalse(new DocumentServerTokens(secret).validateCallback(documentServerToken(payload, "callback"), DOC_KEY, body),
                  "no usable secret");
    }
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

  /**
   * A token without {@code exp} is refused, though signed, for its purpose and
   * its key: ONLYOFFICE Docs 10.0 sets one on every token it signs.
   */
  @Test
  public void testATokenWithoutExpiryIsRefused() {
    Map<String, Object> payload = callbackPayload(DOC_KEY);
    String callback = Jwts.builder().claim("payload", payload).claim("operation", "callback").signWith(KEY).compact();
    String download = Jwts.builder().claim("payload", Map.of("key", DOC_KEY)).claim("operation", "download").signWith(KEY).compact();

    assertFalse(tokens.validateCallback(callback, DOC_KEY, new JSONObject(payload).toString()));
    assertFalse(tokens.validate(download, DOC_KEY, DocumentServerOperation.DOWNLOAD));
    assertTrue(tokens.validate(documentServerToken(Map.of("key", DOC_KEY), "download"), DOC_KEY, DocumentServerOperation.DOWNLOAD));
  }

  /**
   * A token signed with an expiry carries it, and is refused once it passed.
   */
  @Test
  public void testATokenSignedWithAnExpiryCarriesIt() {
    Date expiresAt = new Date((System.currentTimeMillis() / 1000 + 120) * 1000);
    String token = tokens.sign(Map.of("payload", Map.of("key", DOC_KEY)), DocumentServerOperation.DOWNLOAD, expiresAt);

    assertEquals(expiresAt, Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload().getExpiration());
    assertTrue(tokens.validate(token, DOC_KEY, DocumentServerOperation.DOWNLOAD));
    String expired = tokens.sign(Map.of("payload", Map.of("key", DOC_KEY)),
                                 DocumentServerOperation.DOWNLOAD,
                                 new Date(System.currentTimeMillis() - 60_000));
    assertFalse(tokens.validate(expired, DOC_KEY, DocumentServerOperation.DOWNLOAD));
  }

  /**
   * A field the Document Server signs as {@code null}, at the root or nested,
   * is accepted when the body carries the same {@code null}; the body must
   * still equal the signed payload.
   */
  @Test
  public void testACallbackWithANullFieldIsAcceptedAsSigned() {
    Map<String, Object> payload = callbackPayload(DOC_KEY);
    payload.put("status", 3);
    payload.put("url", null);
    Map<String, Object> history = new LinkedHashMap<>();
    history.put("serverVersion", null);
    payload.put("history", history);
    String token = documentServerToken(payload, "callback");
    String body = "{\"key\":\"" + DOC_KEY + "\",\"status\":3,\"users\":[\"john\"],"
        + "\"actions\":[{\"type\":1,\"userid\":\"john\"}],\"url\":null,\"history\":{\"serverVersion\":null}}";

    assertTrue(tokens.validateCallback(token, DOC_KEY, body));
    assertFalse(tokens.validateCallback(token, DOC_KEY, body.replace("\"status\":3", "\"status\":2")));
    assertFalse(tokens.validateCallback(token, DOC_KEY, body.replace("\"url\":null", "\"url\":\"http://evil.example.org/x\"")));
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

  /**
   * A missing or too short secret is reported once, as an ERROR naming the
   * property; a usable one is not.
   */
  @Test
  public void testAnUnusableSecretIsReportedAsAnError() {
    for (String secret : new String[] { null, "", "0123456789012345678901234567890" }) {
      Log log = mock(Log.class);

      new DocumentServerTokens(secret, log);

      ArgumentCaptor<Object> problem = ArgumentCaptor.forClass(Object.class);
      verify(log).error(eq("ONLYOFFICE Document Server misconfigured: {}"), problem.capture());
      assertTrue(String.valueOf(problem.getValue()).contains("onlyoffice.documentserver.secret"), String.valueOf(problem.getValue()));
    }
    Log log = mock(Log.class);
    new DocumentServerTokens(SECRET, log);
    verifyNoInteractions(log);
  }

  /**
   * A refused token is logged by its document key and the reason, never with
   * the token itself.
   */
  @Test
  public void testARefusedTokenIsNotLogged() {
    Log log = mock(Log.class);
    DocumentServerTokens logged = new DocumentServerTokens(SECRET, log);
    String download = documentServerToken(Map.of("key", DOC_KEY), "download");
    String forged = download.substring(0, download.length() - 4) + "AAAA";

    assertFalse(logged.validate(download, DOC_KEY, DocumentServerOperation.CALLBACK));
    assertFalse(logged.validate(forged, DOC_KEY, DocumentServerOperation.DOWNLOAD));

    String everything = mockingDetails(log).getInvocations()
                                           .stream()
                                           .map(invocation -> invocation.getMethod().getName() + Arrays.deepToString(invocation.getArguments()))
                                           .toList()
                                           .toString();
    assertTrue(everything.contains("debug") && everything.contains(DOC_KEY), everything);
    assertFalse(everything.contains(download), everything);
    assertFalse(everything.contains(forged), everything);
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
