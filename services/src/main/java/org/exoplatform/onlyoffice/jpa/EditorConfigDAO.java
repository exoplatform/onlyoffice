package org.exoplatform.onlyoffice.jpa;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.onlyoffice.jpa.entities.EditorConfigEntity;

import java.util.List;

public interface EditorConfigDAO extends GenericDAO<EditorConfigEntity,Long> {

  List<EditorConfigEntity> getConfigByKey(String key);
  List<EditorConfigEntity> getConfigByDocId(String docId);

}
