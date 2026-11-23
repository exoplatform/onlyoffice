package org.exoplatform.onlyoffice;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.onlyoffice.test.AbstractResourceTest;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The Class EditorServiceTest.
 */

public class EditorServiceTest extends AbstractResourceTest {

  /** The Constant LOG. */
  protected static final Log    LOG              = ExoLogger.getLogger(EditorServiceTest.class);

  /** The Constant RESOURCE_URL. */
  protected static final String RESOURCE_URL     = "/onlyoffice/editor";

  /** The Constant USER. */
  protected static final String USER             = "john";

  /** A user other than the test's. */
  protected static final String OTHER_USER       = "mary";

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
        ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
        String containerName = PortalContainer.getCurrentPortalContainerName();
        try {
          ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
          ExoContainerContext.setCurrentContainer(exoContainer);
          RequestLifeCycle.begin(exoContainer);
          LOG.info("Life cycle of {} container started.", containerName);
          key = createTestDocument(USER, "abc.docx", "Testing Content");
          contentPayload.put("key", key);
          statusPayload.put("key", key);
          statusPayload.put("satus", 1);
          statusPayload.put("url", "localhost");
          statusPayload.put("error", 0);
          statusPayloadJson = "{ \"key\": \"" + key + "\", \"status\": 1, \"url\": \"localhost\", \"error\": 0 }";
        } finally {
          RequestLifeCycle.end();
          ExoContainerContext.setCurrentContainer(contextContainer);
          LOG.info("Life cycle of {} container ended.", containerName);
        }
      } catch (Exception e) {
        LOG.error("Cannot prepare status payload", e);
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
    assertEquals("private, no-store", response.getHttpHeaders().getFirst("Cache-Control"));
  }

  /**
   * The viewer configuration of a document is cached by no one, nor is any
   * answer refusing it, and a document the request may not view is answered as
   * not found, with no detail.
   *
   * @throws Exception the exception
   */
  @Test
  public void testViewerConfigIsNotCached() throws Exception {
    startSessionAs(USER);
    Node node = session.getRootNode().addNode("viewed.docx", "nt:file");
    Node contentNode = node.addNode("jcr:content", "nt:unstructured");
    contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
    contentNode.setProperty("jcr:data", "Viewed Content");
    session.save();
    String docId = onlyofficeEditorService.initDocument(node);
    String workspace = session.getWorkspace().getName();
    try {
      ContainerResponse viewer = service("GET", RESOURCE_URL + "/viewer/" + workspace + "/" + docId, "", null, null);
      ContainerResponse unknown = service("GET", RESOURCE_URL + "/viewer/" + workspace + "/unknown-document", "", null, null);
      ContainerResponse failed = service("GET", RESOURCE_URL + "/viewer/unknown-workspace/" + docId, "", null, null);

      assertEquals(500, failed.getStatus());
      assertEquals("private, no-store", failed.getHttpHeaders().getFirst("Cache-Control"));
      assertEquals(200, viewer.getStatus());
      assertEquals("private, no-store", viewer.getHttpHeaders().getFirst("Cache-Control"));
      assertEquals(404, unknown.getStatus());
      assertEquals("{\"error\":\"Document not found\"}", unknown.getEntity());
      assertEquals("private, no-store", unknown.getHttpHeaders().getFirst("Cache-Control"));
    } finally {
      node.remove();
      session.save();
    }
  }

  /**
   * The Document Server's fetch of the content of a document its user may no
   * longer view is answered as not found, with no detail.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContentOfARemovedDocumentIsNotFound() throws Exception {
    startSessionAs(USER);
    String removedKey = createTestDocument(USER, "removed.docx", "Removed Content");
    session.getRootNode().getNode("removed.docx").remove();
    session.save();
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", Map.of("key", removedKey))
                       .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                       .compact();

    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + USER + "/" + removedKey, "", headers, null);

    assertEquals(404, response.getStatus());
    assertEquals("{\"error\":\"Document not found\"}", response.getEntity());
    assertEquals("private, no-store", response.getHttpHeaders().getFirst("Cache-Control"));
  }

  /**
   * The Document Server's fetch of the content of a document its user may no
   * longer read is answered as not found, with no detail.
   *
   * @throws Exception the exception
   */
  @Test
  public void testContentOfADocumentItsUserMayNoLongerReadIsNotFound() throws Exception {
    startSessionAs(USER);
    String withdrawnKey = createTestDocument(USER, "withdrawn.docx", "Withdrawn Content");
    Session systemSession = systemSession("portal-test");
    NodeImpl node = (NodeImpl) systemSession.getRootNode().getNode("withdrawn.docx");
    node.addMixin("exo:privilegeable");
    systemSession.save();
    node.setPermissions(Map.of(OTHER_USER, PermissionType.ALL));
    node.save();
    String token = Jwts.builder()
                       .setSubject("exo-onlyoffice")
                       .claim("payload", Map.of("key", withdrawnKey))
                       .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                       .compact();
    MultivaluedMap<String, String> headers = new MultivaluedMapImpl();
    headers.putSingle("authorization", "Bearer " + token);
    try {
      ContainerResponse response = service("GET", RESOURCE_URL + "/content/" + USER + "/" + withdrawnKey, "", headers, null);

      assertEquals(404, response.getStatus());
      assertEquals("{\"error\":\"Document not found\"}", response.getEntity());
      assertEquals("private, no-store", response.getHttpHeaders().getFirst("Cache-Control"));
    } finally {
      removeAsSystem(node);
    }
  }

  /**
   * The viewer configuration of a document the request may not read is
   * answered as not found, with nothing of the document in the answer.
   *
   * @throws Exception the exception
   */
  @Test
  public void testViewerConfigOfADocumentTheRequestMayNotReadIsNotFound() throws Exception {
    startSessionAs(USER);
    NodeImpl node = createDocumentAsSystem("portal-test", "", "unreadable.docx", Map.of(OTHER_USER, PermissionType.ALL));
    try {
      ContainerResponse response = service("GET", RESOURCE_URL + "/viewer/portal-test/" + node.getUUID(), "", null, null);

      assertEquals(404, response.getStatus());
      assertEquals("{\"error\":\"Document not found\"}", response.getEntity());
      assertEquals("private, no-store", response.getHttpHeaders().getFirst("Cache-Control"));
    } finally {
      removeAsSystem(node);
    }
  }

  private Session systemSession(String workspace) throws Exception {
    return SessionProviderService.getSystemSessionProvider().getSession(workspace, SessionProviderService.getRepository());
  }

  /**
   * A document created in a system session under a folder of a workspace,
   * whose own permissions are the only ones.
   */
  private NodeImpl createDocumentAsSystem(String workspace,
                                          String folderPath,
                                          String title,
                                          Map<String, String[]> permissions) throws Exception {
    Session systemSession = systemSession(workspace);
    Node folder = systemSession.getRootNode();
    for (String segment : folderPath.split("/")) {
      if (!segment.isEmpty()) {
        folder = folder.hasNode(segment) ? folder.getNode(segment) : folder.addNode(segment, "nt:folder");
      }
    }
    NodeImpl node = (NodeImpl) folder.addNode(title, "nt:file");
    node.addMixin("exo:privilegeable");
    node.addMixin("mix:referenceable");
    Node contentNode = node.addNode("jcr:content", "nt:unstructured");
    contentNode.setProperty("jcr:mimeType", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
    contentNode.setProperty("jcr:data", "Content");
    systemSession.save();
    node.setPermissions(permissions);
    node.save();
    return node;
  }

  private void removeAsSystem(NodeImpl node) throws Exception {
    Session systemSession = systemSession(node.getSession().getWorkspace().getName());
    systemSession.getNodeByUUID(node.getUUID()).remove();
    systemSession.save();
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
    assertEquals("private, no-store", response.getHttpHeaders().getFirst("Cache-Control"));
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
