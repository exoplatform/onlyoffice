package org.exoplatform.onlyoffice.mock;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.cms.documents.exception.EditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.documents.model.EditorProvider;
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
  public Node createDocumentFromTemplate(Node currentNode, String title, DocumentTemplate template) throws Exception {
    return null;
  }

  /**
   * Checks for document template plugins.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasDocumentTemplatePlugins() {
    return false;
  }

  /**
   * Gets the registered template plugins.
   *
   * @return the registered template plugins
   */
  @Override
  public Set<NewDocumentTemplatePlugin> getRegisteredTemplatePlugins() {
    return null;
  }


  /**
   * Gets the registered editor plugins.
   *
   * @return the registered editor plugins
   */
  @Override
  public Set<DocumentEditorPlugin> getRegisteredEditorPlugins() {
    return null;
  }

  /**
   * Checks for document editor plugins.
   *
   * @return true, if successful
   */
  @Override
  public boolean hasDocumentEditorPlugins() {
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
  public String getPreferedEditor(String userId, String uuid, String workspace) throws RepositoryException {
    return null;
  }

  /**
   * Sets the prefered editor.
   *
   * @param userId the user id
   * @param provider the provider
   * @param uuid the uuid
   * @param workspace the workspace
   * @throws Exception the exception
   */
  @Override
  public void setPreferedEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException {
    
  }

  /**
   * Gets the editor providers.
   *
   * @return the editor providers
   */
  @Override
  public List<EditorProvider> getEditorProviders() {
    return null;
  }

  /**
   * Gets the editor provider.
   *
   * @param provider the provider
   * @return the editor provider
   * @throws EditorProviderNotFoundException the editor provider not found exception
   */
  @Override
  public EditorProvider getEditorProvider(String provider) throws EditorProviderNotFoundException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Update editor provider.
   *
   * @param editorProvider the editor provider
   * @throws EditorProviderNotFoundException the editor provider not found exception
   */
  @Override
  public void updateEditorProvider(EditorProvider editorProvider) throws EditorProviderNotFoundException {
    
  }

  @Override
  public void setCurrentDocumentEditor(String uuid, String workspace, String provider) throws RepositoryException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void initDocumentEditorsModule() {
    // TODO Auto-generated method stub
    
  }



}
