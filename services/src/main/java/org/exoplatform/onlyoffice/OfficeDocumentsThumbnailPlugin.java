/*
 * Copyright (C) 2025 eXo Platform SAS
 *
 *  This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <gnu.org/licenses>.
 */

package org.exoplatform.onlyoffice;

import java.io.ByteArrayInputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.portal.thumbnail.model.FileContent;
import io.meeds.portal.thumbnail.plugin.ImageThumbnailPlugin;

public class OfficeDocumentsThumbnailPlugin extends ImageThumbnailPlugin {

  public static final String DOCUMENTS_OFFICE = "officeThumbnail";

  public static final String EXO_TITLE        = "exo:title";

  public static final String FILE_TYPE        = "jpeg";

  public static final String FILE_MIME_TYPE   = "image/jpeg";

  private static final Log   log              = ExoLogger.getExoLogger(OfficeDocumentsThumbnailPlugin.class);

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
  public FileContent getImage(String fileId, String userName) throws ObjectNotFoundException {
    UserACL userACL = CommonsUtils.getService(UserACL.class);
    RepositoryService repositoryService = CommonsUtils.getService(RepositoryService.class);
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = getUserSessionProvider(repositoryService, userACL.getUserIdentity(userName));
      Session session = sessionProvider.getSession("collaboration", repositoryService.getDefaultRepository());
      Node file = session.getNodeByUUID(fileId);
      OnlyofficeEditorService onlyofficeEditorService = CommonsUtils.getService(OnlyofficeEditorService.class);
      byte[] convertedContent;
      String fileName =  file.getProperty(EXO_TITLE).getString();
      String originalFileType = fileName.substring(fileName.lastIndexOf('.')+1);
      convertedContent = onlyofficeEditorService.convertNodeContent(file, FILE_TYPE, originalFileType, userName);
      if (convertedContent == null || convertedContent.length == 0) {
        log.error("Provided file with id " + fileId + " cannot be converted");
        return null;
      }
      return new FileContent(fileId,
                             file.getProperty(EXO_TITLE).getString(),
                             FILE_MIME_TYPE,
                             new ByteArrayInputStream(convertedContent));
    } catch (RepositoryException | RepositoryConfigurationException e) {
      throw new ObjectNotFoundException("File with id " + fileId + " not found");
    } catch (Exception e) {
      log.error("Cannot get content stream of node with uid {}", fileId, e);
    } finally {
        assert sessionProvider != null;
        sessionProvider.close();
    }
    return null;
  }
}
