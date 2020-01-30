/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileUIActivity.java 00000 Feb 20, 2019 pnedonosko $
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "war:/groovy/ecm/social-integration/plugin/space/FileUIActivity.gtmpl", events = {
        @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class) }), })
public class FileUIActivity extends org.exoplatform.wcm.ext.component.activity.FileUIActivity {

  /** The Constant LOG. */
  private static final Log                LOG         = ExoLogger.getLogger(FileUIActivity.class);

  /** The editor service. */
  protected final OnlyofficeEditorService editorService;

  /** The editor links. */
  protected final Map<Node, String>       editorLinks = new ConcurrentHashMap<>();

  /**
   * Instantiates a new file UI activity with Edit Online button for office
   * documents.
   *
   * @throws Exception the exception
   */
  public FileUIActivity() throws Exception {
    super();
    this.editorService = this.getApplicationComponent(OnlyofficeEditorService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() throws Exception {
    String activityId = getActivity().getId();

    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      node = editorService.getDocument(node.getSession().getWorkspace().getName(), node.getPath());
      if (node != null) {
        callModule("initActivity('" + editorService.initDocument(node) + "', " + contextEditorLink(node, "stream") + ",'"
            + activityId + "');");
      }
    }
    String userId = WebuiRequestContext.getCurrentInstance().getRemoteUser();
    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node symlink = getContentNode(index);
      Node node = editorService.getDocument(symlink.getSession().getWorkspace().getName(), symlink.getPath());
      if (symlink.isNodeType("exo:symlink")) {
        editorService.addFilePreferences(node, userId, symlink.getPath());
      }
      if (node != null) {
        callModule("initPreview('" + editorService.initDocument(node) + "', " + contextEditorLink(node, "preview") + ",'"
            + new StringBuilder("#Preview").append(activityId).append('-').append(index).toString() + "');");
      }
    }
    super.end();
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
    if (link == null || link.isEmpty()) {
      return "null".intern();
    } else {
      return new StringBuilder().append('\'').append(editorLink(link, context)).append('\'').toString();
    }
  }

}
