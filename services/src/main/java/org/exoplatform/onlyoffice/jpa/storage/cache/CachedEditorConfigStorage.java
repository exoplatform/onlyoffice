package org.exoplatform.onlyoffice.jpa.storage.cache;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.cache.future.FutureExoCache;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.jpa.EditorConfigStorage;
import org.exoplatform.onlyoffice.jpa.storage.impl.RDBMSEditorConfigStorageImpl;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

public class CachedEditorConfigStorage implements EditorConfigStorage {

  private static final int                                     CONFIG_BY_KEY           = 1;

  private static final int                                     CONFIG_BY_DOC_ID        = 2;

  private static final int                                     ACTIVE_CONFIG_BY_DOC_ID = 3;

  private EditorConfigStorage                                  storage;

  private FutureExoCache<String, Map<String, Config>, Integer> futureCache;

  public static final String                                   CACHE_NAME              = "onlyoffice.EditorCache";

  public CachedEditorConfigStorage(final RDBMSEditorConfigStorageImpl storage, CacheService cacheService) {
    this.storage = storage;
    ExoCache<String, Map<String, Config>> configCache = cacheService.getCacheInstance(CACHE_NAME);
    Loader<String, Map<String, Config>, Integer> loader = new Loader<>() {
      @Override
      public Map<String, Config> retrieve(Integer context, String key) throws Exception {
        return switch (context) {
        case CONFIG_BY_KEY -> storage.getConfigsByKey(key);
        case CONFIG_BY_DOC_ID -> storage.getConfigsByDocId(key);
        case ACTIVE_CONFIG_BY_DOC_ID -> storage.getActiveConfigsByDocId(key);
        default -> null;
        };
      }
    };
    this.futureCache = new FutureExoCache<>(loader, configCache);

  }

  @Override
  public Map<String, Config> getConfigsByKey(String key) {
    return futureCache.get(CONFIG_BY_KEY, key);
  }

  @Override
  public Map<String, Config> getConfigsByDocId(String docId) {
    return futureCache.get(CONFIG_BY_DOC_ID, docId);
  }

  @Override
  public Map<String, Config> getActiveConfigsByDocId(String docId) {
    return futureCache.get(ACTIVE_CONFIG_BY_DOC_ID, docId);
  }

  @Override
  public List<Config> getClosedConfigsBefore(long expirationTime) {
    return storage.getClosedConfigsBefore(expirationTime);
  }

  @Override
  public int deleteClosedConfigsBefore(long expirationTime) {
    List<Config> expiredConfigs = storage.getClosedConfigsBefore(expirationTime);
    int deleted = storage.deleteClosedConfigsBefore(expirationTime);
    if (CollectionUtils.isNotEmpty(expiredConfigs)) {
      expiredConfigs.forEach(config -> {
        if (config != null) {
          futureCache.remove(config.getDocument().getKey());
          futureCache.remove(config.getDocId());
        }
      });
    }
    return deleted;
  }

  @Override
  public void saveConfig(String key, Config config, boolean isNew) {
    try {
      storage.saveConfig(key, config, isNew);
    } finally {
      futureCache.remove(key);
    }
  }

  @Override
  public void saveConfig(List<String> keys, Config config, boolean isNew) {
    if (CollectionUtils.isNotEmpty(keys)) {
      try {
        storage.saveConfig(keys, config, isNew);
      } finally {
        keys.stream()
            .filter(StringUtils::isNotBlank)
            .forEach(futureCache::remove);
      }
    }
  }

  @Override
  public void deleteConfig(String key, Config config) {
    try {
      storage.deleteConfig(key, config);
    } finally {
      futureCache.remove(key);
    }
  }

  @Override
  public void deleteConfig(List<String> keys, Config config) {
    if (CollectionUtils.isNotEmpty(keys)) {
      try {
        storage.deleteConfig(keys, config);
      } finally {
        keys.stream()
            .filter(StringUtils::isNotBlank)
            .forEach(futureCache::remove);
      }
    }
  }
}
