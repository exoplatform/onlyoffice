package org.exoplatform.onlyoffice;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.*;

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

  protected Session                 session;

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
    // Given
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    String[] initialMixinNodeTypes = ((NodeImpl) node).getMixinTypeNames();
    Boolean nodePreference = node.hasNode("eoo:preferences");

    // When
    editorService.addFilePreferences(node, "john", "path");

    // Then
    assertFalse(ArrayUtils.contains(initialMixinNodeTypes, "eoo:onlyofficeFile"));
    assertFalse(nodePreference);

    String[] newMixinNodeTypes = ((NodeImpl) node).getMixinTypeNames();
    assertTrue(ArrayUtils.contains(newMixinNodeTypes, "eoo:onlyofficeFile"));
    assertTrue(node.hasNode("eoo:preferences"));
    assertTrue(node.getNode("eoo:preferences").hasNode("john"));
    assertTrue(node.getNode("eoo:preferences").getNode("john").hasProperty("path"));
    assertEquals(node.getNode("eoo:preferences").getNode("john").getProperty("path").getValue().getString(), "path");
    node.remove();
  }

  /* create document */
  protected NodeImpl createDocument(String title, String type, String data, Boolean createAtRoot) throws Exception {
    startSessionAs("root");
    NodeImpl rootNode = (NodeImpl) session.getRootNode();
    rootNode.setPermission("john", new String[] { PermissionType.ADD_NODE, PermissionType.SET_PROPERTY });

    NodeImpl node = createAtRoot ? (NodeImpl) rootNode.addNode(title, type)
                                 : (NodeImpl) rootNode.addNode("Users", "nt:folder").addNode(title, type);
    node.addMixin("exo:privilegeable");
    node.addMixin("exo:activityInfo");
    node.addMixin("exo:datetime");
    node.addMixin("exo:modify");
    node.addMixin("exo:sortable");
    node.setProperty("exo:activityId", Calendar.getInstance());
    node.setProperty("exo:dateModified", Calendar.getInstance());
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
    node.addMixin("mix:referenceable");
    node.addMixin("mix:versionable");
    rootNode.getSession().save();

    rootNode.getSession().save();
    return node;
  }

  /**
   * Test add listener
   */
  @Test
  public void testCallListenerOnCreateWhenAddingListener() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    AtomicBoolean onCreateCalled = new AtomicBoolean(false);
    OnlyofficeEditorListener listener = new OnlyofficeEditorListener() {
      @Override
      public void onCreate(DocumentStatus status) {
        onCreateCalled.set(true);
      }

      @Override
      public void onGet(DocumentStatus status) {

      }

      @Override
      public void onJoined(DocumentStatus status) {

      }

      @Override
      public void onLeaved(DocumentStatus status) {

      }

      @Override
      public void onSaved(DocumentStatus status) {

      }

      @Override
      public void onError(DocumentStatus status) {

      }
    };
    editorService.addListener(listener);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // Then
    assertTrue(onCreateCalled.get());
    node.remove();
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
    SessionProvider systemSessionProvider = SessionProviderService.getSystemSessionProvider();
    session = systemSessionProvider.getSession("portal-test", SessionProviderService.getRepository());
  }

  /**
   * Test remove listener
   */
  @Test
  public void testCallListenerOnCreateWhenRemovingListener() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    AtomicBoolean onCreateCalled = new AtomicBoolean(false);

    OnlyofficeEditorListener listener = new OnlyofficeEditorListener() {
      @Override
      public void onCreate(DocumentStatus status) {
        onCreateCalled.set(true);
      }

      @Override
      public void onGet(DocumentStatus status) {

      }

      @Override
      public void onJoined(DocumentStatus status) {

      }

      @Override
      public void onLeaved(DocumentStatus status) {

      }

      @Override
      public void onSaved(DocumentStatus status) {

      }

      @Override
      public void onError(DocumentStatus status) {

      }
    };
    editorService.addListener(listener);
    editorService.removeListener(listener);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // Then
    assertFalse(onCreateCalled.get());
    node.remove();
  }

  /**
   * Test create document when drive data not null User's documents
   */
  @Test
  public void testCreateDocumentWhenUserDataNotNull() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", false);

    // When
    editorService.addFilePreferences(node, "john", node.getPath());
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // Then
    String docId = node.getUUID();
    String editorURL = "http://127.0.0.1:8080/portal/intranet/oeditor?docId=" + docId;
    assertNotNull(config);
    assertTrue(node.getPath().startsWith("/Users"));
    assertTrue(config.getPath().endsWith("/Test Document.docx"));
    assertTrue(config.isCreated());
    assertFalse(config.isClosing());
    assertFalse(config.isOpen());
    assertFalse(config.isClosed());
    assertNull(config.getError());
    assertEquals(docId, config.getDocId());
    assertEquals(editorURL, config.getEditorUrl());

    assertNotNull(config.getDocument());
    assertEquals("Test Document.docx", config.getDocument().getTitle());
    assertEquals("docx", config.getDocument().getFileType());

    assertNotNull(config.getEditorConfig());
    assertNotNull(config.getEditorConfig().getUser());
    node.remove();
  }

  /**
   * Test create editor for incorrect file
   */
  @Test
  public void testCreateEditorIncorrectFile() throws Exception {
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:base", "testContent", true);
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
   * Test create editor Copy editor for this user from another entry in the
   * configs
   */
  @Test
  public void testCreateEditorWhenCopyEditorForUserFromAnotherEntry() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // When

    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "Root", null, node.getUUID());

    // Then
    String docId = node.getUUID();
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
    assertNotNull(config.getEditorConfig());
    assertNotNull(config.getEditorConfig().getUser());
    node.remove();
  }

  /**
   * Test create new editor config and document key
   */
  @Test
  public void testCreateNewEditorConfigAndDocumentKey() throws Exception {
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

    assertNotNull(config.getEditorConfig());
    assertNotNull(config.getEditorConfig().getUser());
    node.remove();
    session.save();
  }

  /**
   * Test download version
   * Add comment to the FileActivity with current file
   */
  @Test
  public void testDownloadVersionToJcrNodeAndAddingCommentToCurrentFile() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    ActivityTypeUtils.attachActivityId(node, "activityId");

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    editorService.downloadVersion("john", node.getUUID(), false, false, "comment", null);

    // Then
    node = editorService.getDocumentById(config.getWorkspace(), config.getDocId());
    String[] newMixinNodeTypes = ((NodeImpl) node).getMixinTypeNames();
    assertTrue(ArrayUtils.contains(newMixinNodeTypes, "eoo:onlyofficeFile"));
    assertTrue(node.hasProperty("eoo:commentId"));
    assertEquals("", node.getProperty("eoo:commentId").getValue().getString());
    assertTrue(node.hasProperty("exo:lastModifiedDate"));
    assertNotNull(node.getProperty("exo:lastModifiedDate").getValue().getString());
    assertTrue(node.hasProperty("exo:lastModifier"));
    assertEquals("john", node.getProperty("exo:lastModifier").getValue().getString());
    assertNotSame(config.getEditorConfig().getUser().lastSaved.toString(), "0");
    node.remove();
  }

  /**
   * Test download version when contentUrl not null
   */
  @Test
  public void testDownloadVersionWhenContentUrlNotNull() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    editorService.downloadVersion("john", node.getUUID(), false, false, "comment", config.getEditorUrl());

    // Then
    assertNotSame(config.getEditorConfig().getUser().lastSaved.toString(), "0");
    node.remove();
  }

  /**
   * Test get content
   */
  @Test
  public void testGetContent() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentContent documentContent = editorService.getContent("john", config.getDocument().getKey());

    // Then
    assertNotNull(documentContent);
    String content = IOUtils.toString(documentContent.getData(), "UTF-8");
    assertEquals("testContent", content);
    assertEquals("application/vnd.oasis.opendocument.text", documentContent.getType());
    node.remove();
  }

  /**
   * Test get content when file key not found
   */
  @Test
  public void testGetContentWhenFileKeyNotFound() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When

    try {
      editorService.getContent("john", "key");
      fail();
    } catch (BadParameterException e) {
      assertTrue(e instanceof BadParameterException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test get content when user editor not found
   */
  @Test
  public void testGetContentWhenUserEditorNotFound() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // When

    try {
      editorService.getContent("Thomas", config.getDocument().getKey());
      fail();
    } catch (BadParameterException e) {
      assertTrue(e instanceof BadParameterException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test get document with workspace and path
   */
  @Test
  public void testGetDocument() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Node nodeDocument = editorService.getDocument(null, node.getPath());

    // Then
    assertNotNull(nodeDocument);
    assertEquals(nodeDocument.getName(), "Test Document.docx");
    assertEquals(nodeDocument.getPrimaryNodeType().getName(), "nt:file");
    node.remove();
  }

  /**
   * Test get document by id when document exists with workspace and ID
   */
  @Test
  public void testGetDocumentByIdWhenDocumentExists() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Node nodeDocument = editorService.getDocumentById(null, node.getUUID());

    // Then
    assertNotNull(nodeDocument);
    assertEquals(nodeDocument.getName(), "Test Document.docx");
    assertEquals(nodeDocument.getPrimaryNodeType().getName(), "nt:file");
    node.remove();
  }

  /**
   * Test get document by id when wrong id
   */
  @Test
  public void testGetDocumentByIdWhenWrongId() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Node nodeDocument = editorService.getDocumentById(null, "john");

    // Then
    assertNull(nodeDocument);
    node.remove();
  }

  /**
   * Test get documentId
   */
  @Test
  public void testGetDocumentId() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    String documentId = editorService.getDocumentId(node);

    // Then
    assertNotNull(documentId);
    assertEquals(node.getUUID(), documentId);
    node.remove();
  }

  /**
   * Test get editor editor by key when no config
   */
  @Test
  public void testGetEditorByKeyWhenNoConfig() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config configTest = editorService.getEditorByKey("john", null);

    // Then
    assertNull(configTest);
    node.remove();
  }

  /**
   * Test get editor link
   */
  @Test
  public void testGetEditorLink() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    String editorLink = editorService.getEditorLink("http", "127.0.0.1", 8080, null, node.getUUID());

    // Then
    String editorLinkTest = "http://127.0.0.1:8080/portal/intranet/oeditor?docId=" + node.getUUID();
    assertNotNull(editorLink);
    assertEquals(editorLink, editorLinkTest);
    node.remove();
  }

  /**
   * Test get editor when editor exists
   */
  @Test
  public void testGetEditorWhenEditorAlreadyExists() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    Config configTest = editorService.getEditor("john", config.getWorkspace(), node.getPath());

    // Then
    assertNotNull(configTest);
    assertEquals(configTest, config);
    node.remove();
  }

  /**
   * Test get editor when no mix node "mix:referenceable"
   */
  @Test
  public void testGetEditorWhenNoMixNode() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    node.removeMixin("mix:versionable");
    node.save();

    node.removeMixin("mix:referenceable");
    node.save();

    // Then
    Config configTest = editorService.getEditor("john", config.getWorkspace(), node.getPath());
    assertNull(configTest);
    node.remove();
  }

  /**
   * Test get editor with true createCoEditing
   */
  @Test
  public void testGetEditorWithCreateCoEditing() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    Config configTest = editorService.getEditor("john", config.getWorkspace(), node.getPath());

    // Then
    assertNotNull(configTest);
    assertEquals(configTest, config);
    node.remove();
  }

  /**
   * Test get last modifier
   */
  @Test
  public void testGetLastModifier() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    editorService.setLastModifier(node.getUUID(), "john");

    // Then
    Config.Editor.User user = editorService.getLastModifier(node.getUUID());
    assertNotNull(user);
    assertEquals(user.getName(), "John Anthony");
    assertNotSame(user.lastModified, "0");
    node.remove();
  }

  /**
   * Test get state
   */
  @Test
  public void testGetState() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    // already saved
    ChangeState changeStateTosaved = editorService.getState("john", node.getUUID());
    // not saved
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    ChangeState changeStateToNotSaved = editorService.getState("john", node.getUUID());

    // Then
    assertTrue(changeStateTosaved.saved);
    assertFalse(changeStateToNotSaved.saved);
    node.remove();
  }

  /**
   * Test get state when user editor not found
   */
  @Test
  public void testGetStateWhenUserEditorNotFound() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // When

    try {
      editorService.getState("Thomas", node.getUUID());
      fail();
    } catch (BadParameterException e) {
      assertTrue(e instanceof BadParameterException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test get user from exoCache with key and userId when configuration is null
   */
  @Test
  public void testGetUserWhenConfigIsNull() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // Then
    Config.Editor.User user = editorService.getUser("key", "john");
    assertNull(user);
    node.remove();
  }

  /**
   * Test get user from exoCache with key and userId
   */
  @Test
  public void testGetUserWithKeyAndUserId() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // Then
    Config.Editor.User user = editorService.getUser(node.getUUID(), "john");
    assertNotNull(user);
    assertEquals(user.getName(), "John Anthony");
    node.remove();
  }

  /**
   * Test initialise document by node
   */
  @Test
  public void testInitDocumentByNode() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    String initDocument = editorService.initDocument(node);

    // Then
    assertNotNull(initDocument);
    assertEquals(node.getUUID(), initDocument);
    node.remove();
  }

  /**
   * Test initialise document by workspace and path
   */
  @Test
  public void testInitDocumentByWorkspaceAndPath() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());

    // When
    String initDocument = editorService.initDocument(config.getWorkspace(), node.getPath());

    // Then
    assertNotNull(initDocument);
    assertEquals(node.getUUID(), initDocument);
    node.remove();
  }

  /**
   * Test is document mime supported
   */
  @Test
  public void testIsDocumentMimeSupported() throws Exception {
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
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
   * Test is document mime supported when node is null
   */
  @Test
  public void testIsDocumentMimeSupportedWhenNullNode() throws Exception {

    // When
    Boolean isDocumentSupported = editorService.isDocumentMimeSupported(null);

    // Then
    assertFalse(isDocumentSupported);
  }

  /**
   * Test set last modifier
   */
  @Test
  public void testSetLastModifier() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    editorService.setLastModifier(node.getUUID(), "john");

    // Then
    assertNotSame(config.getEditorConfig().getUser().lastModified.toString(), "0");
    node.remove();
  }

  /**
   * Test update Document fot status code 3 it's an error of saving in
   * Onlyoffice, we sync to remote editors list first
   */
  @Test
  public void testUpdateDocumentErrorSavingInOnlyOffice() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(3L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .url("http://127.0.0.1:8080/editor/")
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    assertNotNull(status.getConfig());
    assertNotNull(status.getConfig().getError());
    node.remove();
  }

  /**
   * Test update Document fot status code 3 Error without content URL
   */
  @Test
  public void testUpdateDocumentErrorWithoutContentUrl() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(3L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    assertNotNull(status.getConfig());
    assertNotNull(status.getConfig().getError());
    assertEquals(status.getConfig().getError(), "Error in editor (0). No changes saved");
    node.remove();
  }

  /**
   * Test update Document for unknown status code
   */
  @Test
  public void testUpdateDocumentForUnknownCode() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(5L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(true)
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    Node document = editorService.getDocumentById("portal-test", node.getUUID());
    assertNotNull(document);
    assertEquals(node.getUUID(), document.getUUID());
    assertEquals(node.getPath(), document.getPath());
    assertEquals(node.getPrimaryNodeType().getName(), document.getPrimaryNodeType().getName());
    node.remove();
  }

  /**
   * Test update Document for status code 0 Onlyoffice doesn't know about such
   * document
   */
  @Test
  public void testUpdateDocumentForUnknownDocument() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(0L)
                                                        .userId("john")
                                                        .users(new String[] {})
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    try {
      editorService.updateDocument(status);
      fail();
    } catch (OnlyofficeEditorException e) {
      assertTrue(e instanceof OnlyofficeEditorException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test update Document for unknown file key
   */
  @Test
  public void testUpdateDocumentForUnknownFileKey() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    DocumentStatus status = new DocumentStatus.Builder().status(5L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(true)
                                                        .key("key")
                                                        .build();

    // When
    try {
      editorService.updateDocument(status);
      fail();
    } catch (BadParameterException e) {
      assertTrue(e instanceof BadParameterException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test update Document for unknown user
   */
  @Test
  public void testUpdateDocumentForUnknownUser() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(5L)
                                                        .users(new String[] { "Thomas" })
                                                        .userId("Thomas")
                                                        .saved(true)
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    try {
      editorService.updateDocument(status);
      fail();
    } catch (BadParameterException e) {
      assertTrue(e instanceof BadParameterException);
    } finally {
      node.remove();
    }
  }

  /**
   * Test update Document for status code 6 Forcedsave done, save the version
   * with its URL
   */
  @Test
  public void testUpdateDocumentForceSaveDone() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(6L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(true)
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    Node document = editorService.getDocumentById("portal-test", node.getUUID());
    assertNotNull(status.getConfig());
    assertNotSame(config.getEditorConfig().getUser().getLastSaved(), 0);
    assertNotNull(document);
    assertEquals(node.getUUID(), document.getUUID());
    assertEquals(node.getPath(), document.getPath());
    assertEquals(node.getPrimaryNodeType().getName(), document.getPrimaryNodeType().getName());
    node.remove();
  }

  /**
   * Test update Document fot status code 2 download if there were modifications
   * after the last saving
   */
  @Test
  public void testUpdateDocumentIfThereWereModificationAfterSaving() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(2L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.setLastModifier(node.getUUID(), "john");
    editorService.updateDocument(status);

    // Then
    assertTrue(config.isClosed());
    assertFalse(config.isOpen());
    assertNotSame(config.getEditorConfig().getUser().getLastSaved(), 0);
    node.remove();
  }

  /**
   * Test update Document fot status code 3 "Error in editor. Document still in
   * editing state.
   */
  @Test
  public void testUpdateDocumentWhenDocumentStillInEditing() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    Config configTest = editorService.createEditor("http", "127.0.0.1", 8080, "Root", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(3L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    assertNotNull(status.getConfig());
    assertNotNull(status.getConfig().getError());
    assertEquals(status.getConfig().getError(), "Error in editor. Document still in editing state");
    node.remove();
  }

  /**
   * Test update Document for status code 7 Received Onlyoffice error
   */
  @Test
  public void testUpdateDocumentWhileErrorInForceSaving() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(7L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(true)
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    Node document = editorService.getDocumentById("portal-test", node.getUUID());
    assertNotNull(document);
    assertEquals(node.getUUID(), document.getUUID());
    assertEquals(node.getPath(), document.getPath());
    assertEquals(node.getPrimaryNodeType().getName(), document.getPrimaryNodeType().getName());
    node.remove();
  }

  /**
   * Test update Document for status code 6 Forcedsave done, save the version
   * with its URL and save link
   */
  @Test
  public void testUpdateDocumentWithSaveLink() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(6L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(false)
                                                        .url("http://127.0.0.1:8080/editor/")
                                                        .key(config.getDocument().getKey())
                                                        .build();

    // When
    editorService.updateDocument(status);

    // Then
    assertNotNull(status.getConfig());
    assertNotSame(config.getEditorConfig().getUser().getLinkSaved(), 0);
    assertNotNull(config.getEditorConfig().getUser().getDownloadLink());
    node.remove();
  }

  /**
   * Test update title for not root node
   */
  @Test
  public void testUpdateTitle() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", false);

    // When
    editorService.updateTitle("portal-test", node.getUUID(), "TestDocumentJohn.docx", "john");

    // Then
    assertFalse(node.getParent().hasNode("Test Document.docx"));
    assertTrue(node.getParent().hasNode("TestDocumentJohn.docx"));
    node.remove();
  }

  /**
   * Test update title for root node
   */
  @Test
  public void testUpdateTitleRootNode() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);

    // When
    editorService.updateTitle("portal-test", node.getUUID(), "TestDocumentJohn.docx", "john");

    // Then
    assertFalse(node.getParent().hasNode("Test Document.docx"));
    assertTrue(node.getParent().hasNode("TestDocumentJohn.docx"));
    node.remove();
  }

  /**
   * Test update title when new title is empty
   */
  @Test
  public void testUpdateTitleWhenNewTitleIsEmpty() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", false);

    // When
    editorService.updateTitle("portal-test", node.getUUID(), "", "john");

    // Then
    assertTrue(node.getParent().hasNode("Test Document.docx"));
    node.remove();
  }

  /**
   * Test update title when not found document
   */
  @Test
  public void testUpdateTitleWhenNotFoundDocument() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", false);

    // When
    editorService.updateTitle(null, "docId", "TestDocumentJohn.docx", "john");

    // Then
    assertTrue(node.getParent().hasNode("Test Document.docx"));
    node.remove();

  }

  /**
   * Test user joined and leaved
   */
  @Test
  public void testUserJoinedAndLeaved() throws Exception {
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(1L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .key(config.getDocument().getKey())
                                                        .build();
    assertFalse(config.isOpen());
    editorService.updateDocument(status);
    assertTrue(config.isOpen());
    assertFalse(config.isClosed());
    status = new DocumentStatus.Builder().status(4L)
                                         .userId("john")
                                         .users(new String[] {})
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
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", true);
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

  /**
   * Test get list of versions
   */
  @Test
  public void testGetVersions() throws Exception {
    // Given
    startSessionAs("john");
    Node node = createDocument("Test Document.docx", "nt:file", "testContent", false);
    Config config = editorService.createEditor("http", "127.0.0.1", 8080, "john", null, node.getUUID());
    DocumentStatus status = new DocumentStatus.Builder().status(6L)
                                                        .users(new String[] { "john" })
                                                        .userId("john")
                                                        .saved(true)
                                                        .key(config.getDocument().getKey())
                                                        .comment("Document updated")
                                                        .build();
    editorService.updateDocument(status);

    // When
    List<Version> versions = editorService.getVersions("portal-test", node.getUUID(), 3, 0);

    // Then
    assertNotNull(versions);
    assertEquals(2, versions.size());
    Version version1 = versions.get(0);
    assertEquals("john", version1.getAuthor());
    assertEquals(1, version1.getVersionPageNumber());
    assertEquals("John Anthony", version1.getFullName());
    assertNotNull(version1.getVersionLabels());
    assertEquals(0, version1.getVersionLabels().length);
    Version version2 = versions.get(1);
    assertEquals("john", version2.getAuthor());
    assertEquals("John Anthony", version2.getFullName());
    assertNotNull(version2.getVersionLabels());
    assertEquals(1, version2.getVersionLabels().length);
    assertEquals("Document updated", version2.getVersionLabels()[0]);

    node.remove();
  }
}
