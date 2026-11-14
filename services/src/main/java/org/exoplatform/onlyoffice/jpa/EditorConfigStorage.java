package org.exoplatform.onlyoffice.jpa;

import org.exoplatform.onlyoffice.Config;

import java.util.List;
import java.util.Map;

public interface EditorConfigStorage {

  /**
   * The error of a config closed because the document's content was written
   * outside its editor session: such a config is never opened again, and a
   * save of a copy read before the close keeps it closed.
   */
  String CONTENT_UPDATED_OUTSIDE_EDITOR = "Document content updated outside the editor";

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

  /**
   * Opens a stored config at its opened time, unless a content write outside
   * the editor closed it ({@link #CONTENT_UPDATED_OUTSIDE_EDITOR}), whenever
   * the given copy was read.
   *
   * @param config the config to open, its opened time set
   * @return true if the stored config was opened
   */
  boolean reopenConfig(Config config);

}
