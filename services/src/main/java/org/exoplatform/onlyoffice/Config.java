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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Onlyoffice editor config for its JS API. <br>
 * This class implements {@link Externalizable} for serialization in eXo cache
 * (actual in cluster). Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Editor.java 00000 Feb 12, 2016 pnedonosko $
 */
public class Config implements Externalizable {

  /** The Constant LOG. */
  private static final Log                LOG             = ExoLogger.getLogger(Config.class);

  /** The Constant DATETIME_FORMAT. */
  protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy");

  /** The Constant NO_LANG. */
  protected static final String           NO_LANG         = "no_lang".intern();

  /** The Constant EMPTY. */
  protected static final String           EMPTY           = "".intern();

  /**
   * The Class Builder.
   */
  public static class Builder {

    /** The document ID in storage. */
    protected final String docId;

    /** The workspace of the storage. */
    protected final String workspace;

    /** The path. */
    protected final String path;

    /** The document type. */
    protected final String documentType;

    /** The documentserver url. */
    // DocumentServer link
    protected final String documentserverUrl;

    /** The platform REST URL base. */
    // if set will be used to generate file and callback URLs, for this config
    // and its copies for other users.
    protected String       platformRestUrl;

    /** The editor page at platform URL. */
    protected String       editorUrl;

    /** The document server secret key. **/
    protected String       documentserverSecret;

    /** The last modifier. **/
    protected String       lastModifier;

    /** The lastModified. **/
    protected String         lastModified;

    /** The display path. */
    protected String       displayPath;

    /** The comment.  */
    protected String       comment;

    /** The drive.  */
    protected String       drive;

    /** The rename allowed indicator. */
    protected Boolean      renameAllowed;

    /** The indicator to show if the document has a file activity */
    protected Boolean      isActivity;

    /** The ECMS explorer page URL. */
    protected URI          explorerUri;

    /** The document. */
    // Document
    protected String       fileType, key, title, url;

    /** The folder. */
    // Document.Info
    protected String       owner, uploaded, folder;

    /** The editor. */
    // Editor
    protected String       callbackUrl, lang, mode;

    /** The user. */
    // Editor.User
    protected String       userId, name;

    /**
     * Instantiates a new builder.
     *
     * @param documentserverUrl the document server URL
     * @param documentType the document type
     * @param workspace the workspace
     * @param path the path
     * @param docId the doc id
     */
    protected Builder(String documentserverUrl, String documentType, String workspace, String path, String docId) {
      this.documentserverUrl = documentserverUrl;
      this.documentType = documentType;
      this.docId = docId;
      this.workspace = workspace;
      this.path = path;
    }

    /**
     * Generate file and callback URLs using given Platform base URL. This will
     * erase these URLs explicitly set previously.
     *
     * @param platformRestUrl the platform URL
     * @return the builder
     */
    public Builder generateUrls(String platformRestUrl) {
      this.platformRestUrl = platformRestUrl;
      return this;
    }

    /**
     * Editor page URL.
     *
     * @param editorUrl the editor url
     * @return the builder
     */
    public Builder editorUrl(String editorUrl) {
      this.editorUrl = editorUrl;
      return this;
    }

    /**
     * URI of ECMS explorer page with a document.
     *
     * @param uri the URI of the page
     * @return the builder
     */
    public Builder explorerUri(URI uri) {
      this.explorerUri = uri;
      return this;
    }

    /**
     * Title.
     *
     * @param title the title
     * @return the builder
     */
    public Builder title(String title) {
      this.title = title;
      return this;
    }

    /**
     * Key.
     *
     * @param key the key
     * @return the builder
     */
    public Builder key(String key) {
      this.key = key;
      return this;
    }

    /**
     * File type.
     *
     * @param fileType the file type
     * @return the builder
     */
    public Builder fileType(String fileType) {
      this.fileType = fileType;
      return this;
    }

