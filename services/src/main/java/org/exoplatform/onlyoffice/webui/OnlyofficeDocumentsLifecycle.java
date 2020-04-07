/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.onlyoffice.webui;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.callModule;
import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Listener that will initialize Onlyoffice integration for ECMS Documents app.
 * <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeDocumentsLifecycle.java 00000 Mar 21, 2019 pnedonosko
 *          $
 */
public class OnlyofficeDocumentsLifecycle extends AbstractOnlyofficeLifecycle {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(OnlyofficeDocumentsLifecycle.class);

  /**
   * Instantiates a new Onlyoffice documents lifecycle.
   */
  public OnlyofficeDocumentsLifecycle() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    RequestContext parentContext = context.getParentAppRequestContext();
    UIJCRExplorer explorer = context.getUIApplication().findFirstComponentOfType(UIJCRExplorer.class);
    if (explorer != null && parentContext != null) {
      try {
        String userName = context.getRemoteUser();
        Node node = explorer.getCurrentNode();
        String nodeWs = node.getSession().getWorkspace().getName();
        String nodePath = node.getPath();
        if (isNotSameUserDocument(userName, nodeWs, nodePath, parentContext)) {
          OnlyofficeEditorService editorService = context.getApplication()
                                                         .getApplicationServiceContainer()
                                                         .getComponentInstanceOfType(OnlyofficeEditorService.class);
          String docId = editorService.getDocumentId(node);
          if (docId != null && editorService.isDocumentMimeSupported(node)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Init documents explorer for {}, node: {}:{}, context: {}", userName, nodeWs, nodePath, parentContext);
            }
            parentContext.setAttribute(OnlyofficeContext.USERID_ATTRIBUTE, userName);
            parentContext.setAttribute(OnlyofficeContext.DOCUMENT_WORKSPACE_ATTRIBUTE, nodeWs);
            parentContext.setAttribute(OnlyofficeContext.DOCUMENT_PATH_ATTRIBUTE, nodePath);
            String editorLink = editorService.getEditorLink(node);
            if (editorLink == null || editorLink.isEmpty()) {
              editorLink = "null".intern();
            } else {
              editorLink = new StringBuilder().append('\'').append(editorLink(editorLink, "drives")).append('\'').toString();
            }
            // This will init explorer even for docs that cannot be edited
            // by the user (locked or lack of permissions)
            callModule("initExplorer('" + docId + "', " + editorLink + ");");
          } else if (LOG.isDebugEnabled()) {
            LOG.debug("Document not initialized or not editable for {}, node: {}:{}, context: {}",
                      userName,
                      nodeWs,
                      nodePath,
                      parentContext);
          }
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Already initialized documents explorer for {}, node: {}:{}, context: {}",
                      userName,
                      nodeWs,
                      nodePath,
                      parentContext);
          }
        }
      } catch (Exception e) {
        LOG.error("Couldn't read document of node", e);
      }
    } else if (LOG.isDebugEnabled()) {
      LOG.debug("Explorer or portal context not found, explorer: {}, context: {}", explorer, parentContext);
    }
    super.onEndRequest(app, context);
  }

  /**
   * Checks if is not same user document.
   *
   * @param userName the user name
   * @param nodeWs the node ws
   * @param nodePath the node path
   * @param parentContext the parent context
   * @return true, if is not same user document
   */
  private boolean isNotSameUserDocument(String userName, String nodeWs, String nodePath, RequestContext parentContext) {
    return !(userName.equals(parentContext.getAttribute(OnlyofficeContext.USERID_ATTRIBUTE))
        && nodeWs.equals(parentContext.getAttribute(OnlyofficeContext.DOCUMENT_WORKSPACE_ATTRIBUTE))
        && nodePath.equals(parentContext.getAttribute(OnlyofficeContext.DOCUMENT_PATH_ATTRIBUTE)));
  }
}
