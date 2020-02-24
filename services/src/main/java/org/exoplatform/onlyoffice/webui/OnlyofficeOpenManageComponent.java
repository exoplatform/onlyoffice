/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.editorLink;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Open Onlyoffice editor in file view. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeComponent.java 00000 Mar 01, 2016 pnedonosko $
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class, events = {
    @EventConfig(listeners = OnlyofficeOpenManageComponent.OnlyofficeOpenActionListener.class) })
public class OnlyofficeOpenManageComponent extends UIAbstractManagerComponent {

  /** The Constant LOG. */
  protected static final Log                   LOG     = ExoLogger.getLogger(OnlyofficeOpenManageComponent.class);

  /** The Constant FILTERS. */
  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[] { new FileFilter() });

  /**
   * The listener interface for receiving onlyofficeOpenAction events. The class
   * that is interested in processing a onlyofficeOpenAction event implements
   * this interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addOnlyofficeOpenActionListener</code> method. When the
   * onlyofficeOpenAction event occurs, that object's appropriate method is
   * invoked.
   */
  public static class OnlyofficeOpenActionListener extends UIActionBarActionListener<OnlyofficeOpenManageComponent> {

    /**
     * {@inheritDoc}
     */
    public void processEvent(Event<OnlyofficeOpenManageComponent> event) throws Exception {
      // This code will not be invoked
    }
  }

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    if (name.equals("OnlyofficeOpen")) {
      UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
      if (uiExplorer != null) {
        OnlyofficeEditorService editorService = this.getApplicationComponent(OnlyofficeEditorService.class);
        Node node = uiExplorer.getCurrentNode();
        node = editorService.getDocument(node.getSession().getWorkspace().getName(), node.getPath());
        LinkManager linkManager = CommonsUtils.getService(LinkManager.class);

        if(linkManager.isFileOrParentALink(uiExplorer.getSession(), uiExplorer.getCurrentPath())) {
          editorService.addFilePreferences(node, WebuiRequestContext.getCurrentInstance().getRemoteUser(), uiExplorer.getCurrentPath());
        }
        String editorLink = editorService.getEditorLink(node);
        if (editorLink != null && !editorLink.isEmpty()) {
          return "javascript:window.open('" + editorLink(editorLink, "documents") + "');";
        }
      } else {
        LOG.warn("Cannot find ancestor of type UIJCRExplorer in component " + this + ", parent: " + this.getParent());
      }
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}