    /**
     * Url.
     *
     * @param url the url
     * @return the builder
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Owner.
     *
     * @param owner the owner
     * @return the builder
     */
    public Builder owner(String owner) {
      this.owner = owner;
      return this;
    }

    /**
     * Uploaded.
     *
     * @param uploadedTime the created time
     * @return the builder
     */
    public Builder uploaded(Calendar uploadedTime) {
      this.uploaded = DATETIME_FORMAT.format(uploadedTime.getTime());
      return this;
    }

    /**
     * Display path.
     *
     * @param displayPath the created displayPath
     * @return the builder
     */
    public Builder displayPath(String displayPath) {
      this.displayPath = displayPath;
      return this;
    }

    /**
     * Comment.
     *
     * @param comment the created comment
     * @return the builder
     */
    public Builder comment(String comment) {
      this.comment = comment;
      return this;
    }

    /**
     * Drive.
     *
     * @param drive the created drive
     * @return the builder
     */
    public Builder drive(String drive) {
      this.drive = drive;
      return this;
    }

    /**
     * Rename allowed.
     *
     * @param renameAllowed the renameAllowed
     * @return the builder
     */
    public Builder renameAllowed(Boolean renameAllowed) {
      this.renameAllowed = renameAllowed;
      return this;
    }

    /**
     * IsActivity.
     * 
     * @param isActivity the has isActivity
     * @return the builder
     */
    public Builder isActivity(Boolean isActivity) {
      this.isActivity = isActivity;
      return this;
    }

    /**
     * Folder.
     *
     * @param folder the folder
     * @return the builder
     */
    public Builder folder(String folder) {
      this.folder = folder;
      return this;
    }

    /**
     * Callback url.
     *
     * @param callbackUrl the callback url
     * @return the builder
     */
    public Builder callbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
      return this;
    }

    /**
     * Lang.
     *
     * @param lang the lang
     * @return the builder
     */
    public Builder lang(String lang) {
      this.lang = lang;
      return this;
    }

    /**
     * Mode.
     *
     * @param mode the mode
     * @return the builder
     */
    public Builder mode(String mode) {
      this.mode = mode;
      return this;
    }

    /**
     * User id.
     *
     * @param userId the user id
     * @return the builder
     */
    public Builder userId(String userId) {
      this.userId = userId;
      return this;
    }

    /**
     * User name.
     *
     * @param name the name
     * @return the builder
     */
    public Builder userName(String name) {
      this.name = name;
      return this;
    }

    /**
     * Secret
     * 
     * @param documentServerSecret the document server secret
     * @return the builder
     */
    public Builder secret(String documentServerSecret) {
      this.documentserverSecret = documentServerSecret;
      return this;
    }

    /**
     * Sets last modifier.
     *
     * @param lastModifier the last Modifier
     * @return the builder
     */
    public Builder lastModifier(String lastModifier) {
      this.lastModifier = lastModifier;
      return this;
    }

    /**
     * Sets last modified.
     *
     * @param lastModified the last modified
     * @return the builder
     */
    public Builder lastModified(String lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the config
     */
    public Config build() {
      if (platformRestUrl != null) {
        this.url = new StringBuilder(platformRestUrl).append("/onlyoffice/editor/content/")
                                                     .append(userId)
                                                     .append("/")
                                                     .append(key)
                                                     .toString();
        this.callbackUrl = new StringBuilder(platformRestUrl).append("/onlyoffice/editor/status/")
                                                             .append(userId)
                                                             .append("/")
                                                             .append(key)
                                                             .toString();
      }

      Document.Info info = new Document.Info(owner, uploaded, folder);
      Document.Permissions permissions = new Document.EditPermissions();
      Document document = new Document(key, fileType, title, url, info, permissions);
      Editor.User user = new Editor.User(userId, name);
      Editor editor = new Editor(callbackUrl, lang, mode, user);
      EditorPage editorPage = new EditorPage(comment, renameAllowed, displayPath, lastModifier, lastModified,drive);
      Config config = new Config(documentserverUrl,
                                 platformRestUrl,
                                 editorUrl,
                                 explorerUri,
                                 documentType,
                                 workspace,
                                 path,
                                 editorPage,
                                 isActivity,
                                 docId,
                                 document,
                                 editor);
      if (documentserverSecret != null && !documentserverSecret.trim().isEmpty()) {
        String jwtToken = Jwts.builder()
                              .setSubject("exo-onlyoffice")
                              .claim("document", document)
                              .claim("editorConfig", editor)
                              .claim("documentType", documentType)
                              .signWith(Keys.hmacShaKeyFor(documentserverSecret.getBytes()))
                              .compact();
        config.setToken(jwtToken);
      }

      return config;
    }

  }

