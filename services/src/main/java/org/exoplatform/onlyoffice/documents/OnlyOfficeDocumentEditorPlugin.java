/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.onlyoffice.documents;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.callModule;
import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.cometd.CometdConfig;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;

/**
 * The Class OnlyOfficeNewDocumentEditorPlugin.
 */
public class OnlyOfficeDocumentEditorPlugin extends BaseComponentPlugin implements DocumentEditor {

  /** The Constant PROVIDER_NAME. */
  protected static final String           PROVIDER_NAME                = "onlyoffice";

  /** The Constant PROVIDER_CONFIGURATION_PARAM. */
  protected static final String           PROVIDER_CONFIGURATION_PARAM = "provider-configuration";

  /** The Constant LOG. */
  protected static final Log              LOG                          =
                                              ExoLogger.getLogger(OnlyOfficeDocumentEditorPlugin.class);

  /** The Constant STREAM. */
  protected static final String           STREAM                       = "stream";

  /** The Constant PREVIEW. */
  protected static final String           PREVIEW                      = "peview";

  /** The Constant CLIENT_RESOURCE_PREFIX. */
  protected static final String           CLIENT_RESOURCE_PREFIX       = "OnlyofficeEditorClient.";

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /** The i 18 n service. */
  protected final ResourceBundleService   i18nService;

  /** The cometd service. */
  protected final CometdOnlyofficeService cometdService;

  /** The editor links. */
  protected final Map<Node, String>       editorLinks                  = new ConcurrentHashMap<>();

  /**
   * Instantiates a new OnlyOffice new document editor plugin.
   *
   * @param editorService the editor service
   * @param i18nService the i18nService
   * @param cometdService the cometdService
   */
  public OnlyOfficeDocumentEditorPlugin(OnlyofficeEditorService editorService,
                                        ResourceBundleService i18nService,
                                        CometdOnlyofficeService cometdService) {
    this.editorService = editorService;
    this.i18nService = i18nService;
    this.cometdService = cometdService;
  }

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  @Override
  public String getProviderName() {
    return PROVIDER_NAME;
  }

  /**
   * On document created.
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  @Override
  public void onDocumentCreated(String workspace, String path) throws Exception {
    Node document = editorService.getDocument(workspace, path);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Opening editor page for document {}", document);
    }
    String link = editorService.getEditorLink(document);
    if (link != null) {
      link = "'" + editorLink(link, "documents") + "'";
    } else {
      link = "null".intern();
      LOG.error("Cannot get editor link for document: {}, workspace: {}", path, workspace);
    }
    callModule("initEditorPage(" + link + ");");
  }

  /**
   * On document create.
   *
   * @param template the template
   * @param parentPath the parent path
   * @param title the title
   * @throws Exception the exception
   */
  @Override
  public void beforeDocumentCreate(NewDocumentTemplate template, String parentPath, String title) throws Exception {
    callModule("initNewDocument();");
  }

  /**
   * Inits the activity.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @throws Exception the exception
   */
  @Override
  public void initActivity(String uuid, String workspace, String activityId) throws Exception {
    Node symlink = editorService.getDocumentById(workspace, uuid);
    Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
    if (node != null) {
      String fileId = editorService.initDocument(node);
      String link = contextEditorLink(node, null, STREAM);
      if (link != null) {
        link = new StringBuilder("'").append(link).append("'").toString();
      } else {
        link = "null";
      }
      callModule("initActivity('" + fileId + "', " + link + ", '" + activityId + "');");
    }
  }

