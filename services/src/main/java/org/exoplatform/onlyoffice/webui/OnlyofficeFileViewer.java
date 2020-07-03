package org.exoplatform.onlyoffice.webui;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.OnlyofficeEditorService.Mode;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.form.UIForm;

import antlr.Utils;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:resources/templates/OnlyofficeFileViewer.gtmpl")

/**
 * Onlyoffice File Viewer component which will be used to display Office files on web browser
 */
public class OnlyofficeFileViewer extends UIForm {

  private static final Log LOG = ExoLogger.getExoLogger(OnlyofficeFileViewer.class);

  public OnlyofficeFileViewer() throws Exception {
    LOG.info("ONLYOFFICE FILE VIEWER");
  }

  public Config getFileViewerConfig(String fileId, String workspace) throws Exception {
    OnlyofficeEditorService editorService = WCMCoreUtils.getService(OnlyofficeEditorService.class);
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    HttpServletRequest request = requestContext.getRequest();
 

    try {
      Config config = editorService.createEditor(request.getScheme(),
                                                 request.getServerName(),
                                                 request.getServerPort(),
                                                 request.getRemoteUser(),
                                                 workspace,
                                                 fileId,
                                                 Mode.VIEW);
      if (config != null) {
        return config;
      }

    } catch (RepositoryException e) {
      // TODO: log
      LOG.error(e);
    } catch (OnlyofficeEditorException e) {
      LOG.error(e);
    }
    return null;

  }

}
