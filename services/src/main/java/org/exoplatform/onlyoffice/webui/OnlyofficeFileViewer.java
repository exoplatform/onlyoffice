package org.exoplatform.onlyoffice.webui;

import javax.jcr.RepositoryException;
import jakarta.servlet.http.HttpServletRequest;

import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.form.UIForm;

/**
 * The Class OnlyofficeFileViewer is used to show document preview.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:resources/templates/OnlyofficeFileViewer.gtmpl")

/**
 * Onlyoffice File Viewer component which will be used to display Office files on web browser
 */
public class OnlyofficeFileViewer extends UIForm {

  /** The Constant LOG. */
  private static final Log LOG = ExoLogger.getExoLogger(OnlyofficeFileViewer.class);

  /**
   * Gets the file viewer config.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @return the file viewer config
   */
  public Config getFileViewerConfig(String fileId, String workspace) {
    OnlyofficeEditorService editorService = WCMCoreUtils.getService(OnlyofficeEditorService.class);
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    HttpServletRequest request = requestContext.getRequest();
    try {
      Config config = editorService.createViewer(request.getScheme(),
                                                 request.getServerName(),
                                                 request.getServerPort(),
                                                 request.getRemoteUser(),
                                                 workspace,
                                                 fileId);

      return config;
    } catch (RepositoryException | OnlyofficeEditorException e) {
      LOG.error("Cannot create viewer config for fileId: " + fileId + ", workspace: " + workspace, e);
    }
    return null;
  }
}
