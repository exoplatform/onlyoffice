package org.exoplatform.onlyoffice;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class OnlyofficeEditorServiceTest extends BaseCommonsTestCase {

  /** The Constant LOG. */
  protected static final Log        LOG        = ExoLogger.getLogger(OnlyofficeEditorServiceTest.class);

  /** The Constant SECRET_KEY. */
  protected static final String     SECRET_KEY = "1fRW5pBZu3UIBEdebbpDpKJ4hwExSQoSe97tw8gyYNhqnM1biHb";

  protected OnlyofficeEditorService editorService;

  protected SessionProviderService  sessionProviderService;

    /**
     * Before class.
     */
    @Override
    public void beforeClass() {
        super.beforeClass();
        ExoContainerContext.setCurrentContainer(container);
        this.editorService = getContainer().getComponentInstanceOfType(OnlyofficeEditorService.class);
        this.sessionProviderService = getContainer().getComponentInstanceOfType(SessionProviderService.class);
    }

    /**
     * Test add file preferences
     */
    @Test
    public void testAddFilePreferences() throws Exception {
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        assertEquals(node.getMixinNodeTypes().length, 6);
        editorService.addFilePreferences(node, "john", "path");
        assertEquals(node.getNode("eoo:preferences").getProperties().getSize(), 1);
        assertEquals(node.getMixinNodeTypes().length, 7);
        node.remove();
    }

    /**
     * Test create editor
     */
    @Test
    public void testCreateEditor() throws Exception {
      startSessionAs("john");
      Node node = session.getRootNode().addNode("Test Document.docx", "nt:file");
      node.addMixin("mix:referenceable");
      Node contentNode = node.addNode("jcr:content", "nt:unstructured");
      contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      contentNode.setProperty("jcr:data", "testContent");
      session.save();

      String docId = node.getUUID();
      Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, docId);
      String editorURL = "http://127.0.0.1:8080/portal/intranet/oeditor?docId=" + docId;

      assertNotNull(config);

      assertTrue(config.isCreated());
      assertFalse(config.isClosing());
      assertFalse(config.isOpen());
      assertFalse(config.isClosed());
      assertNull(config.getError());
      assertEquals(docId, config.getDocId());
      assertEquals(editorURL, config.getEditorUrl());
      assertEquals("/Test Document.docx", config.getPath());

      assertNotNull(config.getDocument());
      assertEquals("Test Document.docx", config.getDocument().getTitle());
      assertEquals("docx", config.getDocument().getFileType());
      assertEquals("john", config.getDocument().getInfo().getOwner());

      assertNotNull(config.getEditorConfig());
      assertNotNull(config.getEditorConfig().getUser());
      assertEquals("John Smith", config.getEditorConfig().getUser().getName());
      node.remove();
      session.save();
    }

    /**
     * Test create editor for incorrect file
     */
    @Test
    public void testCreateEditorIncorrectFile() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:base", "testContent");
        try {
            editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
        } catch (OnlyofficeEditorException e) {
            // Ok
            node.remove();
            session.save();
            return;
        }
        // Fail if the exception wasn't thrown
        fail();
    }

     /**
     * Test force save
     * No test for output
     */
    @Test
    public void testForceSave() throws Exception {
        startSessionAs("john");
        editorService.forceSave("john", "key", false, false, true, "testForceSave");
    }

    /**
     * Test get content
     */
    @Test
    public void testGetContent() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

        DocumentContent documentContent = editorService.getContent("john", config.getDocument().getKey());

        assertNotNull(documentContent);
        String content = IOUtils.toString(documentContent.getData(), "UTF-8");
        assertEquals("testContent", content);
        assertEquals("application/vnd.oasis.opendocument.text", documentContent.getType());
        node.remove();
    }

    /**
     * Test get document with workspace and path
     */
    @Test
    public void testGetDocument() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        Node nodeDocument = editorService.getDocument(null, node.getPath());
        assertNotNull(nodeDocument);
        node.remove();
    }

    /**
     * Test get user from exoCache with key and userId
     */
    @Test
    public void testGetUser() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
        Config.Editor.User user = editorService.getUser(node.getUUID(), "john");
        assertNotNull(user);
        assertEquals(user.getName(), "John Smith");
    }

    /**
     * Test is document mime supported
     */
    @Test
    public void testIsDocumentMimeSupported() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        assertTrue(editorService.isDocumentMimeSupported(node));
        node.getNode("jcr:content").setProperty("jcr:mimeType", "text/plain");
        session.save();
        assertFalse(editorService.isDocumentMimeSupported(node));
        Node unsctructuredNode = session.getRootNode().addNode("Test Document.docx", "nt:unstructured");
        session.save();
        assertTrue(editorService.isDocumentMimeSupported(unsctructuredNode));
        node.remove();
        unsctructuredNode.remove();
    }

    /**
     * Test update title for not root node
     */
    @Test
    public void testUpdateTitle() throws Exception {
        startSessionAs("john");
        Node node = createDocumentNotRootNode("Test Document.docx", "nt:file", "testContent");
        editorService.updateTitle("portal-test", node.getUUID(), "TestDocumentJohn.docx", "john");
        assertTrue(node.getParent().hasNode("TestDocumentJohn.docx"));
        node.remove();
    }

    /**
     * Test update title for root node ("/")
     */
    @Test
    public void testUpdateTitleRootNode() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        editorService.updateTitle("portal-test", node.getUUID(), "TestDocumentJohn.docx", "john");
        assertTrue(node.getParent().hasNode("TestDocumentJohn.docx"));
        node.remove();
    }

    /**
     * Test set last modifier
     */
    @Test
    public void testSetLastModifier() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
        editorService.setLastModifier(node.getUUID(),"john");
        assertNotSame(config.getEditorConfig().getUser().lastModified.toString(), "0");
        node.remove();
    }
    /**
     * Test user joined and leaved
     */
    @Test
    public void testUserJoinedAndLeaved() throws Exception {
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
        DocumentStatus status = new DocumentStatus.Builder().status(1L)
                                                            .users(new String[]{"john"})
                                                            .userId("john")
                                                            .key(config.getDocument().getKey())
                                                            .build();
        assertFalse(config.isOpen());
        editorService.updateDocument(status);
        assertTrue(config.isOpen());
        assertFalse(config.isClosed());
        status = new DocumentStatus.Builder().status(4L)
                                             .userId("john")
                                             .users(new String[]{})
                                             .key(config.getDocument().getKey())
                                             .build();
        editorService.updateDocument(status);
        assertFalse(config.isOpen());
        assertTrue(config.isClosed());
        node.remove();
    }

    /**
     * Test validate token
     */
    @Test
    public void testValidateToken() throws Exception {
        startSessionAs("john");
        Node node = createDocument("Test Document.docx", "nt:file", "testContent");
        Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
        String key = config.getDocument().getKey();
        Map<String, Object> payload = new HashMap<>();

        payload.put("key", key);

        String token = Jwts.builder()
                           .setSubject("exo-onlyoffice")
                           .claim("payload", payload)
                           .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                           .compact();

        String wrongSignToken = Jwts.builder()
                                    .setSubject("exo-onlyoffice")
                                    .claim("payload", payload)
                                    .signWith(Keys.hmacShaKeyFor("WRONG-SECRET-KEY-hwExSQoSe97tw8gyYNhqnM1biHb".getBytes()))
                                    .compact();

        payload.replace("key", "wrong-document-key");
        String wrongKeyToken = Jwts.builder()
                                   .setSubject("exo-onlyoffice")
                                   .claim("payload", payload)
                                   .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                                   .compact();

        assertTrue(editorService.validateToken(token, key));

        assertFalse(editorService.validateToken(wrongSignToken, key));
        assertFalse(editorService.validateToken(wrongKeyToken, key));

        payload.clear();
        // URL should end with key
        payload.put("url", "http://127.0.0.1:8080/editor/" + key);

        token = Jwts.builder()
                    .setSubject("exo-onlyoffice")
                    .claim("payload", payload)
                    .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                    .compact();
        assertTrue(editorService.validateToken(token, key));
        node.remove();
    }


  /*
  @Test
  public void testEditedAndClosed() throws Exception {
    startSessionAs("john");
    NodeImpl node = createDocument("Test Document.docx", "nt:file", "testContent");
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    // Open a document
    DocumentStatus status = new DocumentStatus.Builder().status(1L)
                                                        .users(new String[] { "john" })
                                                        .key(config.getDocument().getKey())
                                                        .build();
    editorService.updateDocument("john", status);

    // Edited doc
    editorService.setLastModifier(config.getDocument().getKey(), "john");

    status = new DocumentStatus.Builder().status(2L)
        .users(new String[] { "john" })
        .key(config.getDocument().getKey())
        .config(config)
        .url("http://www.mocky.io/v2/5d1e04da300000369dd72483")
        .build();
    editorService.updateDocument("john", status);
    InputStream dataStream = node.getNode("jcr:content").getProperty("jcr:data").getStream();
    String data = IOUtils.toString(dataStream, "UTF-8");
    assertEquals("Updated Content", data);
  }


  @Test
  public void testCanEditDocument() throws Exception {
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent");
    assertTrue(editorService.canEditDocument(node));
    node.lock(true, false);
    assertFalse(editorService.canEditDocument(node));
    node.remove();
  }
  */


    /**
     * create document for not root node
     */
    protected NodeImpl createDocumentNotRootNode(String title, String type, String data) throws Exception {
        NodeImpl rootNode = (NodeImpl) session.getRootNode();
        rootNode.setPermission("john", new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY});
        NodeImpl node = (NodeImpl) rootNode.addNode("parent", "nt:folder").addNode(title, type);
        node.addMixin("mix:lockable");
        node.addMixin("mix:referenceable");
        node.addMixin("exo:privilegeable");
        node.addMixin("exo:datetime");
        node.addMixin("exo:modify");
        node.addMixin("exo:sortable");
        node.setProperty("exo:lastModifier", "john");
        node.setProperty("exo:lastModifiedDate", Calendar.getInstance());
        if (type.equals("nt:file")) {
            Node contentNode = node.addNode("jcr:content", "nt:unstructured");
            contentNode.addMixin("exo:datetime");
            contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            contentNode.setProperty("jcr:data", data);
            contentNode.setProperty("exo:dateCreated", Calendar.getInstance());
            contentNode.setProperty("exo:dateModified", Calendar.getInstance());
        }

        rootNode.save();
        return node;
    }


    /* create document on root node parent("/") */
    protected NodeImpl createDocument(String title, String type, String data) throws Exception {
        NodeImpl rootNode = (NodeImpl) session.getRootNode();
        rootNode.setPermission("john", new String[]{PermissionType.ADD_NODE, PermissionType.SET_PROPERTY});
        NodeImpl node = (NodeImpl) rootNode.addNode(title, type);
        node.addMixin("mix:lockable");
        node.addMixin("mix:referenceable");
        node.addMixin("exo:privilegeable");
        node.addMixin("exo:datetime");
        node.addMixin("exo:modify");
        node.addMixin("exo:sortable");
        node.setProperty("exo:lastModifier", "john");
        node.setProperty("exo:lastModifiedDate", Calendar.getInstance());
        if (type.equals("nt:file")) {
            Node contentNode = node.addNode("jcr:content", "nt:unstructured");
            contentNode.addMixin("exo:datetime");
            contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            contentNode.setProperty("jcr:data", data);
            contentNode.setProperty("exo:dateCreated", Calendar.getInstance());
            contentNode.setProperty("exo:dateModified", Calendar.getInstance());
        }

        rootNode.save();
        return node;
    }


    protected void startSessionAs(String user) throws Exception {
        HashSet<MembershipEntry> memberships = new HashSet<MembershipEntry>();
        memberships.add(new MembershipEntry("/platform/administrators"));
        Identity identity = new Identity(user, memberships);
        ConversationState state = new ConversationState(identity);
        state.setAttribute(ConversationState.SUBJECT, identity.getSubject());
        ConversationState.setCurrent(state);
        SessionProvider provider = new SessionProvider(state);
        sessionProviderService.setSessionProvider(null, provider);
        session = provider.getSession(WORKSPACE_NAME, repositoryService.getCurrentRepository());
    }



}
