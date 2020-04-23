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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.cometd.CometdConfig;
import org.exoplatform.onlyoffice.cometd.CometdOnlyofficeService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeClientContext.java 00000 Mar 18, 2019 pnedonosko $
 */
public class OnlyofficeContext {

  /** The Constant USERID_ATTRIBUTE. */
  public static final String    USERID_ATTRIBUTE             = "OnlyofficeContext.userId";

  /** The Constant DOCUMENT_WORKSPACE_ATTRIBUTE. */
  public static final String    DOCUMENT_WORKSPACE_ATTRIBUTE = "OnlyofficeContext.document.workspace";

  /** The Constant DOCUMENT_PATH_ATTRIBUTE. */
  public static final String    DOCUMENT_PATH_ATTRIBUTE      = "OnlyofficeContext.document.path";

  /** The Constant JAVASCRIPT. */
  protected static final String JAVASCRIPT                   = "OnlyofficeContext_Javascript".intern();

  /** The Constant CLIENT_RESOURCE_PREFIX. */
  protected static final String CLIENT_RESOURCE_PREFIX       = "OnlyofficeEditorClient.";

  /** The Constant LOG. */
  protected static final Log    LOG                          = ExoLogger.getLogger(OnlyofficeContext.class);

  /** The require. */
  private final RequireJS       require;

  /**
   * Instantiates a new onlyoffice client context.
   *
   * @param requestContext the request context
   * @throws Exception the exception
   */
  private OnlyofficeContext(WebuiRequestContext requestContext) throws Exception {
    JavascriptManager js = requestContext.getJavascriptManager();
    this.require = js.require("SHARED/onlyoffice", "onlyoffice");

    // Basic JS module initialization
    String messagesJson;
    try {
      ResourceBundleService i18nService = requestContext.getApplication()
                                                        .getApplicationServiceContainer()
                                                        .getComponentInstanceOfType(ResourceBundleService.class);
      ResourceBundle res = i18nService.getResourceBundle("locale.onlyoffice.OnlyofficeClient", requestContext.getLocale());
      Map<String, String> resMap = new HashMap<String, String>();
      for (Enumeration<String> keys = res.getKeys(); keys.hasMoreElements();) {
        String key = keys.nextElement();
        String bundleKey;
        if (key.startsWith(CLIENT_RESOURCE_PREFIX)) {
          bundleKey = key.substring(CLIENT_RESOURCE_PREFIX.length());
        } else {
          bundleKey = key;
        }
        resMap.put(bundleKey, res.getString(key));
      }
      messagesJson = new JsonGeneratorImpl().createJsonObjectFromMap(resMap).toString();
    } catch (JsonException e) {
      LOG.warn("Cannot serialize messages bundle JSON", e);
      messagesJson = "{}";
    } catch (Exception e) {
      LOG.warn("Cannot build messages bundle", e);
      messagesJson = "{}";
    }

    ConversationState convo = ConversationState.getCurrent();
    if (convo != null && convo.getIdentity() != null) {
      ExoContainer container = requestContext.getApplication().getApplicationServiceContainer();
      CometdOnlyofficeService cometdService = container.getComponentInstanceOfType(CometdOnlyofficeService.class);

      String userId = convo.getIdentity().getUserId();

      CometdConfig cometdConf = new CometdConfig(cometdService.getCometdServerPath(), cometdService.getUserToken(userId), PortalContainer.getCurrentPortalContainerName());

      callOnModule("init('" + userId + "', " + cometdConf.toJSON() + ", " + messagesJson + ");");
    } else {
      throw new OnlyofficeEditorException("Authenticated user required");
    }
  }

  /**
   * App require JS.
   *
   * @return the require JS
   */
  private RequireJS appRequireJS() {
    return require;
  }

  /**
   * Call on module.
   *
   * @param code the code
   */
  private void callOnModule(String code) {
    require.addScripts(new StringBuilder("onlyoffice.").append(code).append("\n").toString());
  }

  /**
   * Show client error.
   *
   * @param title the title
   * @param message the message
   */
  private void showClientError(String title, String message) {
    callOnModule(new StringBuilder("showError('").append(title).append("', '" + message + "');").toString());
  }

  /**
   * Context.
   *
   * @return the onlyoffice context
   * @throws Exception the exception
   */
  private static OnlyofficeContext context() throws Exception {
    OnlyofficeContext context;
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    Object obj = requestContext.getAttribute(JAVASCRIPT);
    if (obj == null || !OnlyofficeContext.class.isAssignableFrom(obj.getClass())) {
      synchronized (requestContext) {
        obj = requestContext.getAttribute(JAVASCRIPT);
        if (obj == null || !OnlyofficeContext.class.isAssignableFrom(obj.getClass())) {
          context = new OnlyofficeContext(requestContext);
          requestContext.setAttribute(JAVASCRIPT, context);
        } else {
          context = OnlyofficeContext.class.cast(obj);
        }
      }
    } else {
      context = OnlyofficeContext.class.cast(obj);
    }
    return context;
  }

  /**
   * Inits the context (current user, CometD settings, etc).
   * on Platform app request start.
   *
   * @throws Exception the exception
   */
  public static void init() throws Exception {
    context();
  }

  /**
   * Adds the script to be called on <code>onlyoffice</code> module. Finally it
   * will appear as <code>onlyoffice.myMethod(...)</code>, where myMethod(...)
   * it's what given as code parameter.
   *
   * @param code the code of a method to invoke on onlyoffice module
   * @throws Exception the exception
   */
  public static void callModule(String code) throws Exception {
    context().callOnModule(code);
  }

  /**
   * Return Web UI app's RequireJS instance.
   *
   * @return the require JS
   * @throws Exception the exception
   */
  public static RequireJS requireJS() throws Exception {
    return context().appRequireJS();
  }

  /**
   * Show error message to an user.
   *
   * @param title the title
   * @param message the message
   */
  public static void showError(String title, String message) {
    try {
      context().showClientError(title, message);
    } catch (Exception e) {
      LOG.error("Error initializing context", e);
    }
  }

  /**
   * Generate Editor link with context information: source app (e.g. stream or
   * documents), space name etc.
   *
   * @param link the link obtained from
   *          {@link OnlyofficeEditorService#getEditorLink(javax.jcr.Node)}
   * @param source the source name, can be any text value
   * @return the string with link URL
   */
  public static String editorLink(String link, String source) {
    if (link != null && source != null) {
      StringBuilder linkBuilder = new StringBuilder(link).append("&source=").append(source);
      return linkBuilder.toString();
    }
    return null;
  }

}
