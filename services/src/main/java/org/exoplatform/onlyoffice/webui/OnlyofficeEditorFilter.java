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

import static org.exoplatform.onlyoffice.webui.OnlyofficeEditorLifecycle.EDITOR_STATES_ATTR_NAME;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.webui.application.WebuiApplication;

/**
 * Filter to add listener that will initialize Onlyoffice editor app. <br>
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorFilter.java 00000 Mar 21, 2019 pnedonosko $
 */
public class OnlyofficeEditorFilter extends AbstractOnlyofficeWebFilter {

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    WebAppController controller = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WebAppController.class);
    WebuiApplication app = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
    if (app != null) {
      // Initialize portal app, this will happen once per app lifetime
      @SuppressWarnings("rawtypes")
      final List<ApplicationLifecycle> lifecycles = app.getApplicationLifecycle();
      OnlyofficeEditorLifecycle lifecycle = getLifecycle(lifecycles, OnlyofficeEditorLifecycle.class);
      if (lifecycle == null) {
        synchronized (lifecycles) {
          lifecycle = getLifecycle(lifecycles, OnlyofficeEditorLifecycle.class);
          if (lifecycle == null) {
            lifecycles.add(lifecycle = new OnlyofficeEditorLifecycle());
          }
        }
      }
      // Prepare env for lifecycle work 
      request.setAttribute(EDITOR_STATES_ATTR_NAME, ConcurrentHashMap.newKeySet());
      try {
        chain.doFilter(request, response);
      } finally {
        // run restore for a case of exception in request processing
        lifecycle.restore(request);
        request.removeAttribute(EDITOR_STATES_ATTR_NAME);
      }
    } else {
      chain.doFilter(request, response);
    }
  }
}
