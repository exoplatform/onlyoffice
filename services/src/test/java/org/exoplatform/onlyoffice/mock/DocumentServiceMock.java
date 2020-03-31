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
    // TODO Auto-generated method stub
    return null;
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
    // TODO Auto-generated method stub
    return null;
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
    // TODO Auto-generated method stub
    return null;
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
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Adds the document template plugin.
   *
   * @param plugin the plugin
   */
  @Override
  public void addDocumentTemplatePlugin(ComponentPlugin plugin) {
    // TODO Auto-generated method stub
    
  }

  /**
   * Adds the document editor plugin.
   *
   * @param plugin the plugin
   */
  @Override
  public void addDocumentEditorPlugin(ComponentPlugin plugin) {
    // TODO Auto-generated method stub
    
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


  /**
   * Adds the document metadata plugin.
   *
   * @param plugin the plugin
   */
  @Override
  public void addDocumentMetadataPlugin(ComponentPlugin plugin) {
    // TODO Auto-generated method stub
    
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
   * Gets the editor provider.
   *
   * @param provider the provider
   * @return the editor provider
   * @throws DocumentEditorProviderNotFoundException the document editor provider not found exception
   */
  @Override
  public DocumentEditorProvider getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPreferedEditor(String userId, String uuid, String workspace) throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void savePreferedEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setCurrentDocumentProvider(String uuid, String workspace, String provider) throws RepositoryException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getCurrentDocumentProvider(String uuid, String workspace) throws RepositoryException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void initDocumentEditorsModule(String provider, String workspace) {
    // TODO Auto-generated method stub
    
  }


}
