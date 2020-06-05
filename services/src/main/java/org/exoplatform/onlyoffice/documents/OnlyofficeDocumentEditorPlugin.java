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
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.onlyoffice.DocumentNotFoundException;
import org.exoplatform.onlyoffice.EditorLinkNotFoundException;
import org.exoplatform.onlyoffice.OnlyOfficeDocumentUpdateActivityHandler;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.cometd.CometdConfig;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.cms.documents.DocumentUpdateActivityHandler;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class OnlyOfficeNewDocumentEditorPlugin.
 */
public class OnlyofficeDocumentEditorPlugin extends BaseComponentPlugin implements DocumentEditor {

  /** The Constant PROVIDER_NAME. */
  protected static final String                 PROVIDER_NAME                       = "onlyoffice";

  /** The Constant EDITING_FINISHED_DELAY. */
  protected static final long                   EDITING_FINISHED_DELAY              = 10000L;

  /** The Constant PROVIDER_CONFIGURATION_PARAM. */
  protected static final String                 PROVIDER_CONFIGURATION_PARAM        = "provider-configuration";

  /** The Constant EDITOR_LINK_NOT_FOUND_ERROR. */
  protected static final String                 EDITOR_LINK_NOT_FOUND_ERROR         = "EditorLinkNotFoundError";

  /** The Constant EDITOR_LINK_NOT_FOUND_ERROR_MESSAGE. */
  protected static final String                 EDITOR_LINK_NOT_FOUND_ERROR_MESSAGE = "EditorLinkNotFoundErrorMessage";

  /** The Constant STORAGE_ERROR. */
  protected static final String                 STORAGE_ERROR                       = "StorageError";

  /** The Constant STORAGE_ERROR_MESSAGE. */
  protected static final String                 STORAGE_ERROR_MESSAGE               = "StorageErrorMessage";

  /** The Constant INTERNAL_EDITOR_ERROR. */
  protected static final String                 INTERNAL_EDITOR_ERROR               = "InternalEditorError";

  /** The Constant INTERNAL_EDITOR_ERROR_MESSAGE. */
  protected static final String                 INTERNAL_EDITOR_ERROR_MESSAGE       = "InternalEditorErrorMessage";

  /** The Constant LOG. */
  protected static final Log                    LOG                                 =
                                                    ExoLogger.getLogger(OnlyofficeDocumentEditorPlugin.class);

  /** The Constant STREAM. */
  protected static final String                 STREAM                              = "stream";

  /** The Constant PREVIEW. */
  protected static final String                 PREVIEW                             = "peview";

  /** The Constant DRIVES. */
  protected static final String                 DRIVES                              = "drives";

  /** The Constant CLIENT_RESOURCE_PREFIX. */
  protected static final String                 CLIENT_RESOURCE_PREFIX              = "OnlyofficeEditorClient.";

  /** The editor service. */
  protected final OnlyofficeEditorService       editorService;

  /** The i 18 n service. */
  protected final ResourceBundleService         i18nService;

  /** The cometd service. */
  protected final CometdOnlyofficeService       cometdService;

  /** The link manager. */
  protected final LinkManager                   linkManager;

  /** The editor links. */
  protected final Map<Node, String>             editorLinks                         = new ConcurrentHashMap<>();

  /** The update handler. */
  protected final DocumentUpdateActivityHandler updateHandler;

