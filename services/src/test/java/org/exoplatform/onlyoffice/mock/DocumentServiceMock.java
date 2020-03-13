package org.exoplatform.onlyoffice.mock;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.impl.DocumentEditorProviderImpl;
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

  /**
   * Adds the document template plugin.
   *
   * @param plugin the plugin
   */
  @Override
  public void addDocumentTemplatePlugin(ComponentPlugin plugin) {

  }

  /**
   * Adds the document editor plugin.
   *
   * @param plugin the plugin
   */
  @Override
  public void addDocumentEditorPlugin(ComponentPlugin plugin) {

  }

  /**
   * Creates the document from template.
   *
   * @param currentNode the current node
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  @Override
  public Node createDocumentFromTemplate(Node currentNode, String title, NewDocumentTemplate template) throws Exception {
    return null;
  }

  /**
   * Checks for document template plugins.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasDocumentTemplateProviders() {
    return false;
  }

  /**
   * Checks for document editor providers.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasDocumentEditorProviders() {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Gets the prefered editor.
   *
   * @param userId the user id
   * @param uuid the uuid
   * @param workspace the workspace
   * @return the prefered editor
   * @throws Exception the exception
   */
  @Override
  public String getPreferedEditor(String userId, String uuid, String workspace) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Save prefered editor.
   *
   * @param userId the user id
   * @param provider the provider
   * @param uuid the uuid
   * @param workspace the workspace
   * @throws Exception the exception
   */
  @Override
  public void savePreferedEditor(String userId, String provider, String uuid, String workspace) throws Exception {
    // TODO Auto-generated method stub

  }

  /**
   * Gets the editor provider.
   *
   * @param provider the provider
   * @return the editor provider
   * @throws DocumentEditorProviderNotFoundException the document editor provider not found exception
   */
  @Override
  public DocumentEditorProviderImpl getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the document editor providers.
   *
   * @return the document editor providers
   */
  @Override
  public List<DocumentEditorProvider> getDocumentEditorProviders() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the new document template providers.
   *
   * @return the new document template providers
   */
  @Override
  public List<NewDocumentTemplateProvider> getNewDocumentTemplateProviders() {
    // TODO Auto-generated method stub
    return null;
  }

}
