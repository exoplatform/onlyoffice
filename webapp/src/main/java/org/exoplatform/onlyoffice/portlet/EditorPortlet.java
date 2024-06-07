
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
package org.exoplatform.onlyoffice.portlet;

import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.callModule;
import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.requireJS;
import static org.exoplatform.onlyoffice.webui.OnlyofficeContext.showError;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.RepositoryException;
import javax.portlet.GenericPortlet;
import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.w3c.dom.Element;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;

/**
 * The Class EditorPortlet.
 */
public class EditorPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log        LOG = ExoLogger.getLogger(EditorPortlet.class);

  /** The onlyoffice. */
  private OnlyofficeEditorService onlyoffice;

  /** The i 18 n service. */
  private ResourceBundleService   i18nService;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws PortletException {
    super.init();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    this.onlyoffice = container.getComponentInstanceOfType(OnlyofficeEditorService.class);
    this.i18nService = container.getComponentInstanceOfType(ResourceBundleService.class);
  }

  /**
   * Do headers.
   *
   * @param request the request
   * @param response the response
   */
  @Override
  protected void doHeaders(RenderRequest request, RenderResponse response) {
    super.doHeaders(request, response);

    ResourceBundle i18n = i18nService.getResourceBundle(
                                                        new String[] { "locale.onlyoffice.Onlyoffice",
                                                            "locale.onlyoffice.OnlyofficeClient" },
                                                        request.getLocale());

    Config config = getConfig(request, response, i18n, OnlyofficeEditorService.VIEW_MODE);

    if (config != null) {
      Element onlyOfficeJavascript = response.createElement("script");
      onlyOfficeJavascript.setAttribute("type", "text/javascript");
      onlyOfficeJavascript.setAttribute("src", config.getDocumentserverJsUrl());
      response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, onlyOfficeJavascript);
    }
  }

  /**
   * Renderer the portlet view.
   *
   * @param request the request
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws PortletException the portlet exception
   */
  @RenderMode(name = "view")
  public void view(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    ResourceBundle i18n = i18nService.getResourceBundle(
                                                        new String[] { "locale.onlyoffice.Onlyoffice",
                                                            "locale.onlyoffice.OnlyofficeClient" },
                                                        request.getLocale());

    
    RequestContext requestContext = RequestContext.getCurrentInstance();
    String mode = requestContext.getRequestParameter("mode");
    Config config = getConfig(request, response, i18n, mode);
    if (config != null) {
      try {
        requireJS().require("SHARED/bts_tooltip");
        if (OnlyofficeEditorService.VIEW_MODE.equals(mode)) {
          callModule("initViewer(" + config.toJSON() + ");");
        } else {
          callModule("initEditor(" + config.toJSON() + ");");
        }
      } catch (JsonException e) {
        LOG.error("Error converting editor configuration to JSON for node by ID: {}", config.getDocId(), e);
        showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                  i18n.getString("OnlyofficeEditor.error.CannotSendEditorConfiguration"));
      } catch (Exception e) {
        LOG.error("Error initializing editor configuration for node by ID: {}", config.getDocId(), e);
        showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                  i18n.getString("OnlyofficeEditor.error.CannotInitEditorConfiguration"));
      }
    }

    PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/editor.jsp");
    prDispatcher.include(request, response);
  }

  /**
   * Get editor config
   * If the config already exists, the createEditor returns it instead of creating a new one.
   *
   * @param request the request
   * @param response the response
   * @param i18n the i18n resource bundle
   * @return the editor config
   */
  private Config getConfig(RenderRequest request, RenderResponse response, ResourceBundle i18n, String mode) {
    Config config = null;

    RequestContext requestContext = RequestContext.getCurrentInstance();
    String docId = requestContext.getRequestParameter("docId");

    if (docId != null) {
      try {
        if (OnlyofficeEditorService.VIEW_MODE.equals(mode)) {
          config = onlyoffice.createViewer(request.getScheme(),
                                           request.getServerName(),
                                           request.getServerPort(),
                                           request.getRemoteUser(),
                                           null,
                                           docId);
        } else {
          config = onlyoffice.createEditor(request.getScheme(),
                                           request.getServerName(),
                                           request.getServerPort(),
                                           request.getRemoteUser(),
                                           null,
                                           docId);
        }
        if (config != null) {
          if (config.getEditorConfig().getLang() == null) {
            if (request.getLocale() != null) {
              // If user lang not defined use current request one
              config.getEditorConfig().setLang(request.getLocale().getLanguage());
            } else {
              // Otherwise use system default one
              config.getEditorConfig().setLang(Locale.getDefault().getLanguage());
            }
          }
        } else {
          showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                    i18n.getString("OnlyofficeEditor.error.EditorCannotBeCreated"));
        }
      } catch (RepositoryException e) {
        LOG.error("Error reading document node by ID: {}", docId, e);
        showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                  i18n.getString("OnlyofficeEditor.error.CannotReadDocument"));
      } catch (OnlyofficeEditorException e) {
        LOG.error("Error creating document editor for node by ID: {}", docId, e);
        showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                  i18n.getString("OnlyofficeEditor.error.CannotCreateEditor"));
      } catch (Exception e) {
        LOG.error("Error initializing editor configuration for node by ID: {}", docId, e);
        showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"),
                  i18n.getString("OnlyofficeEditor.error.CannotInitEditorConfiguration"));
      }
    } else {
      showError(i18n.getString("OnlyofficeEditorClient.ErrorTitle"), i18n.getString("OnlyofficeEditor.error.DocumentIdRequired"));
    }

    return config;
  }
}
