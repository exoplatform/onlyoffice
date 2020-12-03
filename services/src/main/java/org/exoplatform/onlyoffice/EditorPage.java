package org.exoplatform.onlyoffice;

/**
 * The Class EditorPage.
 */
public class EditorPage {

  /** The comment. */
  protected String comment;

  /** The drive. */
  protected String drive;

  /** The rename allowed. */
  protected Boolean renameAllowed;

  /** The display path. */
  protected String displayPath;

  /** The last modifier. */
  protected String lastModifier;

  /** The last modified. */
  protected String lastModified;

  /**
   * Instantiates a new editor page.
   *
   * @param comment the comment
   * @param drive the drive
   * @param renameAllowed the rename allowed
   * @param displayPath the display path
   * @param lastModifier the last modifier
   * @param lastModified the last modified
   */
  public EditorPage(String comment, Boolean renameAllowed, String displayPath, String lastModifier, String lastModified,String drive) {
    super();
    this.comment = comment;
    this.renameAllowed = renameAllowed;
    this.displayPath = displayPath;
    this.lastModifier = lastModifier;
    this.lastModified = lastModified;
    this.drive = drive;
  }

  /**
   * Instantiates a new editor page.
   */
  public EditorPage() {
  }

  /**
   * Gets the comment.
   *
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * Gets the drive.
   *
   * @return the drive
   */
  public String getDrive() {
    return drive;
  }

  /**
   * Sets the comment.
   *
   * @param comment the new comment
   */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /**
   * Sets the drive.
   *
   * @param drive the new drive
   */
  public void setDrive(String drive) {
    this.drive = drive;
  }

  /**
   * Gets the rename allowed.
   *
   * @return the rename allowed
   */
  public Boolean getRenameAllowed() {
    return renameAllowed;
  }

  /**
   * Sets the rename allowed.
   *
   * @param renameAllowed the new rename allowed
   */
  public void setRenameAllowed(Boolean renameAllowed) {
    this.renameAllowed = renameAllowed;
  }

  /**
   * Gets the display path.
   *
   * @return the display path
   */
  public String getDisplayPath() {
    return displayPath;
  }

  /**
   * Sets the display path.
   *
   * @param displayPath the new display path
   */
  public void setDisplayPath(String displayPath) {
    this.displayPath = displayPath;
  }

  /**
   * Gets the last modifier.
   *
   * @return the last modifier
   */
  public String getLastModifier() {
    return lastModifier;
  }

  /**
   * Sets the last modifier.
   *
   * @param lastModifier the new last modifier
   */
  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  /**
   * Gets the last modified.
   *
   * @return the last modified
   */
  public String getLastModified() {
    return lastModified;
  }

  /**
   * Sets the last modified.
   *
   * @param lastModified the new last modified
   */
  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }
 
}
