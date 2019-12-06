package org.exoplatform.onlyoffice;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.exoplatform.onlyoffice.test.AbstractResourceTest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The Class EditorServiceTest.
 */

public class EditorServiceTest extends AbstractResourceTest {

  /** The Constant RESOURCE_URL. */
  protected static final String RESOURCE_URL     = "/onlyoffice/editor";

  /** The Constant USER. */
  protected static final String USER             = "john";

  /** The Constant SECRET_KEY. */
  protected static final String SECRET_KEY       = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  /** The Constant WRONG_SECRET_KEY. */
  protected static final String WRONG_SECRET_KEY = "WRONG-SECRET-KEY-94037466gKfv37jvfdG43";

  /** The key. */
  protected String              key;

  /** The content payload. */
  protected Map<String, Object> contentPayload   = new HashMap<>();

  /** The status payload. */
  protected Map<String, Object> statusPayload    = new HashMap<>();

  /** The status payload json. */
  protected String              statusPayloadJson;

  /**
   * Before class.
   */
  @Override
  protected void beforeClass() {
    super.beforeClass();
    if (key == null) {
      try {
        startSessionAs(USER);
        key = createTestDocument(USER, "abc.docx", "Testing Content");
        contentPayload.put("key", key);

        statusPayload.put("key", key);
        statusPayload.put("satus", 1);
        statusPayload.put("url", "localhost");
        statusPayload.put("error", 0);
        statusPayloadJson = "{ \"key\": \"" + key + "\", \"status\": 1, \"url\": \"localhost\", \"error\": 0 }";
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Test content.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContent() throws Exception {
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", contentPayload)
                       .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                       .compact();

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + USER + "/" + key, "", headers, null);
    String content = IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8.name());
    
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals("Testing Content", content);
  }

  /**
   * Test content wrong token.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContentWrongToken() throws Exception {
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", contentPayload)
                       .signWith(Keys.hmacShaKeyFor(WRONG_SECRET_KEY.getBytes()))
                       .compact();

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + USER + "/" + key, "", headers, null);
    
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  /**
   * Test content no token.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContentNoToken() throws Exception {
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + USER + "/" + key, "", null, null);
    
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  /**
   * Test status.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStatus() throws Exception {
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", statusPayload)
                       .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                       .compact();

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + USER + "/" + key,
                                         "",
                                         headers,
                                         statusPayloadJson.getBytes());

    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals("{\"error\": 0}", response.getEntity());
  }

  /**
   * Test status wrong token.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStatusWrongToken() throws Exception {
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", statusPayload)
                       .signWith(Keys.hmacShaKeyFor(WRONG_SECRET_KEY.getBytes()))
                       .compact();
    
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + USER + "/" + key,
                                         "",
                                         headers,
                                         statusPayloadJson.getBytes());
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

  /**
   * Test status no token.
   *
   * @throws Exception the exception
   */
  @Test
  public void testStatusNoToken() throws Exception {
    ContainerResponse response = service("POST",
                                         RESOURCE_URL + "/status/" + USER + "/" + key,
                                         "",
                                         null,
                                         statusPayloadJson.getBytes());
    assertNotNull(response);
    assertEquals(401, response.getStatus());
    assertEquals("{\"error\":\"The token is not valid\"}", response.getEntity());
  }

}