  /**
   * The Onlyoffice Document.
   */
  public static class Document {

    /**
     * The Class Info.
     */
    public static class Info {

      /** The owner. */
      protected final String owner;

      /** The uploaded. */
      protected final String uploaded; // '2010-07-07 3:46 PM'

      /** The folder. */
      protected final String folder;  // 'Example Files'

      /**
       * Instantiates a new info.
       *
       * @param owner the owner
       * @param uploaded the uploaded
       * @param folder the folder
       */
      protected Info(String owner, String uploaded, String folder) {
        super();
        this.owner = owner;
        this.uploaded = uploaded;
        this.folder = folder;
      }

      /**
       * Gets the owner.
       *
       * @return the owner
       */
      public String getOwner() {
        return owner;
      }

      /**
       * Gets the uploaded.
       *
       * @return the uploaded
       */
      public String getUploaded() {
        return uploaded;
      }

      /**
       * Gets the folder.
       *
       * @return the folder
       */
      public String getFolder() {
        return folder;
      }

    }

    /**
     * The Class Permissions.
     */
    public static abstract class Permissions {

      /** The download. */
      protected final boolean download;

      /** The edit. */
      protected final boolean edit;

      /**
       * Instantiates a new permissions.
       *
       * @param download the download
       * @param edit the edit
       */
      protected Permissions(boolean download, boolean edit) {
        this.download = download;
        this.edit = edit;
      }

      /**
       * Checks if is download.
       *
       * @return the download
       */
      public boolean isDownload() {
        return download;
      }

      /**
       * Checks if is edits the.
       *
       * @return the edit
       */
      public boolean isEdit() {
        return edit;
      }

    }

    /**
     * The Class EditPermissions.
     */
    public static class EditPermissions extends Permissions {

      /**
       * Instantiates a new edits the permissions.
       */
      protected EditPermissions() {
        super(true, true);
      }
    }

    /** The file type. */
    protected final String      fileType;

    /** The key. */
    protected final String      key;

    /** The title. */
    protected final String      title;

    /** The url. */
    protected final String      url;

    /** The info. */
    protected final Info        info;

    /** The permissions. */
    protected final Permissions permissions;

    /**
     * Instantiates a new document.
     *
     * @param key the key
     * @param fileType the file type
     * @param title the title
     * @param url the url
     * @param info the info
     * @param permissions the permissions
     */
    protected Document(String key,
                       String fileType,
                       String title,
                       String url,
                       Info info,
                       Permissions permissions) {
      super();
      this.fileType = fileType;
      this.key = key;
      this.title = title;
      this.url = url;
      this.info = info;
      this.permissions = permissions;
    }

    /**
     * For user.
     *
     * @param id the id
     * @param name the name
     * @param url the url
     * @return the document
     */
    protected Document forUser(String id, String name, String url) {
      return new Document(key, fileType, title, url, info, permissions);
    }

    /**
     * Gets the file type.
     *
     * @return the fileType
     */
    public String getFileType() {
      return fileType;
    }

    /**
     * Gets the key.
     *
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
      return title;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
      return url;
    }

    /**
     * Gets the info.
     *
     * @return the info
     */
    public Info getInfo() {
      return info;
    }

    /**
     * Gets the permissions.
     *
     * @return the permissions
     */
    public Permissions getPermissions() {
      return permissions;
    }
  }

