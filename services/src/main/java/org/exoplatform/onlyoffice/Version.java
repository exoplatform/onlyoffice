package org.exoplatform.onlyoffice;

public class Version {

  private String   author              = "";

  private long     createdTime;

  private String   displayName         = "";

  private String   fullName            = "";

  private String   name                = "";

  private String[] versionLabels       = new String[0];

  public String getAuthor() {
    return this.author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public long getCreatedTime() {
    return this.createdTime;
  }

  public void setCreatedTime(long createdTime) {
    this.createdTime = createdTime;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFullName() {
    return this.fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getVersionLabels() {
    return this.versionLabels;
  }

  public void setVersionLabels(String[] versionLabels) {
    this.versionLabels = versionLabels;
  }

}
