/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.onlyoffice.portlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.webui.OnlyofficeContext;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.application.RequireJS;

/**
 * Pins the editor page's choice between the viewer and the editor: an
 * anonymous visitor of a public site gets the viewer whatever mode the link
 * asks for, a user gets the mode the link asks for.
 */
@ExtendWith(MockitoExtension.class)
class EditorPortletTest {

  private static final String                DOC_ID = "doc-id";

  @Mock
  private OnlyofficeEditorService            onlyoffice;

  @Mock
  private ResourceBundleService              i18nService;

  @Mock
  private RequestContext                     requestContext;

  @Mock
  private RenderRequest                      request;

  @Mock
  private RenderResponse                     response;

  private MockedStatic<ExoContainerContext>  containerContext;

  private MockedStatic<RequestContext>       requestContextStatic;

  private MockedStatic<OnlyofficeContext>    onlyofficeContext;

  private EditorPortlet                      portlet;

  @BeforeEach
  void setUp() throws Exception {
    ExoContainer container = mock(ExoContainer.class);
    when(container.getComponentInstanceOfType(OnlyofficeEditorService.class)).thenReturn(onlyoffice);
    when(container.getComponentInstanceOfType(ResourceBundleService.class)).thenReturn(i18nService);
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(ExoContainerContext::getCurrentContainer).thenReturn(container);

    requestContextStatic = mockStatic(RequestContext.class);
    requestContextStatic.when(RequestContext::getCurrentInstance).thenReturn(requestContext);
    when(requestContext.getRequestParameter("docId")).thenReturn(DOC_ID);

    RequireJS requireJS = mock(RequireJS.class);
    onlyofficeContext = mockStatic(OnlyofficeContext.class);
    onlyofficeContext.when(OnlyofficeContext::requireJS).thenReturn(requireJS);

    ResourceBundle resourceBundle = mock(ResourceBundle.class);
    when(i18nService.getResourceBundle(any(String[].class), any())).thenReturn(resourceBundle);
    when(request.getLocale()).thenReturn(Locale.ENGLISH);
    lenient().when(request.getScheme()).thenReturn("http");
    lenient().when(request.getServerName()).thenReturn("localhost");
    lenient().when(request.getServerPort()).thenReturn(8080);

    PortletRequestDispatcher dispatcher = mock(PortletRequestDispatcher.class);
    PortletContext portletContext = mock(PortletContext.class);
    when(portletContext.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    PortletConfig portletConfig = mock(PortletConfig.class);
    when(portletConfig.getPortletContext()).thenReturn(portletContext);
    portlet = new EditorPortlet();
    portlet.init(portletConfig);
  }

  @AfterEach
  void tearDown() {
    onlyofficeContext.close();
    requestContextStatic.close();
    containerContext.close();
  }

  @Test
  void testAnonymousVisitorGetsTheViewerForALinkWithoutMode() throws Exception {
    when(request.getRemoteUser()).thenReturn(null);
    when(requestContext.getRequestParameter("mode")).thenReturn(null);
    Config config = config();
    when(onlyoffice.createViewer(eq("http"), eq("localhost"), eq(8080), isNull(), isNull(), eq(DOC_ID))).thenReturn(config);

    portlet.view(request, response);

    verify(onlyoffice, never()).createEditor(anyString(), anyString(), anyInt(), any(), any(), anyString(), any());
    onlyofficeContext.verify(() -> OnlyofficeContext.callModule(argThat(code -> code.contains("initViewer"))));
  }

  @Test
  void testUserGetsTheEditorForAnEditLink() throws Exception {
    when(request.getRemoteUser()).thenReturn("john");
    when(requestContext.getRequestParameter("mode")).thenReturn(OnlyofficeEditorService.EDIT_MODE);
    Config config = config();
    when(onlyoffice.createEditor(eq("http"),
                                 eq("localhost"),
                                 eq(8080),
                                 eq("john"),
                                 isNull(),
                                 eq(DOC_ID),
                                 eq(OnlyofficeEditorService.EDIT_MODE))).thenReturn(config);

    portlet.view(request, response);

    verify(onlyoffice, never()).createViewer(anyString(), anyString(), anyInt(), any(), any(), anyString());
    onlyofficeContext.verify(() -> OnlyofficeContext.callModule(argThat(code -> code.contains("initEditor"))));
  }

  private Config config() throws Exception {
    Config config = mock(Config.class, RETURNS_DEEP_STUBS);
    when(config.getEditorConfig().getLang()).thenReturn("en");
    when(config.toJSON()).thenReturn("{}");
    return config;
  }
}
