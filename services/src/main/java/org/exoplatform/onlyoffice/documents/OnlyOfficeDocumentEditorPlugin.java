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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
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

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /** The i 18 n service. */
  protected final ResourceBundleService   i18nService;

  /** The editor links. */
  protected final Map<Node, String>       editorLinks                  = new ConcurrentHashMap<>();

  /**
   * Instantiates a new only office new document editor plugin.
   *
   * @param editorService the editor service
   * @param i18nService the i18nService
   */
  public OnlyOfficeDocumentEditorPlugin(OnlyofficeEditorService editorService, ResourceBundleService i18nService) {
    this.editorService = editorService;
    this.i18nService = i18nService;
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
      callModule("initActivity('" + fileId + "', " + link + ", '" + activityId + "');");
    }
  }

  /**
   * Inits the preview.
   *
   * @param fileId the uuid
   * @param workspace the workspace
   * @param requestUri the requestUri
   * @return editor settings
   */
  @Override
  public Object initPreview(String fileId, String workspace, URI requestUri) {
    try {
      Node symlink = editorService.getDocumentById(workspace, fileId);
      Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
      if (node != null) {
        if (symlink.isNodeType("exo:symlink")) {
          String userId = ConversationState.getCurrent().getIdentity().getUserId();
          editorService.addFilePreferences(node, userId, symlink.getPath());
        }
        String documentId = editorService.initDocument(node);
        String link = contextEditorLink(node, requestUri, PREVIEW);
        return new EditorSetting(documentId, link);
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
      return new StringBuilder().append("'").append(editorLink(link, context)).append("'").toString();
    }
    return "null".intern();
  }

  /**
   * The Class EditorSetting.
   */
  protected static class EditorSetting {

    /** The file id. */
    private final String fileId;

    /** The link. */
    private final String link;

    /**
     * Instantiates a new editor setting.
     *
     * @param fileId the file id
     * @param link the link
     */
    public EditorSetting(String fileId, String link) {
      this.fileId = fileId;
      this.link = link;
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

  }

}
