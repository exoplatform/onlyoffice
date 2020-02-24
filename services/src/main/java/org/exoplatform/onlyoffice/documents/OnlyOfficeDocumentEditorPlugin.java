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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class OnlyOfficeNewDocumentEditorPlugin.
 */
public class OnlyOfficeDocumentEditorPlugin extends BaseComponentPlugin implements DocumentEditorPlugin {

  /** The Constant PROVIDER_NAME. */
  protected static final String           PROVIDER_NAME = "onlyoffice";

  /** The Constant LOG. */
  protected static final Log              LOG           = ExoLogger.getLogger(OnlyOfficeDocumentEditorPlugin.class);

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /** The i 18 n service. */
  protected final ResourceBundleService   i18nService;

  /** The editor links. */
  protected final Map<Node, String>       editorLinks   = new ConcurrentHashMap<>();

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
    LOG.debug("Opening editor page for document {}", document);
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
  public void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title) throws Exception {
    callModule("initActivity('initNewDocument();");
  }

  /**
   * Inits the activity.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @param context the context
   * @throws Exception the exception
   */
  @Override
  public void initActivity(String uuid, String workspace, String activityId, String context) throws Exception {
    Node symlink = editorService.getDocumentById(workspace, uuid);
    Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
    if (node != null) {
      String fileId = editorService.initDocument(node);
      String link = contextEditorLink(node, context);
      callModule("initActivity('" + fileId + "', " + link + ", '" + activityId + "');");
    }
  }

 
  /**
   * Inits the preview.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param activityId the activity id
   * @param context the context
   * @param index the index
   * @throws Exception the exception
   */
  @Override
  public void initPreview(String uuid, String workspace, String activityId, String context, int index) throws Exception {
    Node symlink = editorService.getDocumentById(workspace, uuid);
    Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
    if (node != null) {
      if (symlink.isNodeType("exo:symlink")) {
        String userId = WebuiRequestContext.getCurrentInstance().getRemoteUser();
        editorService.addFilePreferences(node, userId, symlink.getPath());
      }
      String fileId = editorService.initDocument(node);
      String link = contextEditorLink(node, context);
      callModule("initPreview('" + fileId + "', " + link + ", '" + activityId + "', '" + index + "');");
    }
  }

  /**
   * Gets the editor link.
   *
   * @param docNode the doc node
   * @return the editor link
   */
  protected String getEditorLink(Node docNode) {
    try {
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
   * @param context the context
   * @return the string
   */
  private String contextEditorLink(Node node, String context) {
    String link = editorLinks.computeIfAbsent(node, n -> getEditorLink(n));
    if (link != null && !link.isEmpty()) {
      return new StringBuilder().append("'").append(editorLink(link, context)).append("'").toString();
    }
    return "null".intern();
  }

}