  /**
   * The Onlyoffice Editor.
   */
  public static class Editor {

    /**
     * The Class User.
     */
    public static class User {

      /** The id. */
      protected final String     id;

      /** The name. */
      protected final String     name;

      /** The lastModified timestamp. */
      protected Long             lastModified = Long.valueOf(0);

      /** The last saved timestamp. */
      protected Long             lastSaved    = Long.valueOf(0);

      /** The last link saved timestamp. */
      protected Long             linkSaved    = Long.valueOf(0);

      /** The download link. */
      protected String           downloadLink;

      /**
       * Instantiates a new user.
       *
       * @param id the id
       * @param name the name
       */
      protected User(String id, String name) {
        super();
        this.id = id;
        this.name = name;
      }

      /**
       * Gets the id.
       *
       * @return the id
       */
      public String getId() {
        return id;
      }

      /**
       * Gets the name.
       *
       * @return the name
       */
      public String getName() {
        return name;
      }

      /**
       * Gets the lastModified.
       * 
       * @return the lastModified
       */
      public long getLastModified() {
        return lastModified;
      }

      /**
       * Sets the lastModified.
       * 
       * @param lastModified the lastModified
       */
      public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
      }

      /**
       * Gets the linkSaved.
       * 
       * @return the linkSaved
       */
      public long getLinkSaved() {
        return linkSaved;
      }

      /**
       * Sets the linkSaved.
       * 
       * @param linkSaved the linkSaved
       */
      public void setLinkSaved(long linkSaved) {
        this.linkSaved = linkSaved;
      }

      /**
       * Gets the downloadLink.
       * 
       * @return the downloadLink
        */
      public String getDownloadLink() {
        return downloadLink;
      }

      /**
       * Sets the downloadLink.
       * 
       * @param downloadLink the downloadLink
       */
      public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
      }

      /**
       * Gets the last saved.
       *
       * @return the lastSaved
       */
      public long getLastSaved() {
        return lastSaved;
      }

