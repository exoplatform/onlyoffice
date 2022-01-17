package org.exoplatform.onlyoffice.jpa.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

@Entity(name = "EditorConfigEntity")
@ExoEntity
@Table(name = "OO_EDITOR_CONFIG")
@NamedQueries({
    @NamedQuery(name = "EditorConfigEntity.getConfigByKey", query = "SELECT e FROM EditorConfigEntity e WHERE e.documentKey = :key"),
    @NamedQuery(name = "EditorConfigEntity.getConfigByDocId", query = "SELECT e FROM EditorConfigEntity e WHERE e.documentId = :docId")
})
public class EditorConfigEntity {
  @Id
  @SequenceGenerator(name = "SEQ_OO_EDITOR_CONFIG_ID", sequenceName = "SEQ_OO_EDITOR_CONFIG_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_OO_EDITOR_CONFIG_ID")
  @Column(name = "ID")
  private Long id;

  @Column(name = "DOCUMENT_ID")
  private String documentId;

  @Column(name = "WORKSPACE")
  private String workspace;

  @Column(name = "PATH")
  private String path;

  @Column(name = "DOCUMENT_TYPE")
  private String documentType;

  @Column(name = "DOCUMENT_SERVER_URL")
  private String documentServerUrl;

  @Column(name = "PLATFORM_REST_URL")
  private String platformRestUrl;

  @Column(name = "EDITOR_URL")
  private String editorUrl;

  @Column(name = "DOWNLOAD_URL")
  private String downloadUrl;

  @Column(name = "EXPLORER_URL")
  private String explorerUrl;

  @Column(name = "IS_ACTIVITY")
  private boolean isActivity;

  @Column(name = "ERROR")
  private String error;

  @Column(name = "OPEN")
  private boolean open;

  @Column(name = "CLOSING")
  private boolean closing;

  @Column(name = "OPENED_TIME")
  private Long openedTime;

  @Column(name = "CLOSED_TIME")
  private Long closedTime;

  @Column(name = "EDITOR_PAGE_LAST_MODIFIER")
  private String editorPageLastModifier;

  @Column(name = "EDITOR_PAGE_LAST_MODIFIED")
  private String editorPageLastModified;

  @Column(name = "EDITOR_PAGE_DISPLAY_PATH")
  private String editorPageDisplayPath;

  @Column(name = "EDITOR_PAGE_COMMENT")
  private String editorPageComment;

  @Column(name = "EDITOR_PAGE_DRIVE")
  private String editorPageDrive;

  @Column(name = "EDITOR_PAGE_RENAMED_ALLOWED")
  private boolean editorPageRenamedAllowed;

  @Column(name = "DOCUMENT_FILETYPE")
  private String documentFileType;

  @Column(name = "DOCUMENT_KEY")
  private String documentKey;

  @Column(name = "DOCUMENT_TITLE")
  private String documentTitle;

  @Column(name = "DOCUMENT_URL")
  private String documentUrl;

  @Column(name = "DOCUMENT_INFO_OWNER")
  private String documentInfoOwner;

  @Column(name = "DOCUMENT_INFO_UPLOADED")
  private String documentInfoUploaded;

  @Column(name = "DOCUMENT_INFO_FOLDER")
  private String documentInfoFolder;

  @Column(name = "PERMISSION_ALLOWEDIT")
  private boolean permissionAllowEdit;

  @Column(name = "EDITOR_CALLBACKURL")
  private String editorCallbackUrl;

  @Column(name = "EDITOR_LANG")
  private String editorLang;

  @Column(name = "EDITOR_MODE")
  private String editorMode;

  @Column(name = "EDITOR_USER_USERID")
  private String editorUserUserid;

  @Column(name = "EDITOR_USER_NAME")
  private String editorUserName;

  @Column(name = "EDITOR_USER_LASTMODIFIED")
  private long editorUserLastModified;

  @Column(name = "EDITOR_USER_LASTSAVED")
  private long editorUserLastSaved;

  @Column(name = "EDITOR_USER_LINKSAVED")
  private long editorUserLinkSaved;

  @Column(name = "EDITOR_USER_DOWNLOAD_LINK")
  private String editorUserDownloadLink;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public String getDocumentServerUrl() {
    return documentServerUrl;
  }

  public void setDocumentServerUrl(String documentServerUrl) {
    this.documentServerUrl = documentServerUrl;
  }

  public String getPlatformRestUrl() {
    return platformRestUrl;
  }

  public void setPlatformRestUrl(String platformRestUrl) {
    this.platformRestUrl = platformRestUrl;
  }

  public String getEditorUrl() {
    return editorUrl;
  }

