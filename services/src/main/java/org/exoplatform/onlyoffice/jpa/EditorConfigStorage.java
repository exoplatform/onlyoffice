package org.exoplatform.onlyoffice.jpa;

import org.exoplatform.onlyoffice.Config;

import java.util.List;
import java.util.Map;

public interface EditorConfigStorage {

  Map<String,Config> getConfigsByKey(String key);
  Map<String,Config> getConfigsByDocId(String docId);
  Map<String,Config> getActiveConfigsByDocId(String docId);

  /**
   * Every active config of a document, a user's older ones included, which
   * {@link #getActiveConfigsByDocId(String)} hides behind their newest.
   *
   * @param docId the document node's UUID
   * @return the active configs, newest first
   */
  List<Config> getAllActiveConfigsByDocId(String docId);
  List<Config> deleteClosedConfigsBefore(long expirationTime, int limit);
  void saveConfig(String key, Config config, boolean isNew);
  void saveConfig(List<String> keys, Config config, boolean isNew);
  void deleteConfig(String key, Config config);
  void deleteConfig(List<String> keys, Config config);
  int closeUnopenedConversionConfigs();

}
