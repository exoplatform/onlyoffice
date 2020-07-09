/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.onlyoffice;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.onlyoffice.Config.Editor;
import org.exoplatform.services.organization.User;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveService.java 00000 Feb 14, 2013 pnedonosko $
 */
public interface OnlyofficeEditorService {

  /** The edit mode. */
  static String EDIT_MODE            = "edit";

  /** The view mode. */
  static String VIEW_MODE            = "view";

  /** The editor opened event. */
  static String EDITOR_OPENED_EVENT  = "exo.onlyoffice.editor.opened";

  /** The editor closed event. */
  static String EDITOR_CLOSED_EVENT  = "exo.onlyoffice.editor.closed";

  /** The editor saved event. */
  static String EDITOR_SAVED_EVENT   = "exo.onlyoffice.editor.saved";

  /** The editor version event. */
  static String EDITOR_VERSION_EVENT = "exo.onlyoffice.editor.version";

  /** The editor error event. */
  static String EDITOR_ERROR_EVENT   = "exo.onlyoffice.editor.error";

  /**
   * Return existing editor configuration for given user and node. If editor not
   * open for given node or user then <code>null</code> will be returned. If
   * user not valid then OnlyofficeEditorException will be thrown.
   *
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param path {@link String}
   * @return {@link Config} or <code>null</code>
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  Config getEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Return existing editor for given temporal key. If editor or user not found
   * then <code>null</code> will be returned. If user not valid then
   * OnlyofficeEditorException will be thrown.
   *
   * @param userId the user id
   * @param key the key, see {@link #getEditor(String, String, String)}
   * @return the editor by key
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  Config getEditorByKey(String userId, String key) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Create an editor configuration for given user and node.
   *
   * @param userSchema the schema
   * @param userHost the host
   * @param userPost the user post
   * @param userId {@link String}
   * @param workspace {@link String}
   * @param docId {@link String} a document reference in the workspace, see
   *          {@link #initDocument(String, String)}
   * @return {@link Config} instance in case of successful creation or
   *         <code>null</code> if local file type not supported.
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  Config createEditor(String userSchema,
                      String userHost,
                      int userPost,
                      String userId,
                      String workspace,
                      String docId) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Creates the viewer.
   *
   * @param userSchema the user schema
   * @param userHost the user host
   * @param userPost the user post
   * @param userId the user id
   * @param workspace the workspace
   * @param docId the doc id
   * @return the config
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  Config createViewer(String userSchema,
                      String userHost,
                      int userPost,
                      String userId,
                      String workspace,
                      String docId) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Update a configuration associated with given DocumentStatus 
   * instance. 
   * This operation will close the editor and it will not be usable after that.
   * 
   * @param status {@link DocumentStatus}
   * @throws OnlyofficeEditorException if editor exception happened
   * @throws RepositoryException if storage exception happened
   */
  void updateDocument(DocumentStatus status) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Inits the document and returns an ID for use within editors. Node may be
   * saved by this method if ID generation will be required, in this case it
   * should be allowed to edit the node (not locked and user has write
   * permissions).
   *
   * @param node the node of the document
   * @return the string with document ID for use within editors
   * @throws RepositoryException the repository exception
   */
  String initDocument(Node node) throws RepositoryException;

  /**
   * Inits the document and returns an ID for use within editors. Node will be
   * saved by this method.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the string
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  String initDocument(String workspace, String path) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Gets the editor page URL for opening at Platform server.
   *
   * @param node the node
   * @param scheme the scheme
   * @param host the host
   * @param port the port
   * @return the editor link
   * @throws EditorLinkNotFoundException the editor link not found exceptions
   * @throws RepositoryException the repository exception
   */
  public String getEditorLink(Node node, String scheme, String host, int port) throws RepositoryException,
                                                                               EditorLinkNotFoundException;

  /**
   * Gets the document node by its path and optionally a repository workspace.
   *
   * @param workspace the workspace, can be <code>null</code>, then a default
   *          one will be used
   * @param path the path of a document
   * @return the document or <code>null</code> if nothing found
   * @throws RepositoryException the repository exception
   * @throws BadParameterException the bad parameter exeption
   */
  Node getDocument(String workspace, String path) throws RepositoryException, BadParameterException;

  /**
   * Get file content.
   *
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @return {@link DocumentContent}
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  DocumentContent getContent(String userId, String fileKey) throws OnlyofficeEditorException, RepositoryException;

  /**
   * Check does given host can download document content by this service. It's
   * optional feature, configurable and allow only configured Document server by
   * default.
   * 
   * @param hostName {@link String}
   * @return <code>true</code> if client host with given name can download
   *         document content, <code>false</code> otherwise.
   */
  boolean canDownloadBy(String hostName);

