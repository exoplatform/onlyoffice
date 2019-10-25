package org.exoplatform.onlyoffice.mock;

import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;

/**
 * The Class DocumentServiceMock.
 */
public class DocumentServiceMock implements DocumentService {

  /**
   * Find doc by id.
   *
   * @param id the id
   * @return the document
   * @throws RepositoryException the repository exception
   */
  @Override
  public Document findDocById(String id) throws RepositoryException {
    return null;
  }

  /**
   * Gets the short link in documents app.
   *
   * @param workspaceName the workspace name
   * @param nodeId the node id
   * @return the short link in documents app
   * @throws Exception the exception
   */
  @Override
  public String getShortLinkInDocumentsApp(String workspaceName, String nodeId) throws Exception {
    return "/testlink";
  }

  /**
   * Gets the link in documents app.
   *
   * @param nodePath the node path
   * @return the link in documents app
   * @throws Exception the exception
   */
  @Override
  public String getLinkInDocumentsApp(String nodePath) throws Exception {
    return "/testlink";
  }

  /**
   * Gets the link in documents app.
   *
   * @param nodePath the node path
   * @param drive the drive
   * @return the link in documents app
   * @throws Exception the exception
   */
  @Override
  public String getLinkInDocumentsApp(String nodePath, DriveData drive) throws Exception {
    return "/testlink";
  }

  /**
   * Gets the drive of node.
   *
   * @param nodePath the node path
   * @return the drive of node
   * @throws Exception the exception
   */
  @Override
  public DriveData getDriveOfNode(String nodePath) throws Exception {
    DriveData driveData = new DriveData();
    driveData.setLabel("label");
    driveData.setName("nodeDrive");
    driveData.setHomePath("/homePath");
    driveData.setWorkspace("workspace");
    return driveData;
  }

  /**
   * Gets the drive of node.
   *
   * @param nodePath the node path
   * @param userId the user id
   * @param memberships the memberships
   * @return the drive of node
   * @throws Exception the exception
   */
  @Override
  public DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception {
    return null;
  }

}
