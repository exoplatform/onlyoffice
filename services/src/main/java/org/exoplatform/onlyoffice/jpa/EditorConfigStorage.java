package org.exoplatform.onlyoffice.jpa;

import org.exoplatform.onlyoffice.Config;

import java.util.List;
import java.util.Map;

public interface EditorConfigStorage {

  Map<String,Config> getConfigsByKey(String key);
  Map<String,Config> getConfigsByDocId(String docId);
  Map<String,Config> getActiveConfigsByDocId(String docId);
  List<Config> getClosedConfigsBefore(long expirationTime);
  int deleteClosedConfigsBefore(long expirationTime);
  void saveConfig(String key, Config config, boolean isNew);
  void saveConfig(List<String> keys, Config config, boolean isNew);
  void deleteConfig(String key, Config config);
  void deleteConfig(List<String> keys, Config config);

}