  /**
   * Local state of editing document.
   *
   * @param userId {@link String}
   * @param fileKey {@link String}
   * @return {@link ChangeState}
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  ChangeState getState(String userId, String fileKey) throws OnlyofficeEditorException;

  /**
   * Add listener to the service.
   *
   * @param listener the listener
   */
  void addListener(OnlyofficeEditorListener listener);

  /**
   * Remove listener from the service.
   *
   * @param listener the listener
   */
  void removeListener(OnlyofficeEditorListener listener);

  /**
   * Adds DocumentTypePlugin to the service to check mimetypes of documents.
   * 
   * @param plugin - the plugin to be added
   */
  void addTypePlugin(ComponentPlugin plugin);

  /**
   *get the list of versions of the document with the given id.
   *
   * @param  workspace the workspace
   * @param docId the document id
   * @param itemParPage the versions par page
   * @param pageNum the page number to load
   * @return list of versions for node
   * @throws Exception the exception
   */
  List<Version> getVersions(String workspace, String docId, int itemParPage, int pageNum) throws Exception;

  /**
   * Checks if the node isn't locked and can be edited by the current user.
   *
   * @param node the node
   * @return true, if the current user can edit the node
   * @throws RepositoryException the repository exeption
   */
  boolean canEditDocument(Node node) throws RepositoryException;

  /**
   * Gets the document ID for given node. It will return an ID for use within an
   * editor, otherwise <code>null</code> will be returned.
   *
   * @param node the node
   * @return the document ID or <code>null</code>
   * @throws DocumentNotFoundException the document not found exception
   * @throws RepositoryException the repository exception
   * @see #initDocument(String, String)
   * @see #canEditDocument(Node)
   */
  String getDocumentId(Node node) throws DocumentNotFoundException, RepositoryException;

  /**
   * Checks if the node has compatible mime-types.
   *
   * @param node the node
   * @return true, if the node mime-types are supported
   * @throws RepositoryException the repository exeption
   */
  boolean isDocumentMimeSupported(Node node) throws RepositoryException;

  /**
   * Gets the document node by its id and optionally a repository workspace.
   *
   * @param workspace the workspace, can be <code>null</code>, then a default
   *          one will be used
   * @param uuid the id of a document
   * @return the document or <code>null</code> if nothing found
   * @throws DocumentNotFoundException the document not found exception
   * @throws RepositoryException the repository exception
   */
  Node getDocumentById(String workspace, String uuid) throws RepositoryException, DocumentNotFoundException;

  /**
   * Builds status object based on params. Obtains the config
   *
   * @param userId the userId
   * @param key the key
   * @param coEdited the coEdited
   * @param forcesaved the forceSaved
   * @param comment the comment
   * @param contentUrl the contentUrl
   */
  void downloadVersion(String userId, String key, boolean coEdited, boolean forcesaved, String comment, String contentUrl);

  /**
   * Gets the last modifier userId.
   * 
   * @param key the key
   * @return the editor user
   */
  Editor.User getLastModifier(String key);

  /**
   * Sets the last modifier userId.
   * 
   * @param key the key
   * @param userId the userId
   */
  void setLastModifier(String key, String userId);

  /**
   * Forces saving a document on document server.
   * 
   * @param userId the userId
   * @param key the key
   * @param download the download
   * @param coEdit the coedit
   * @param forcesaved the forcesaved
   * @param comment the comment
   */
  void forceSave(String userId, String key, boolean download, boolean coEdit, boolean forcesaved, String comment);

  /**
   * Gets a user.
   * 
   * @param key the key
   * @param userId the userId
   * @return the user
   */
  Editor.User getUser(String key, String userId);

  /**
   * Validates the JWT token received from the document server.
   * 
   * @param token the token
   * @param key the document key
   * @return true, if the token is correct, false otherwise
   */
  boolean validateToken(String token, String key);

  /**
   * Updates title of a document.
   * 
   * @param workspace the workspace
   * @param docId the docId
   * @param title the title
   * @param userId the userId
   */
  void updateTitle(String workspace, String docId, String title, String userId);

  /**
   * Gets the user.
   *
   * @param username the username
   * @return the user
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  User getUser(String username) throws OnlyofficeEditorException;

  /**
   * Adds file preferences (path to symlink)
   * @param node the node
   * @param userId the userId
   * @param path the path
   * @throws RepositoryException the repository exception
   */
  void addFilePreferences(Node node, String userId, String path) throws RepositoryException;

}