      /**
       * Sets the last saved.
       *
       * @param lastSaved the lastSaved to set
       */
      public void setLastSaved(long lastSaved) {
        this.lastSaved = lastSaved;
      }
    }

    /** The callback url. */
    protected final String callbackUrl;

    /** The mode. */
    protected final String mode;

    /** The user. */
    protected final User   user;

    /** The lang. */
    protected String       lang;

    /**
     * Instantiates a new editor.
     *
     * @param callbackUrl the callback url
     * @param lang the lang
     * @param mode the mode
     * @param user the user
     */
    protected Editor(String callbackUrl, String lang, String mode, User user) {
      super();
      this.callbackUrl = callbackUrl;
      this.lang = lang;
      this.mode = mode;
      this.user = user;
    }

    /**
     * Gets the callback url.
     *
     * @return the callbackUrl
     */
    public String getCallbackUrl() {
      return callbackUrl;
    }

    /**
     * Gets the language of user editor.
     *
     * @return the lang can be <code>null</code> if unable to define from user
     *         profile
     */
    public String getLang() {
      return lang;
    }

    /**
     * Sets the lang.
     *
     * @param lang the lang to set
     */
    public void setLang(String lang) {
      this.lang = lang;
    }

    /**
     * Gets the mode.
     *
     * @return the mode
     */
    public String getMode() {
      return mode;
    }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public User getUser() {
      return user;
    }

    /**
     * For user.
     *
     * @param id the id
     * @param name the name
     * @param lang the lang
     * @param callbackUrl the callback url
     * @return the editor
     */
    protected Editor forUser(String id, String name, String lang, String callbackUrl) {
      User otherUser = new User(id, name);
      // FYI locks maintenance will introduce complex logic
      // simpler: each user may contain own lock token only, but don't rely on
      // others
      // otherUser.setLockToken(user.getLockToken());
      return new Editor(callbackUrl, lang, mode, otherUser);
    }
  }

  /**
   * Editor.
   *
   * @param documentserverUrl the documentserver url
   * @param documentType the document type
   * @param workspace the workspace
   * @param path the path
   * @param docId the document ID
   * @return the builder
   */
  protected static Builder editor(String documentserverUrl, String documentType, String workspace, String path, String docId) {
    return new Builder(documentserverUrl, documentType, workspace, path, docId);
  }

  /**
   * File url.
   *
   * @param baseUrl the platform url
   * @param userId the user id
   * @param key the key
   * @return the string
   */
  protected static String fileUrl(CharSequence baseUrl, String userId, String key) {
    return new StringBuilder(baseUrl).append("/onlyoffice/editor/content/").append(userId).append("/").append(key).toString();
  }

  /**
   * Callback url.
   *
   * @param baseUrl the platform url
   * @param userId the user id
   * @param key the key
   * @return the string
   */
  protected static String callbackUrl(CharSequence baseUrl, String userId, String key) {
    return new StringBuilder(baseUrl).append("/onlyoffice/editor/status/").append(userId).append("/").append(key).toString();
  }

  /** The Document Server URL. */
  private String                          documentserverUrl, documentserverJsUrl;

  /** The Platform REST URL base (to generate file URLs for users). */
  private String                          platformRestUrl;

  /** The editor page URL. */
  private String                          editorUrl;

  /** The explorer page URL (ECMS Explorer page). */
  private transient URI                   explorerUri;

  /** The workspace. */
  private String                          workspace;

  /** The path. */
  private String                          path;

  /** The editor page. */
  private EditorPage                      editorPage = new EditorPage();

  /** The isActivity */
  private Boolean                         isActivity;

  /** The document ID in storage. */
  private String                          docId;

  /** The document type. */
  private String                          documentType;

  /** The token. */
  private String                          token;

  /** The document. */
  private Document                        document;

  /** The editor config. */
  private Editor                          editorConfig;

  /** The error. */
  private String                          error;


  private transient ThreadLocal<Boolean>  sameModifier     = new ThreadLocal<>();

  private transient ThreadLocal<Calendar> previousModified = new ThreadLocal<>();

  /**
   * Marker of editor state. By default editor state is undefined and will be
   * treated as not open nor not closed. When editor will be open in Onlyoffice
   * it will send a status (1) and then need mark the editor open.
   */
  private Boolean                         open;

  /**
   * Marker for transient state between an UI closed in eXo and actually saved
   * data submitted from Onlyoffice DS.
   */
  private Boolean                         closing;

  /**  The open timestamp. */
  private Long                            openedTime;

  /**  The close timestamp. */
  private Long                            closedTime;

  /**
   * Instantiates a new config for use with {@link Externalizable} methods. User
   * by serialization.
   */
  public Config() {
    // nothing
  }

  /**
   * Editor config constructor.
   *
   * @param documentserverUrl the documentserver URL
   * @param platformRestUrl the platform url
   * @param editorUrl the editor url
   * @param explorerUri the explorer uri
   * @param documentType the document type
   * @param workspace the workspace
   * @param path the path
   * @param editorPage the editor page
   * @param isActivity the isActivity
   * @param docId the document ID
   * @param document the document
   * @param editor the editor
   */
  protected Config(String documentserverUrl,
                   String platformRestUrl,
                   String editorUrl,
                   URI explorerUri,
                   String documentType,
                   String workspace,
                   String path,
                   EditorPage editorPage,
                   Boolean isActivity,
                   String docId,
                   Document document,
                   Editor editor) {
    this.workspace = workspace;
    this.path = path;
    this.isActivity = isActivity;
    this.editorPage = editorPage;
    this.docId = docId;
    this.documentType = documentType;
    this.documentserverUrl = documentserverUrl;
    this.documentserverJsUrl = new StringBuilder(documentserverUrl).append("apps/api/documents/api.js").toString();
    this.platformRestUrl = platformRestUrl;
    this.editorUrl = editorUrl;
    this.explorerUri = explorerUri;
    this.document = document;
    this.editorConfig = editor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    // Strings
    out.writeUTF(workspace);
    out.writeUTF(path);
    out.writeUTF(documentType);
    out.writeUTF(documentserverUrl);
    out.writeUTF(documentserverJsUrl);
    out.writeUTF(platformRestUrl.toString());
    out.writeUTF(editorUrl);
    out.writeBoolean(isActivity);
    try {
      out.writeObject(explorerUri);
    } catch (Exception e) {
      LOG.warn("Error serializing explorer URI for " + path, e);
    }

    out.writeUTF(open != null ? open.toString() : EMPTY);
    // Note: closing state isn't replicable
    out.writeUTF(error != null ? error : EMPTY);

    // Objects
    // EditorPage: displayPath, comment, renameAllowed, lastModifier, lastModified, drive.
    writeUTF(out, editorPage.displayPath);
    writeUTF(out, editorPage.comment);
    writeBoolean(out, editorPage.renameAllowed);
    writeUTF(out, editorPage.lastModifier);
    writeUTF(out, editorPage.lastModified);
    writeUTF(out, editorPage.drive);

    // Document: key, fileType, title, url, info(owner, uploaded, folder)
    out.writeUTF(document.getKey());
    out.writeUTF(document.getFileType());
    out.writeUTF(document.getTitle());
    out.writeUTF(document.getUrl());
    out.writeUTF(document.getInfo().getOwner());
    out.writeUTF(document.getInfo().getUploaded());
    out.writeUTF(document.getInfo().getFolder());

    // Editor: callbackUrl, lang, mode, user(userId, name)
    out.writeUTF(editorConfig.getCallbackUrl());
    String elang = editorConfig.getLang();
    out.writeUTF(elang != null ? elang : NO_LANG);
    out.writeUTF(editorConfig.getMode());
    out.writeUTF(editorConfig.getUser().getId());
    out.writeUTF(editorConfig.getUser().getName());
    out.writeLong(editorConfig.getUser().getLastModified());
    out.writeLong(editorConfig.getUser().getLastSaved());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    // Strings
    this.workspace = in.readUTF();
    this.path = in.readUTF();
    this.documentType = in.readUTF();
    this.documentserverUrl = in.readUTF();
    this.documentserverJsUrl = in.readUTF();
    this.platformRestUrl = in.readUTF();
    this.editorUrl = in.readUTF();
    this.isActivity = in.readBoolean();
    try {
      this.explorerUri = (URI) in.readObject();
    } catch (Exception e) {
      LOG.warn("Error deserializing explorer URI for " + path, e);
      this.explorerUri = null;
    }
    String openString = in.readUTF();
    // Note: closing state isn't replicable (due to short lifecycle, few seconds
    // max and it's valuable
    // per-user session only, but in cluster with sticky sessions an user will
    // not call another server).
    if (EMPTY.equals(openString)) {
      open = closing = null;
    } else {
      open = Boolean.valueOf(openString);
      closing = new Boolean(false);
    }
    String errorString = in.readUTF();
    if (EMPTY.equals(errorString)) {
      error = null;
    } else {
      error = errorString;
    }

    // Objects
    // EditorPage: displayPath, comment, renameAllowed, lastModifier, lastModified, drive.
    String edisplayPath = in.readUTF();
    String ecomment = in.readUTF();
    Boolean erenameAllowed = in.readBoolean();
    String emodifier = in.readUTF();
    String emodified = in.readUTF();
    String edrive = in.readUTF();
    this.editorPage = new EditorPage(ecomment, erenameAllowed, edisplayPath, emodifier, emodified,edrive);

    // Document: key, fileType, title, url, info(owner, uploaded, folder)
    String dkey = in.readUTF();
    String dfileType = in.readUTF();
    String dtitle = in.readUTF();
    String durl = in.readUTF();
    String diauthor = in.readUTF();
    String dicreated = in.readUTF();
    String difolder = in.readUTF();
    Document.Info dinfo = new Document.Info(diauthor, dicreated, difolder);
    this.document = new Document(dkey, dfileType, dtitle, durl, dinfo, new Document.EditPermissions());

    // Editor: callbackUrl, lang, mode, user(userId, name)
    String ecallbackUrl = in.readUTF();
    String elang = in.readUTF();
    if (NO_LANG.equals(elang)) {
      elang = null;
    }
    String emode = in.readUTF();
    String euid = in.readUTF();
    String euname = in.readUTF();
    long lastModified = in.readLong();
    long lastSaved = in.readLong();
    Editor.User euser = new Editor.User(euid, euname);
    euser.setLastModified(lastModified);
    euser.setLastSaved(lastSaved);
    this.editorConfig = new Editor(ecallbackUrl, elang, emode, euser);
  }

  /**
   * Gets the documentserver js url.
   *
   * @return the documentserverJsUrl
   */
  public String getDocumentserverJsUrl() {
    return documentserverJsUrl;
  }

  /**
   * Gets the documentserver url.
   *
   * @return the documentserverUrl
   */
  public String getDocumentserverUrl() {
    return documentserverUrl;
  }

  /**
   * Gets the token.
   *
   * @return the token
   */
  public String getToken() {
    return token;
  }

  /**
   * Sets the token.
   *
   * @param token the token
   */
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Gets a workspace of the storage.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Gets the path in storage.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }


  /**
   * Is activity
   *
   * @return the isActivity
   */
  public Boolean isActivity() {
    return isActivity;
  }

  /**
   * Gets the editor page.
   *
   * @return the editorPage
   */
  public EditorPage getEditorPage() {
    return editorPage;
  }
  
  /**
   * Gets the document ID in storage.
   *
   * @return the docId
   */
  public String getDocId() {
    return docId;
  }

  /**
   * Gets the editor absolute URL.
   *
   * @return the editorUrl
   */
  public String getEditorUrl() {
    return editorUrl;
  }

  /**
   * Gets the explorer absolute URL.
   *
   * @return the explorerUrl
   */
  public String getExplorerUrl() {
    return explorerUri.toString();
  }

  /**
   * Gets the explorer page URI.
   *
   * @return the explorer URI
   */
  public URI getExplorerUri() {
    return explorerUri;
  }

  /**
   * Gets the document type.
   *
   * @return the documentType
   */
  public String getDocumentType() {
    return documentType;
  }

  /**
   * Gets the document.
   *
   * @return the config
   */
  public Document getDocument() {
    return document;
  }

  /**
   * Gets the openedTime.
   *
   * @return the openedTime
   */
  public Long getOpenedTime() {
    return openedTime;
  }

  /**
   * Gets the closedTime.
   *
   * @return the closedTime
   */
  public Long getClosedTime() {
    return closedTime;
  }

  /**
   * Sets the openedTime.
   *
   * @param openedTime the openedTime
   */
  protected void setOpenedTime(Long openedTime) {
    this.openedTime = openedTime;
  }

  /**
   * Sets the closedTime.
   *
   * @param closedTime the closedTime
   */
  protected void setClosedTime(Long closedTime) {
    this.closedTime = closedTime;
  }

  /**
   * Sets the editorPage.
   *
   * @param editorPage the editorPage
   */
  protected void setEditorPage(EditorPage editorPage) {
    this.editorPage = editorPage;
  }

  /**
   * Gets the editor config.
   *
   * @return the editor
   */
  public Editor getEditorConfig() {
    return editorConfig;
  }

  public void setPreviousModified(Calendar previousModified) {
    this.previousModified.set(previousModified);
  }

  public Calendar getPreviousModified() {
    return this.previousModified.get();
  }

  public void setSameModifier(Boolean samemodifier) {
    this.sameModifier.set(samemodifier);
  }

  public Boolean getSameModifier() {
    return this.sameModifier.get();
  }

  /**
   * Create a copy of this editor but for another given user.
   * 
   * @param id {@link String}
   * @param name {@link String}
   * @param lang {@link String}
   * @param documentserverSecret the documentserverSecret
   * @return {@link Config} an instance of config similar to this but with
   *         another user in the editor
   */
  public Config forUser(String id, String name, String lang, String documentserverSecret) {
    Document userDocument = document.forUser(id, name, fileUrl(platformRestUrl, id, document.getKey()));
    Editor userEditor = editorConfig.forUser(id, name, lang, callbackUrl(platformRestUrl, id, document.getKey()));
    Config config = new Config(documentserverUrl,
                               platformRestUrl,
                               editorUrl,
                               explorerUri,
                               documentType,
                               workspace,
                               path,
                               editorPage,
                               isActivity,
                               docId,
                               userDocument,
                               userEditor);
    if (documentserverSecret != null && !documentserverSecret.trim().isEmpty()) {
      String jwtToken = Jwts.builder()
                            .setSubject("exo-onlyoffice")
                            .claim("document", userDocument)
                            .claim("editorConfig", userEditor)
                            .claim("documentType", documentType)
                            .signWith(Keys.hmacShaKeyFor(documentserverSecret.getBytes()))
                            .compact();
      config.setToken(jwtToken);
    }

    return config;
  }

  /**
   * Checks if is created.
   *
   * @return true, if is created
   */
  public boolean isCreated() {
    return open == null;
  }

  /**
   * Checks if is editor open.
   *
   * @return true, if is open
   */
  public boolean isOpen() {
    return open != null ? open.booleanValue() : false;
  }

  /**
   * Checks if is editor closed (including closing state).
   *
   * @return true, if is in closed or closing state
   */
  public boolean isClosed() {
    return open != null ? !open.booleanValue() : false;
  }

  /**
   * Checks if is editor currently closing (saving the document). A closing
   * state is a sub-form of closed state.
   *
   * @return true of document in closing (saving) state
   */
  public boolean isClosing() {
    return closing != null ? closing.booleanValue() : false;
  }

  /**
   * Mark this config as open: user opened this editor.
   */
  public void open() {
    this.open = new Boolean(true);
    this.closing = new Boolean(false);
    this.openedTime = System.currentTimeMillis();
  }

  /**
   * Mark this config as closing: user already closed this editor but document
   * not yet saved in the storage. This state is actual for last user who will
   * save the document submitted by the DS. Note that only already open editor
   * can be set to closing state, otherwise this method will have not effect.
   */
  public void closing() {
    if (open != null && open.booleanValue()) {
      this.open = new Boolean(false);
      this.closing = new Boolean(true);
      this.closedTime = System.currentTimeMillis();
    }
  }

  /**
   * Mark this config as closed: the editor closed, if it was last user in the
   * editor, then its document should be saved in the storage.
   */
  public void closed() {
    this.open = new Boolean(false);
    this.closing = new Boolean(false);
    if (this.closedTime == null) {
      this.closedTime = System.currentTimeMillis();
    }
  }

  /**
   * Sets the error.
   *
   * @param error the new error
   */
  protected void setError(String error) {
    this.error = error;
  }

  /**
   * Checks for error.
   *
   * @return true, if successful
   */
  public boolean hasError() {
    return this.error != null;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public String getError() {
    return this.error;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Config) {
      Config other = (Config) obj;
      return this.documentType.equals(other.documentType) && this.workspace.equals(other.workspace)
          && this.path.equals(other.path);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(documentType);
    s.append(' ');
    s.append(workspace);
    s.append(':');
    s.append(path);
    if (open != null) {
      s.append(" (");
      s.append(open.booleanValue() ? "open" : (closing.booleanValue() ? "closing" : "closed"));
      s.append(')');
    }
    return s.toString();
  }

  /**
   * Return this config as JSON string.
   *
   * @return the string
   * @throws JsonException the json exception
   */
  public String toJSON() throws JsonException {
    JsonGeneratorImpl gen = new JsonGeneratorImpl();
    return gen.createJsonObject(this).toString();
  }

}
