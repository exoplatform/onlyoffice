package org.exoplatform.onlyoffice;

import java.io.ByteArrayInputStream;
import java.util.Date;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.thumbnail.ImageThumbnailPlugin;

public class OfficeDocumentsThumbnailPlugin extends ImageThumbnailPlugin {

  public static final String DOCUMENTS_OFFICE = "documentsOffice";


  public static final String EXO_TITLE = "exo:title";

  public static final String FILE_TYPE = "jpeg";

  public static final String FILE_MIME_TYPE = "image/jpeg";

  private static final Log log = ExoLogger.getExoLogger(OfficeDocumentsThumbnailPlugin.class);

  public static SessionProvider getUserSessionProvider(RepositoryService repositoryService, Identity aclIdentity) {
    SessionProvider sessionProvider = new SessionProvider(new ConversationState(aclIdentity));
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      String workspace = repository.getConfiguration().getDefaultWorkspaceName();
      sessionProvider.setCurrentRepository(repository);
      sessionProvider.setCurrentWorkspace(workspace);
      return sessionProvider;
    } catch (RepositoryException e) {
      throw new IllegalStateException("Can't build a SessionProvider", e);
    }
  }

  @Override
  public String getFileType() {
    return DOCUMENTS_OFFICE;
  }

  @Override
  public FileItem getImage(String fileId, String userName) {
    UserACL userACL = CommonsUtils.getService(UserACL.class);
    RepositoryService repositoryService = CommonsUtils.getService(RepositoryService.class);
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = getUserSessionProvider(repositoryService, userACL.getUserIdentity(userName));
      Session session = sessionProvider.getSession("collaboration", repositoryService.getDefaultRepository());
      Node file = session.getNodeByUUID(fileId);
      OnlyofficeEditorService onlyofficeEditorService = CommonsUtils.getService(OnlyofficeEditorService.class);
      byte[] convertedContent;
      convertedContent = onlyofficeEditorService.convertNodeContent(file, FILE_TYPE, userName);
      FileItem fileItem = new FileItem(null,
              file.getProperty(EXO_TITLE).getString(),
              FILE_MIME_TYPE,
              "",
              0,
              new Date(),
              "",
              false,
              new ByteArrayInputStream(convertedContent));
      return fileItem;
    } catch (ItemNotFoundException e) {
      log.warn("Node with uuid {} not exists in collaboration workspace", fileId);
    } catch (Exception e) {
      log.error("Cannot get content stream of node with uid {}", fileId, e);
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
    return null;
  }
}
