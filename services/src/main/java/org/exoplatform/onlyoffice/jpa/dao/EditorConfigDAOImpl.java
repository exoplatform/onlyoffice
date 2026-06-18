package org.exoplatform.onlyoffice.jpa.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.onlyoffice.jpa.EditorConfigDAO;
import org.exoplatform.onlyoffice.jpa.entities.EditorConfigEntity;

import jakarta.persistence.TypedQuery;
import java.util.List;

public class EditorConfigDAOImpl extends GenericDAOJPAImpl<EditorConfigEntity, Long> implements EditorConfigDAO {

  @Override
  public List<EditorConfigEntity> getConfigByKey(String key) {
    TypedQuery<EditorConfigEntity> query = getEntityManager()
        .createNamedQuery("EditorConfigEntity.getConfigByKey",EditorConfigEntity.class);
    query.setParameter("key", key);
    return query.getResultList();
  }

  @Override
  public List<EditorConfigEntity> getConfigByDocId(String docId) {
    TypedQuery<EditorConfigEntity> query = getEntityManager()
        .createNamedQuery("EditorConfigEntity.getConfigByDocId",EditorConfigEntity.class);
    query.setParameter("docId", docId);
    return query.getResultList();
  }
  @Override
  public List<EditorConfigEntity> getActiveConfigByDocId(String docId) {
    TypedQuery<EditorConfigEntity> query = getEntityManager()
        .createNamedQuery("EditorConfigEntity.getActiveConfigByDocId",EditorConfigEntity.class);
    query.setParameter("docId", docId);
    return query.getResultList();
  }

  @Override
  public List<EditorConfigEntity> getClosedConfigBefore(long expirationTime) {
    TypedQuery<EditorConfigEntity> query = getEntityManager()
        .createNamedQuery("EditorConfigEntity.getClosedConfigBefore",EditorConfigEntity.class);
    query.setParameter("expirationTime", expirationTime);
    return query.getResultList();
  }

}
