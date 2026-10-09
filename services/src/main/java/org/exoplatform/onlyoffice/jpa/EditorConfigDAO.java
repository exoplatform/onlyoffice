package org.exoplatform.onlyoffice.jpa;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.onlyoffice.jpa.entities.EditorConfigEntity;

import java.util.List;

public interface EditorConfigDAO extends GenericDAO<EditorConfigEntity,Long> {

  List<EditorConfigEntity> getConfigByKey(String key);
  List<EditorConfigEntity> getConfigByDocId(String docId);
  List<EditorConfigEntity> getActiveConfigByDocId(String docId);
  List<EditorConfigEntity> getClosedConfigBefore(long expirationTime, int limit);

  /**
   * Closes, at the given time, the active configs built for a conversion that
   * no editor ever opened: not open, no closed time (a closing config has
   * one), and an empty explorer URL, which only a conversion config and its
   * copies have.
   *
   * @param closedTime the closed time to set
   * @return the number of closed configs
   */
  int closeUnopenedConversionConfigs(long closedTime);

}
