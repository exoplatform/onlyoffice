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

import jakarta.servlet.ServletRequest;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.web.application.Application;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequestFailure;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Base listener for Onlyoffice integration in Platforms apps. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: AbstractOnlyofficeLifecycle.java 00000 Mar 21, 2019 pnedonosko
 *          $
 */
public abstract class AbstractOnlyofficeLifecycle implements ApplicationLifecycle<WebuiRequestContext> {

  /**
   * Instantiates a new abstract Onlyoffice lifecycle.
   */
  public AbstractOnlyofficeLifecycle() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInit(Application app) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onStartRequest(Application app, WebuiRequestContext context) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onEndRequest(Application app, WebuiRequestContext context) throws Exception {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onFailRequest(Application app, WebuiRequestContext context, RequestFailure failureType) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDestroy(Application app) throws Exception {
    // nothing
  }
  
  /**
   * Gets the servlet request associated with given context.
   *
   * @param context the context
   * @return the servlet request
   */
  protected ServletRequest getServletRequest(WebuiRequestContext context) {
    try {
      // First we assume it's PortalRequestContext
      return context.getRequest();
    } catch(ClassCastException e) {
      // Then try get portlet's parent context
      RequestContext parentContext = context.getParentAppRequestContext();
      if (parentContext != null && PortalRequestContext.class.isAssignableFrom(parentContext.getClass())) {
        return PortalRequestContext.class.cast(parentContext).getRequest();
      }
    }
    return null;
  }
}