  /**
   * Instantiates a new OnlyOffice new document editor plugin.
   *
   * @param editorService the editor service
   * @param i18nService the i18nService
   * @param cometdService the cometdService
   * @param linkManager the link manager
   */
  public OnlyofficeDocumentEditorPlugin(OnlyofficeEditorService editorService,
                                        ResourceBundleService i18nService,
                                        CometdOnlyofficeService cometdService,
                                        LinkManager linkManager) {
    this.editorService = editorService;
    this.i18nService = i18nService;
    this.cometdService = cometdService;
    this.linkManager = linkManager;
    this.updateHandler = new OnlyOfficeDocumentUpdateActivityHandler();
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
    String link = "'" + contextEditorLink(document, DRIVES, null) + "'";
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
    if (node != null && editorService.isDocumentMimeSupported(node)) {
      String fileId = editorService.initDocument(node);
      String link = "null";
      try {
        link = contextEditorLink(node, STREAM, null);
        link = new StringBuilder("'").append(link).append("'").toString();
      } catch (EditorLinkNotFoundException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Cannot get editor link for activity: ", e.getMessage());
        }
      } catch (OnlyofficeEditorException e) {
        LOG.warn("Cannot get editor link for activity: ", e);
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
      if (node != null && editorService.isDocumentMimeSupported(node)) {
        if (symlink.isNodeType("exo:symlink")) {
          editorService.addFilePreferences(node, userId, symlink.getPath());
        }
        String documentId = editorService.initDocument(node);
        String link = null;
        EditorError error = null;
        try {
          link = contextEditorLink(node, PREVIEW, requestUri);
        } catch (EditorLinkNotFoundException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Cannot get editor link for preview: {}", e.getMessage());
          }
          error = new EditorError(EDITOR_LINK_NOT_FOUND_ERROR, EDITOR_LINK_NOT_FOUND_ERROR_MESSAGE);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Cannot get editor link for preview: ", e);
          error = new EditorError(INTERNAL_EDITOR_ERROR, INTERNAL_EDITOR_ERROR_MESSAGE);
        } catch (RepositoryException e) {
          LOG.error("Cannot get editor link for preview: ", e);
          error = new EditorError(STORAGE_ERROR, STORAGE_ERROR_MESSAGE);
        }

        Map<String, String> messages = initMessages(locale);
        CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(),
                                                   cometdService.getUserToken(userId),
                                                   PortalContainer.getCurrentPortalContainerName());
        return new EditorSetting(documentId, link, userId, cometdConf, messages, error);
      }
    } catch (OnlyofficeEditorException e) {
      LOG.error("Cannot initialize preview for fileId: {}, workspace: {}. {}", fileId, workspace, e.getMessage());
    } catch (RepositoryException e) {
      LOG.error("Cannot initialize preview", e);
    }
    return null;
  }

  /**
   * Inits the explorer.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @param context the context
   * @return the editor setting
   */
  @SuppressWarnings("unchecked")
  @Override
  public EditorSetting initExplorer(String fileId, String workspace, WebuiRequestContext context) {
    try {
      String userId = context.getRemoteUser();
      Node node = editorService.getDocumentById(workspace, fileId);
      if (editorService.isDocumentMimeSupported(node)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Init documents explorer for node: {}:{}", workspace, fileId);
        }
        // Handling symlinks
        UIJCRExplorer uiExplorer = context.getUIApplication().findFirstComponentOfType(UIJCRExplorer.class);
        if (uiExplorer != null) {
          if (linkManager.isFileOrParentALink(uiExplorer.getSession(), uiExplorer.getCurrentPath())) {
            editorService.addFilePreferences(node, userId, uiExplorer.getCurrentPath());
          }
        } else {
          LOG.warn("Cannot check for symlink node {}:{} - UIJCRExplorer is null", fileId, workspace);
        }
        String link = null;
        EditorError error = null;
        try {
          link = contextEditorLink(node, PREVIEW, null);
        } catch (EditorLinkNotFoundException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Cannot get editor link for preview: {}", e.getMessage());
          }
          error = new EditorError(EDITOR_LINK_NOT_FOUND_ERROR, EDITOR_LINK_NOT_FOUND_ERROR_MESSAGE);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Cannot get editor link for preview: ", e);
          error = new EditorError(INTERNAL_EDITOR_ERROR, INTERNAL_EDITOR_ERROR_MESSAGE);
        } catch (RepositoryException e) {
          LOG.error("Cannot get editor link for preview: ", e);
          error = new EditorError(STORAGE_ERROR, STORAGE_ERROR_MESSAGE);
        }
        Map<String, String> messages = initMessages(context.getLocale());
        CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(),
                                                   cometdService.getUserToken(userId),
                                                   PortalContainer.getCurrentPortalContainerName());
        return new EditorSetting(fileId, link, userId, cometdConf, messages, error);
      }
    } catch (Exception e) {
      LOG.error("Cannot initialize explorer for fileId: " + fileId + ", workspace: " + workspace, e);
    }
    return null;
  }

  /**
   * Checks if is document supported.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @return true, if is document supported
   */
  @Override
  public boolean isDocumentSupported(String fileId, String workspace) {
    try {
      Node node = editorService.getDocumentById(workspace, fileId);
      return editorService.canEditDocument(node);
    } catch (DocumentNotFoundException | RepositoryException e) {
      LOG.error("Cannot check if the file is supported", e);
    }
    return false;
  }

  /**
   * Gets the document update handler.
   *
   * @return the document update handler
   */
  @Override
  public DocumentUpdateActivityHandler getDocumentUpdateHandler() {
    return updateHandler;
  }

  /**
   * On last editor closed.
   *
   * @param fileId the file id
   * @param workspace the workspace
   */
  @Override
  public void onLastEditorClosed(String fileId, String workspace) {
    // Nothing
  }

  @Override
  public long getEditingFinishedDelay() {
    return EDITING_FINISHED_DELAY;
  }

  /**
   * Context editor link.
   *
   * @param node the node
   * @param context the context
   * @param requestURI the request URI
   * @return the string
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  private String contextEditorLink(Node node, String context, URI requestURI) throws OnlyofficeEditorException,
                                                                              RepositoryException {
    String link;
    if (requestURI != null) {
      link = editorService.getEditorLink(node, requestURI.getScheme(), requestURI.getHost(), requestURI.getPort());
    } else {
      PortalRequestContext pcontext = Util.getPortalRequestContext();
      if (pcontext != null) {
        link = editorService.getEditorLink(node,
                                           pcontext.getRequest().getScheme(),
                                           pcontext.getRequest().getServerName(),
                                           pcontext.getRequest().getServerPort());
      } else {
        throw new OnlyofficeEditorException("Cannot get editor link - request URI and PortalRequestContext are null");
      }
    }
    editorLinks.putIfAbsent(node, link);
    return editorLink(link, context);
  }

  /**
   * Generate Editor link with context information: source app (e.g. stream or
   * drives), space name etc.
   *
   * @param link the link obtained from
   *          {@link OnlyofficeEditorService#getEditorLink(Node, String, String, int)}
   * @param source the source name, can be any text value
   * @return the string with link URL
   */
  private String editorLink(String link, String source) {
    StringBuilder linkBuilder = new StringBuilder(link).append("&source=").append(source);
    return linkBuilder.toString();
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

    /** The error. */
    private final EditorError         error;

    /**
     * Instantiates a new editor setting.
     *
     * @param fileId the file id
     * @param link the link
     * @param userId the user id
     * @param cometdConf the cometd conf
     * @param messages the messages
     * @param error the error
     */
    public EditorSetting(String fileId,
                         String link,
                         String userId,
                         CometdConfig cometdConf,
                         Map<String, String> messages,
                         EditorError error) {
      this.fileId = fileId;
      this.link = link;
      this.userId = userId;
      this.cometdConf = cometdConf;
      this.messages = messages;
      this.error = error;
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

    /**
     * Gets the error.
     *
     * @return the error
     */
    public EditorError getError() {
      return error;
    }
  }

  /**
   * The Class Error.
   */
  public static class EditorError {

    /** The key. */
    private final String type;

    /** The message. */
    private final String message;

    /**
     * Instantiates a new editor error.
     *
     * @param type the type
     * @param message the message
     */
    public EditorError(String type, String message) {
      this.type = type;
      this.message = message;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
      return type;
    }

  }

}
