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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.json.JSONException;
import org.json.JSONObject;

import org.exoplatform.onlyoffice.constant.DocumentServerOperation;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Signs the tokens the addon sends to ONLYOFFICE Docs and validates the tokens
 * the Document Server sends back, with the shared secret. Every token carries
 * its purpose in the root {@code operation} claim
 * ({@link DocumentServerOperation}): ONLYOFFICE Docs 10.0 refuses a token sent
 * to a service of another purpose, and this class refuses a token received on
 * an endpoint of another purpose. Without a usable secret nothing is signed and
 * every received token is refused.
 */
public class DocumentServerTokens {

  /** The minimum length of the secret, in bytes: HMAC-SHA256 needs 256 bits. */
  public static final int MIN_SECRET_BYTES = 32;

  private static final Log LOG = ExoLogger.getLogger(DocumentServerTokens.class);

  private static final String PAYLOAD_CLAIM = "payload";

  private static final String KEY_CLAIM = "key";

  private static final String URL_CLAIM = "url";

  private static final String TOKEN_FIELD = "token";

  private final SecretKey signingKey;

  private final String secretProblem;

  private final Log log;

  /**
   * Reports a missing or too short secret as an ERROR, once.
   *
   * @param secret the secret shared with the Document Server, may be null
   */
  public DocumentServerTokens(String secret) {
    this(secret, LOG);
  }

  DocumentServerTokens(String secret, Log log) {
    this.log = log;
    this.secretProblem = secretProblem(secret);
    this.signingKey = secretProblem == null ? Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)) : null;
    if (secretProblem != null) {
      log.error("ONLYOFFICE Document Server misconfigured: {}", secretProblem);
    }
  }

  /**
   * @return why the secret cannot be used, or null when it can
   */
  public String getSecretProblem() {
    return secretProblem;
  }

  /**
   * Signs a token for a request to the Document Server.
   *
   * @param claims the claims of the request, left unchanged
   * @param operation the service the token is for
   * @return the signed token
   * @throws IllegalStateException when the secret is missing or too short
   */
  public String sign(Map<String, ?> claims, DocumentServerOperation operation) {
    if (signingKey == null) {
      throw new IllegalStateException("Cannot sign a request to the Document Server: " + secretProblem);
    }
    Map<String, Object> signed = new LinkedHashMap<>(claims);
    signed.put(DocumentServerOperation.CLAIM, operation.getClaimValue());
    return Jwts.builder().claims(signed).signWith(signingKey).compact();
  }

  /**
   * Validates a token the Document Server sent to an addon endpoint.
   *
   * @param token the token, may be null
   * @param key the document key the endpoint was called for, may be null
   * @param expected the purpose of the endpoint: {@link DocumentServerOperation#CALLBACK}
   *          or {@link DocumentServerOperation#DOWNLOAD}
   * @return true when the token is signed with the secret, unexpired, carries
   *         the expected purpose and is for that key: for a callback its
   *         {@code payload.key} equals the key; for a download its
   *         {@code payload.key} equals the key or its {@code payload.url} ends
   *         with it
   */
  public boolean validate(String token, String key, DocumentServerOperation expected) {
    Map<?, ?> payload = payload(token, key, expected);
    if (payload == null) {
      return false;
    }
    if (payload.containsKey(KEY_CLAIM)) {
      return key.equals(String.valueOf(payload.get(KEY_CLAIM)));
    }
    return expected == DocumentServerOperation.DOWNLOAD && payload.containsKey(URL_CLAIM)
        && String.valueOf(payload.get(URL_CLAIM)).endsWith(key);
  }

  /**
   * Validates a save or status callback: its token, and the callback the
   * addon acts on. ONLYOFFICE Docs signs the whole callback in the token's
   * {@code payload}, so the body must be that payload, its own {@code token}
   * field aside.
   *
   * @param token the token of the Authorization header, may be null
   * @param key the document key the endpoint was called for, may be null
   * @param body the callback body, may be null
   * @return true when the token is a valid callback token whose
   *         {@code payload.key} equals the key and whose payload equals the
   *         body
   */
  public boolean validateCallback(String token, String key, String body) {
    Map<?, ?> payload = payload(token, key, DocumentServerOperation.CALLBACK);
    if (payload == null || body == null || !key.equals(String.valueOf(payload.get(KEY_CLAIM)))) {
      return false;
    }
    JSONObject callback;
    try {
      callback = new JSONObject(body);
    } catch (JSONException e) {
      log.debug("Document Server callback refused for key {}: the body is not a JSON object", key);
      return false;
    }
    callback.remove(TOKEN_FIELD);
    if (!new JSONObject(payload).similar(callback)) {
      log.debug("Document Server callback refused for key {}: the body differs from the signed payload", key);
      return false;
    }
    return true;
  }

  /**
   * @return the token's payload when the token is signed with the secret,
   *         unexpired and carries the expected purpose; null otherwise
   */
  private Map<?, ?> payload(String token, String key, DocumentServerOperation expected) {
    if (signingKey == null || token == null || key == null) {
      return null;
    }
    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    } catch (RuntimeException e) {
      log.debug("Document Server token refused for key {}: {}", key, e.getMessage());
      return null;
    }
    if (!expected.getClaimValue().equals(claims.get(DocumentServerOperation.CLAIM))) {
      log.debug("Document Server token refused for key {}: operation {} where {} is expected",
                key,
                claims.get(DocumentServerOperation.CLAIM),
                expected.getClaimValue());
      return null;
    }
    return claims.get(PAYLOAD_CLAIM) instanceof Map<?, ?> payload ? payload : null;
  }


  static String secretProblem(String secret) {
    if (secret == null || secret.trim().isEmpty()) {
      return "the property onlyoffice.documentserver.secret is empty. ONLYOFFICE Docs 10.0 refuses unsigned requests, "
          + "so the editor, conversions and the Document Builder do not work, and every Document Server callback is refused, "
          + "until it is set to the secret configured on the Document Server";
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      return "the property onlyoffice.documentserver.secret is shorter than " + MIN_SECRET_BYTES
          + " bytes, which HMAC-SHA256 tokens need. The editor, conversions and the Document Builder do not work, "
          + "and every Document Server callback is refused, until a longer secret is set on both sides";
    }
    return null;
  }
}
