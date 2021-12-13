package org.exoplatform.onlyoffice.jpa.storage.cache;

import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.jpa.EditorConfigStorage;
import org.exoplatform.onlyoffice.jpa.storage.impl.RDBMSEditorConfigStorageImpl;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import java.util.List;
import java.util.Map;

public class CachedEditorConfigStorage implements EditorConfigStorage {

  private EditorConfigStorage storage;

  private final ExoCache<String, Map<String, Config>> configCache;

  public static final String     CACHE_NAME               = "onlyoffice.EditorCache";


  public CachedEditorConfigStorage(final RDBMSEditorConfigStorageImpl storage, CacheService cacheService) {
    this.storage = storage;
    configCache = cacheService.getCacheInstance(CACHE_NAME);
  }

  @Override
  public Map<String, Config> getConfigsByKey(String key) {
    Map<String, Config> configs = configCache.get(key);
    if (configs != null) {
      return configs;
    }
    configs = storage.getConfigsByKey(key);
    if (configs != null && !configs.isEmpty()) {
      configCache.put(key, configs);
    }
    return configs;
  }

  @Override
  public Map<String, Config> getConfigsByDocId(String docId) {
    Map<String, Config> configs = configCache.get(docId);
    if (configs != null) {
      return configs;
    }
    configs = storage.getConfigsByDocId(docId);
    if (configs != null && !configs.isEmpty()) {
      configCache.put(docId, configs);
    }
    return configs;
  }

  @Override
  public void saveConfig(String key, Config config, boolean isNew) {
    storage.saveConfig(key,config,isNew);
    configCache.remove(key);
  }

  @Override
  public void saveConfig(List<String> keys, Config config, boolean isNew) {
    storage.saveConfig(keys,config,isNew);
    keys.forEach(configCache::remove);
  }

  @Override
  public void deleteConfig(String key, Config config) {
    storage.deleteConfig(key,config);
    configCache.remove(key);
  }

  @Override
  public void deleteConfig(List<String> keys, Config config) {
    storage.deleteConfig(keys,config);
    keys.forEach(configCache::remove);
  }
}
