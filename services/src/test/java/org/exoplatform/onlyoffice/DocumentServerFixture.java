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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.ObjectMapper;

/**
 * A request ONLYOFFICE Docs 10.0 sent, captured on a real server and kept in
 * {@code src/test/resources/documentserver-10.0}: its token claims verbatim
 * and, for a callback, its body. A test re-signs the claims with its own
 * secret and fresh times, since the captured token expired five minutes after
 * it was sent, and names its own document: the key of a callback, the URL of
 * a download, every other claim staying as captured.
 */
public final class DocumentServerFixture {

  /** A save callback, status 1: a user opened the document. */
  public static final String CALLBACK_EDITING        = "callback-editing.json";

  /** A save callback, status 4: the document was closed without changes. */
  public static final String CALLBACK_CLOSED         = "callback-closed.json";

  /** The fetch of the document of an editing session. */
  public static final String DOWNLOAD_DOCUMENT       = "download-document.json";

  /** The Document Builder's fetch of its script. */
  public static final String DOWNLOAD_BUILDER_SCRIPT = "download-builder-script.json";

  private static final ObjectMapper MAPPER         = new ObjectMapper();

  private final Map<String, Object> claims;

  private final Map<String, Object> body;

  private DocumentServerFixture(Map<String, Object> claims, Map<String, Object> body) {
    this.claims = claims;
    this.body = body;
  }

  /**
   * @param name the fixture file
   * @return the captured request
   */
  @SuppressWarnings("unchecked")
  public static DocumentServerFixture load(String name) {
    try (InputStream in = DocumentServerFixture.class.getResourceAsStream("/documentserver-10.0/" + name)) {
      if (in == null) {
        throw new IllegalArgumentException("No Document Server fixture " + name);
      }
      Map<String, Object> fixture = MAPPER.readValue(in, Map.class);
      return new DocumentServerFixture((Map<String, Object>) fixture.get("claims"), (Map<String, Object>) fixture.get("body"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * @return the captured claims, the payload included
   */
  public Map<String, Object> claims() {
    return copy(claims);
  }

  /**
   * @param key the document key
   * @param secret the secret to sign with
   * @return the captured callback token, for that key
   */
  public String callbackToken(String key, String secret) {
    return sign(withPayload("key", key), secret);
  }

  /**
   * @param key the document key
   * @param userId the user the callback names, in place of the captured one
   * @return the captured callback body, for that key and user
   */
  @SuppressWarnings("unchecked")
  public String callbackBody(String key, String userId) {
    Map<String, Object> named = new LinkedHashMap<>(body);
    named.put("key", key);
    if (named.containsKey("users")) {
      named.put("users", List.of(userId));
    }
    if (named.get("actions") instanceof List<?> actions) {
      named.put("actions", actions.stream().map(action -> {
        Map<String, Object> renamed = new LinkedHashMap<>((Map<String, Object>) action);
        renamed.put("userid", userId);
        return renamed;
      }).toList());
    }
    return MAPPER.writeValueAsString(named);
  }

  /**
   * @param url the URL of the file fetched
   * @param secret the secret to sign with
   * @return the captured download token, for that URL
   */
  public String downloadToken(String url, String secret) {
    return sign(withPayload("url", url), secret);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> withPayload(String name, String value) {
    Map<String, Object> named = copy(claims);
    Map<String, Object> payload = new LinkedHashMap<>((Map<String, Object>) named.get("payload"));
    if (!payload.containsKey(name)) {
      throw new IllegalStateException("The captured payload has no " + name + ": " + payload);
    }
    payload.put(name, value);
    named.put("payload", payload);
    return named;
  }

  private static String sign(Map<String, Object> claims, String secret) {
    Map<String, Object> fresh = new LinkedHashMap<>(claims);
    fresh.remove("iat");
    fresh.remove("exp");
    long now = System.currentTimeMillis();
    return Jwts.builder()
               .claims(fresh)
               .issuedAt(new Date(now))
               .expiration(new Date(now + 300_000))
               .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
               .compact();
  }

  private static Map<String, Object> copy(Map<String, Object> claims) {
    return new LinkedHashMap<>(claims);
  }
}
