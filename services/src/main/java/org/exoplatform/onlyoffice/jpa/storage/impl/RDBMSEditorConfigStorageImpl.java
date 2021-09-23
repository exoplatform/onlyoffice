package org.exoplatform.onlyoffice.jpa.storage.impl;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.jpa.EditorConfigDAO;
import org.exoplatform.onlyoffice.jpa.EditorConfigStorage;
import org.exoplatform.onlyoffice.jpa.entities.EditorConfigEntity;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RDBMSEditorConfigStorageImpl implements EditorConfigStorage {

  private final EditorConfigDAO editorConfigDAO;

  public RDBMSEditorConfigStorageImpl (EditorConfigDAO editorConfigDAO) {
    this.editorConfigDAO=editorConfigDAO;
  }
  @Override
  @ExoTransactional
  public Map<String,Config> getConfigsByKey(String key) {
    List<EditorConfigEntity> entities = editorConfigDAO.getConfigByKey(key);
    return entities.stream()
                   .collect(Collectors.toMap(EditorConfigEntity::getEditorUserUserid,
                                             this::buildFromEntity));
  }

  @Override
  @ExoTransactional
  public Map<String,Config> getConfigsByDocId(String docId) {
    List<EditorConfigEntity> entities = editorConfigDAO.getConfigByDocId(docId);
    return entities.stream()
                   .collect(Collectors.toConcurrentMap(EditorConfigEntity::getEditorUserUserid,
                                                       this::buildFromEntity));  }



  @Override
  @ExoTransactional
  public void saveConfig(String key, Config config, boolean isNew) {
    if (isNew) {
      EditorConfigEntity entity = buildFromDTO(config);
      editorConfigDAO.create(entity);
      config.setDatabaseId(entity.getId());
    } else {
      EditorConfigEntity entity = editorConfigDAO.find(config.getDatabaseId());
      if (entity!=null) {
        entity = buildFromDTO(config);
        editorConfigDAO.update(entity);
      } else {
        throw new RuntimeException("Unable to save OO Config");
      }
    }
  }

  @Override
  public void saveConfig(List<String> keys, Config config, boolean isNew) {
    this.saveConfig("", config, isNew);
  }

  @Override
  @ExoTransactional
  public void deleteConfig(String key, Config config) {
    EditorConfigEntity entity = editorConfigDAO.find(config.getDatabaseId());
    if (entity!=null) {
      editorConfigDAO.delete(entity);
    }
  }
  @Override
  public void deleteConfig(List<String> keys, Config config) {
    this.deleteConfig("",config);
  }


  private EditorConfigEntity buildFromDTO(Config config) {
    EditorConfigEntity result = new EditorConfigEntity();

    result.setId(config.getDatabaseId());

    result.setDocumentId(config.getDocId());
    result.setWorkspace(config.getWorkspace());
    result.setPath(config.getPath());
    result.setDocumentType(config.getDocumentType());
    result.setDocumentServerUrl(config.getDocumentserverUrl());
    result.setPlatformRestUrl(config.getPlatformRestUrl());
    result.setEditorUrl(config.getEditorUrl());
    result.setDownloadUrl(config.getDownloadUrl());
    result.setExplorerUrl(config.getExplorerUrl());
    result.setActivity(config.isActivity());
    result.setError(config.getError());
    result.setOpen(config.isOpen());
    result.setClosing(config.isClosing());
    result.setOpenedTime(config.getOpenedTime());
    result.setClosedTime(config.getClosedTime());
    result.setEditorPageLastModifier(config.getEditorPage().getLastModifier());
    result.setEditorPageLastModified(config.getEditorPage().getLastModified());
    result.setEditorPageDisplayPath(config.getEditorPage().getDisplayPath());
    result.setEditorPageComment(config.getEditorPage().getComment());
    result.setEditorPageDrive(config.getEditorPage().getDrive());
    result.setEditorPageRenamedAllowed(config.getEditorPage().getRenameAllowed());
    result.setDocumentFileType(config.getDocument().getFileType());
    result.setDocumentKey(config.getDocument().getKey());
    result.setDocumentTitle(config.getDocument().getTitle());
    result.setDocumentUrl(config.getDocument().getUrl());
    result.setDocumentInfoOwner(config.getDocument().getInfo().getOwner());
    result.setDocumentInfoUploaded(config.getDocument().getInfo().getUploaded());
    result.setDocumentInfoFolder(config.getDocument().getInfo().getFolder());
    result.setPermissionAllowEdit(config.getDocument().getPermissions().isEdit());
    result.setEditorCallbackUrl(config.getEditorConfig().getCallbackUrl());
    result.setEditorLang(config.getEditorConfig().getLang());
    result.setEditorMode(config.getEditorConfig().getMode());
    result.setEditorUserUserid(config.getEditorConfig().getUser().getId());
    result.setEditorUserName(config.getEditorConfig().getUser().getName());
    result.setEditorUserLastModified(config.getEditorConfig().getUser().getLastModified());
    result.setEditorUserLastSaved(config.getEditorConfig().getUser().getLastSaved());
    result.setEditorUserLinkSaved(config.getEditorConfig().getUser().getLinkSaved());
    result.setEditorUserDownloadLink(config.getEditorConfig().getUser().getDownloadLink());

    return result;


  }


  private Config buildFromEntity(EditorConfigEntity entity) {


    Config.Builder builder = Config.editor(entity.getDocumentServerUrl(), entity.getDocumentType(),
                                           entity.getWorkspace(), entity.getPath(), entity.getDocumentId());
    builder.owner(entity.getEditorUserUserid());
    builder.fileType(entity.getDocumentFileType());
    builder.uploaded(entity.getDocumentInfoUploaded());
    builder.displayPath(entity.getEditorPageDisplayPath());
    builder.comment(entity.getEditorPageComment());
    builder.drive(entity.getEditorPageDrive());
    builder.renameAllowed(entity.isEditorPageRenamedAllowed());
    builder.isActivity(entity.isActivity());
    builder.folder(entity.getDocumentInfoFolder());
    builder.lang(entity.getEditorLang());
    builder.mode(entity.getEditorMode());
    builder.title(entity.getDocumentTitle());
    builder.userId(entity.getEditorUserUserid());
    builder.userName(entity.getEditorUserName());
    builder.lastModifier(entity.getEditorPageLastModifier());
    builder.lastModified(entity.getEditorPageLastModified());
    builder.key(entity.getDocumentKey());
    builder.generateUrls(entity.getPlatformRestUrl());
    builder.editorUrl(entity.getEditorUrl());
    builder.explorerUri(entity.getExplorerUrl());
    builder.secret(getDocumentServiceSecret());
    builder.setAllowEdition(entity.isPermissionAllowEdit());

    Config result = builder.build();
    result.setError(entity.getError());
    result.setDatabaseId(entity.getId());
    result.setOpen(entity.isOpen());
    result.setClosing(entity.isClosing());
    result.setOpenedTime(entity.getOpenedTime());
    result.setClosedTime(entity.getClosedTime());

    result.getEditorConfig().getUser().setLastModified(entity.getEditorUserLastModified());
    result.getEditorConfig().getUser().setLastSaved(entity.getEditorUserLastSaved());
    result.getEditorConfig().getUser().setLinkSaved(entity.getEditorUserLinkSaved());
    result.getEditorConfig().getUser().setDownloadLink(entity.getEditorUserDownloadLink());
    return result;
  }

  private String getDocumentServiceSecret() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    OnlyofficeEditorService editorService = container.getComponentInstanceOfType(OnlyofficeEditorService.class);
    return editorService.getDocumentServiceSecret();
  }
}
