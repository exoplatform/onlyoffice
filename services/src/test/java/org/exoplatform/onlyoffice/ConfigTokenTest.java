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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The token of the editor-opening config is signed only with a secret
 * {@link DocumentServerTokens} can use: with a missing or too short one, the
 * config carries no token rather than failing each opening.
 */
public class ConfigTokenTest {

  private static final String SECRET = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  @Test
  public void testAnUnusableSecretIssuesNoEditorToken() {
    for (String secret : new String[] { null, "", "   ", "0123456789012345678901234567890" }) {
      Config config = editor(secret).build();

      assertNull(config.getToken(), "secret: " + secret);
      assertNull(config.forUser("mary", "Mary", "en", secret).getToken(), "secret: " + secret);
    }
  }

  @Test
  public void testAUsableSecretSignsTheEditorToken() {
    Config config = editor(SECRET).build();

    assertEquals("exo-onlyoffice", claims(config.getToken()).getSubject());
    assertNotNull(claims(config.getToken()).get("document"));
    assertEquals("exo-onlyoffice", claims(config.forUser("mary", "Mary", "en", SECRET).getToken()).getSubject());
  }

  private static Config.Builder editor(String secret) {
    return Config.editor("http://documentserver.example.org", "word", "collaboration", "/doc.docx", "doc-id")
                 .generateUrls("http://exo.example.org/rest")
                 .key("a1b2c3d4e5f6")
                 .title("doc.docx")
                 .fileType("docx")
                 .userId("john")
                 .userName("John")
                 .lang("en")
                 .mode("edit")
                 .secret(secret);
  }

  private static Claims claims(String token) {
    return Jwts.parser()
               .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
               .build()
               .parseSignedClaims(token)
               .getPayload();
  }
}
