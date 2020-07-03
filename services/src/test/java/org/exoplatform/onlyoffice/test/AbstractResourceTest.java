package org.exoplatform.onlyoffice.test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.*;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.OnlyofficeEditorService.Mode;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.tools.DummyContainerResponseWriter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * The Class AbstractResourceTest.
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test/test-configuration.xml") })
public class AbstractResourceTest extends BaseCommonsTestCase {

  /** The request handler. */
  protected RequestHandlerImpl      requestHandler;

  /** The session provider service. */
  protected SessionProviderService  sessionProviderService;

  /** The session provider. */
  protected SessionProvider         sessionProvider;
  
  protected Session                 session;

  /** The onlyoffice editor service. */
  protected OnlyofficeEditorService onlyofficeEditorService;

  /** The jcr service. */
  protected RepositoryService       jcrService;

  /**
   * Before class.
   */
  @Override
  protected void beforeClass() {
    super.beforeClass();
    ExoContainerContext.setCurrentContainer(container);
    this.sessionProviderService = getContainer().getComponentInstanceOfType(SessionProviderService.class);
    this.container = PortalContainer.getInstance();
    this.requestHandler = getContainer().getComponentInstanceOfType(RequestHandlerImpl.class);
    this.jcrService = getContainer().getComponentInstanceOfType(RepositoryService.class);
    this.onlyofficeEditorService = getContainer().getComponentInstanceOfType(OnlyofficeEditorService.class);
  }

  /**
   * Sends a request to specified URI.
   *
   * @param method the method
   * @param requestURI the request URI
   * @param baseURI the base URI
   * @param headers the headers
   * @param data the data
   * @return the container response
   * @throws Exception the exception
   */
  protected ContainerResponse service(String method,
                                      String requestURI,
                                      String baseURI,
                                      Map<String, List<String>> headers,
                                      byte[] data) throws Exception {

    if (headers == null) {
      headers = new MultivaluedMapImpl();
    }

    ByteArrayInputStream in = null;
    if (data != null) {
      in = new ByteArrayInputStream(data);
    }

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest =
                                   new OnlyofficeMockHttpServletRequest("", in, in != null ? in.available() : 0, method, headers);
    envctx.put(HttpServletRequest.class, httpRequest);
    EnvironmentContext.setCurrent(envctx);
    ContainerRequest request = new ContainerRequest(method,
                                                    new URI(requestURI),
                                                    new URI(baseURI),
                                                    in,
                                                    new InputHeadersMap(headers));
    ContainerResponse response = new ContainerResponse(new DummyContainerResponseWriter());
    requestHandler.handleRequest(request, response);
    return response;
  }

  /**
   * Start session as a user.
   *
   * @param user the user
   * @throws RepositoryException 
   */
  protected void startSessionAs(String user) throws RepositoryException {
    HashSet<MembershipEntry> memberships = new HashSet<MembershipEntry>();
    memberships.add(new MembershipEntry("/platform/administrators"));
    Identity identity = new Identity(user, memberships);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
    sessionProviderService.setSessionProvider(null, new SessionProvider(state));
    sessionProvider = sessionProviderService.getSessionProvider(null);
    session = sessionProvider.getSession("portal-test", SessionProviderService.getRepository());
  }

  /**
   * Creates the test document.
   *
   * @param user the user
   * @param title the title
   * @param content the content
   * @return the key
   * @throws Exception the exception
   */
  protected String createTestDocument(String user, String title, String content) throws Exception {
    Node node = session.getRootNode().addNode(title, "nt:file");
    Node contentNode = node.addNode("jcr:content", "nt:unstructured");
    contentNode.setProperty("jcr:mimeType", "application/vnd.oasis.opendocument.text");
    contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
    contentNode.setProperty("jcr:data", content);
    session.save();
    String docId = onlyofficeEditorService.initDocument(node);
    Config config = onlyofficeEditorService.createEditor("http", "localhost", 8080, user, null, docId, Mode.EDIT);
    return config != null ? config.getDocument().getKey() : null;
  }
}