  public void setEditorUrl(String editorUrl) {
    this.editorUrl = editorUrl;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getExplorerUrl() {
    return explorerUrl;
  }

  public void setExplorerUrl(String explorerUrl) {
    this.explorerUrl = explorerUrl;
  }

  public boolean isActivity() {
    return isActivity;
  }

  public void setActivity(boolean activity) {
    isActivity = activity;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public boolean isClosing() {
    return closing;
  }

  public void setClosing(boolean closing) {
    this.closing = closing;
  }

  public Long getOpenedTime() {
    return openedTime;
  }

  public void setOpenedTime(Long openedTime) {
    this.openedTime = openedTime;
  }

  public Long getClosedTime() {
    return closedTime;
  }

  public void setClosedTime(Long closedTime) {
    this.closedTime = closedTime;
  }

  public String getEditorPageLastModifier() {
    return editorPageLastModifier;
  }

  public void setEditorPageLastModifier(String editorPageLastModifier) {
    this.editorPageLastModifier = editorPageLastModifier;
  }

  public String getEditorPageLastModified() {
    return editorPageLastModified;
  }

  public void setEditorPageLastModified(String editorPageLastModified) {
    this.editorPageLastModified = editorPageLastModified;
  }

  public String getEditorPageDisplayPath() {
    return editorPageDisplayPath;
  }

  public void setEditorPageDisplayPath(String editorPageDisplayPath) {
    this.editorPageDisplayPath = editorPageDisplayPath;
  }

  public String getEditorPageComment() {
    return editorPageComment;
  }

  public void setEditorPageComment(String editorPageComment) {
    this.editorPageComment = editorPageComment;
  }

  public String getEditorPageDrive() {
    return editorPageDrive;
  }

  public void setEditorPageDrive(String editorPageDrive) {
    this.editorPageDrive = editorPageDrive;
  }

  public boolean isEditorPageRenamedAllowed() {
    return editorPageRenamedAllowed;
  }

  public void setEditorPageRenamedAllowed(boolean editorPageRenamedAllowed) {
    this.editorPageRenamedAllowed = editorPageRenamedAllowed;
  }

  public String getDocumentFileType() {
    return documentFileType;
  }

  public void setDocumentFileType(String documentFileType) {
    this.documentFileType = documentFileType;
  }

  public String getDocumentKey() {
    return documentKey;
  }

  public void setDocumentKey(String documentKey) {
    this.documentKey = documentKey;
  }

  public String getDocumentTitle() {
    return documentTitle;
  }

  public void setDocumentTitle(String documentTitle) {
    this.documentTitle = documentTitle;
  }

  public String getDocumentUrl() {
    return documentUrl;
  }

  public void setDocumentUrl(String documentUrl) {
    this.documentUrl = documentUrl;
  }

  public String getDocumentInfoOwner() {
    return documentInfoOwner;
  }

  public void setDocumentInfoOwner(String documentInfoOwner) {
    this.documentInfoOwner = documentInfoOwner;
  }

  public String getDocumentInfoUploaded() {
    return documentInfoUploaded;
  }

  public void setDocumentInfoUploaded(String documentInfoUploaded) {
    this.documentInfoUploaded = documentInfoUploaded;
  }

  public String getDocumentInfoFolder() {
    return documentInfoFolder;
  }

  public void setDocumentInfoFolder(String documentInfoFolder) {
    this.documentInfoFolder = documentInfoFolder;
  }

  public boolean isPermissionAllowEdit() {
    return permissionAllowEdit;
  }

  public void setPermissionAllowEdit(boolean permissionAllowEdit) {
    this.permissionAllowEdit = permissionAllowEdit;
  }

  public String getEditorCallbackUrl() {
    return editorCallbackUrl;
  }

  public void setEditorCallbackUrl(String editorCallbackUrl) {
    this.editorCallbackUrl = editorCallbackUrl;
  }

  public String getEditorLang() {
    return editorLang;
  }

  public void setEditorLang(String editorLang) {
    this.editorLang = editorLang;
  }

  public String getEditorMode() {
    return editorMode;
  }

  public void setEditorMode(String editorMode) {
    this.editorMode = editorMode;
  }

  public String getEditorUserUserid() {
    return editorUserUserid;
  }

  public void setEditorUserUserid(String editorUserUserid) {
    this.editorUserUserid = editorUserUserid;
  }

  public String getEditorUserName() {
    return editorUserName;
  }

  public void setEditorUserName(String editorUserName) {
    this.editorUserName = editorUserName;
  }

  public long getEditorUserLastModified() {
    return editorUserLastModified;
  }

  public void setEditorUserLastModified(long editorUserLastModified) {
    this.editorUserLastModified = editorUserLastModified;
  }

  public long getEditorUserLastSaved() {
    return editorUserLastSaved;
  }

  public void setEditorUserLastSaved(long editorUserLastSaved) {
    this.editorUserLastSaved = editorUserLastSaved;
  }

  public long getEditorUserLinkSaved() {
    return editorUserLinkSaved;
  }

  public void setEditorUserLinkSaved(long editorUserLinkSaved) {
    this.editorUserLinkSaved = editorUserLinkSaved;
  }

  public String getEditorUserDownloadLink() {
    return editorUserDownloadLink;
  }

  public void setEditorUserDownloadLink(String editorUserDownloadLink) {
    this.editorUserDownloadLink = editorUserDownloadLink;
  }
}
