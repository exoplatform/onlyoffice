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

  /**
   * Opens a config at the given time, unless its stored error is the given
   * one: a single conditional update, so a close written meanwhile by another
   * transaction is never overwritten.
   *
   * @param id the config's database id
   * @param openedTime the opened time to set
   * @param closedError the error that keeps the config closed
   * @return the number of opened configs, 0 or 1
   */
  int reopenConfig(long id, long openedTime, String closedError);

}
