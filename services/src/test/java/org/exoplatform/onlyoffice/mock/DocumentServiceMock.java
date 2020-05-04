/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
   * {@inheritDoc}
   */
  @Override
  public Document findDocById(String id) throws RepositoryException {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DocumentEditorProvider> getDocumentEditorProviders() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DocumentEditorProvider getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException {
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
   * Gets the new document template providers.
   *
   * @return the new document template providers
   */
  @Override
  public List<NewDocumentTemplateProvider> getNewDocumentTemplateProviders() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPreferredEditor(String userId, String uuid, String workspace) throws RepositoryException {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void savePreferredEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentMetadataPlugin(ComponentPlugin plugin) {
  }

  @Override
  public void setCurrentDocumentProvider(String uuid, String workspace, String provider) throws RepositoryException {
  }

  @Override
  public String getCurrentDocumentProvider(String uuid, String workspace) throws RepositoryException {
    return null;
  }

}
