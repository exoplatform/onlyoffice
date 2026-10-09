package org.exoplatform.onlyoffice.jpa.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.onlyoffice.jpa.EditorConfigDAO;
import org.exoplatform.onlyoffice.jpa.entities.EditorConfigEntity;

import jakarta.persistence.Query;
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
  public List<EditorConfigEntity> getClosedConfigBefore(long expirationTime, int limit) {
    TypedQuery<EditorConfigEntity> query = getEntityManager()
        .createNamedQuery("EditorConfigEntity.getClosedConfigBefore",EditorConfigEntity.class);
    query.setParameter("expirationTime", expirationTime);
    query.setMaxResults(limit);
    return query.getResultList();
  }

  @Override
  public int closeUnopenedConversionConfigs(long closedTime) {
    Query query = getEntityManager().createNamedQuery("EditorConfigEntity.closeUnopenedConversionConfigs");
    query.setParameter("closedTime", closedTime);
    int closed = query.executeUpdate();
    // the bulk update bypasses the persistence context: entities it already
    // holds would keep the closed time they were loaded with
    getEntityManager().clear();
    return closed;
  }

}