  /**
   * Inits the preview.
   *
   * @param fileId the uuid
   * @param workspace the workspace
   * @param requestUri the requestUri
   * @param locale the locale
   * @return editor settings
   */
  @SuppressWarnings("unchecked")
  @Override
  public EditorSetting initPreview(String fileId, String workspace, URI requestUri, Locale locale) {
    try {
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      Node symlink = editorService.getDocumentById(workspace, fileId);
      Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
      if (node != null) {
        if (symlink.isNodeType("exo:symlink")) {
          editorService.addFilePreferences(node, userId, symlink.getPath());
        }
        String documentId = editorService.initDocument(node);
        String link = contextEditorLink(node, requestUri, PREVIEW);
        Map<String, String> messages = initMessages(locale);
        CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(),
                                                   cometdService.getUserToken(userId),
                                                   PortalContainer.getCurrentPortalContainerName());
        return new EditorSetting(documentId, link, userId, cometdConf, messages);
      }
    } catch (OnlyofficeEditorException e) {
      LOG.error("Cannot initialize preview for fileId: {}, workspace: {}. {}", fileId, workspace, e.getMessage());
    } catch (RepositoryException e) {
      LOG.error("Cannot initialize preview", e);
    }
    return null;

  }

  /**
   * Gets the editor link.
   *
   * @param docNode the doc node
   * @param requestURI the requestURI
   * @return the editor link
   */
  protected String getEditorLink(Node docNode, URI requestURI) {
    try {
      if (requestURI != null) {
        return editorService.getEditorLink(docNode, requestURI);
      }
      return editorService.getEditorLink(docNode);
    } catch (OnlyofficeEditorException | RepositoryException e) {
      LOG.error(e);
      return null;
    }
  }

  /**
   * Context editor link.
   *
   * @param node the node
   * @param requestURI the requestURI
   * @param context the context
   * @return the link
   */
  private String contextEditorLink(Node node, URI requestURI, String context) {
    String link = editorLinks.computeIfAbsent(node, n -> getEditorLink(n, requestURI));
    if (link != null && !link.isEmpty()) {
      return editorLink(link, context);
    }
    return null;
  }

  /**
   * Inits the messages.
   *
   * @param locale the locale
   * @return the map
   */
  private Map<String, String> initMessages(Locale locale) {
    ResourceBundle res = i18nService.getResourceBundle("locale.onlyoffice.OnlyofficeClient", locale);
    Map<String, String> messages = new HashMap<String, String>();
    for (Enumeration<String> keys = res.getKeys(); keys.hasMoreElements();) {
      String key = keys.nextElement();
      String bundleKey;
      if (key.startsWith(CLIENT_RESOURCE_PREFIX)) {
        bundleKey = key.substring(CLIENT_RESOURCE_PREFIX.length());
      } else {
        bundleKey = key;
      }
      messages.put(bundleKey, res.getString(key));
    }
    return messages;
  }

  /**
   * The Class EditorSetting.
   */
  protected static class EditorSetting {

    /** The file id. */
    private final String              fileId;

    /** The link. */
    private final String              link;

    /** The user id. */
    private final String              userId;

    /** The cometd conf. */
    private final CometdConfig        cometdConf;

    /** The messages. */
    private final Map<String, String> messages;

    /**
     * Instantiates a new editor setting.
     *
     * @param fileId the file id
     * @param link the link
     * @param userId the user id
     * @param cometdConf the cometd conf
     * @param messages the messages
     */
    public EditorSetting(String fileId, String link, String userId, CometdConfig cometdConf, Map<String, String> messages) {
      this.fileId = fileId;
      this.link = link;
      this.userId = userId;
      this.cometdConf = cometdConf;
      this.messages = messages;
    }

    /**
     * Gets the file id.
     *
     * @return the file id
     */
    public String getFileId() {
      return fileId;
    }

    /**
     * Gets the link.
     *
     * @return the link
     */
    public String getLink() {
      return link;
    }

    /**
     * Gets the user id.
     *
     * @return the user id
     */
    public String getUserId() {
      return userId;
    }

    /**
     * Gets the cometd conf.
     *
     * @return the cometd conf
     */
    public CometdConfig getCometdConf() {
      return cometdConf;
    }

    /**
     * Gets the messages.
     *
     * @return the messages
     */
    public Map<String, String> getMessages() {
      return messages;
    }
  }

}
