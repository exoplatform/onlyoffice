
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

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorListener.java 00000 Mar 4, 2016 pnedonosko $
 */
public interface OnlyofficeEditorListener {

  /** The FILE_DELEDED_ERROR code. */
  long FILE_DELETED_ERROR = -1;

  /**
   * New editor just created.
   *
   * @param status the status
   */
  void onCreate(DocumentStatus status);

  /**
   * Existing editor obtained by user.
   *
   * @param status the status
   */
  void onGet(DocumentStatus status);

  /**
   * User joined co-editing document (it's second or more user). 
   *
   * @param status the status
   */
  void onJoined(DocumentStatus status);

  /**
   * User leaved co-editing document (it's at least second user gone). 
   *
   * @param status the status
   */
  void onLeaved(DocumentStatus status);

  /**
   * Document was saved and editor released.
   *
   * @param status the status
   */
  void onSaved(DocumentStatus status);

  /**
   * Error saving document in editor. Error message if found, will be set in the {@link Config#getError()}.
   *
   * @param status the status
   */
  void onError(DocumentStatus status);
  
  /**
   * Invoked when on content update occurs.
   *
   * @param workspace the workspace
   * @param fileId the file id
   * @param userId the user id
   */
  void onContentUpdated(String workspace, String fileId, String userId);

}
