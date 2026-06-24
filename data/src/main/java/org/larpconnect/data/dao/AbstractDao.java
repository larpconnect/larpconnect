package org.larpconnect.data.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.larpconnect.data.context.TenantContext;
import org.larpconnect.data.entity.NjallEntity;
import org.larpconnect.data.entity.Studio;
import org.larpconnect.data.entity.TenantEntity;

/**
 * Abstract implementation of base DAO operations with session management and soft-delete filtering.
 */
abstract class AbstractDao<E extends NjallEntity, D extends BaseDao<E, D>> {
  private final SessionFactory sessionFactory;
  private final Class<E> entityClass;

  protected AbstractDao(SessionFactory sessionFactory, Class<E> entityClass) {
    this.sessionFactory = sessionFactory;
    this.entityClass = entityClass;
  }

  protected final Session currentSession() {
    return sessionFactory.getCurrentSession();
  }

  public final Optional<E> findById(String schema, UUID id) {
    if (id == null) {
      return Optional.empty();
    }
    String oldSchema = TenantContext.getTenantSchema();
    TenantContext.setTenantSchema(schema);
    try {
      E entity = currentSession().find(entityClass, id);
      if (entity == null) {
        return Optional.empty();
      }
      if (entity instanceof TenantEntity tenantEntity && tenantEntity.getDeletedOn() != null) {
        return Optional.empty();
      }
      if (entity instanceof Studio studio && studio.getDeletedAt() != null) {
        return Optional.empty();
      }
      return Optional.of(entity);
    } finally {
      restoreSchema(oldSchema);
    }
  }

  public final void save(String schema, E entity) {
    String oldSchema = TenantContext.getTenantSchema();
    TenantContext.setTenantSchema(schema);
    try {
      currentSession().persist(entity);
    } finally {
      restoreSchema(oldSchema);
    }
  }

  public final void delete(String schema, E entity) {
    String oldSchema = TenantContext.getTenantSchema();
    TenantContext.setTenantSchema(schema);
    try {
      if (entity instanceof TenantEntity tenantEntity) {
        tenantEntity.setDeletedOn(Instant.now());
        currentSession().merge(tenantEntity);
      } else {
        Studio studio = (Studio) entity;
        studio.setDeletedAt(Instant.now());
        currentSession().merge(studio);
      }
    } finally {
      restoreSchema(oldSchema);
    }
  }

  public final List<E> findAll(String schema) {
    String oldSchema = TenantContext.getTenantSchema();
    TenantContext.setTenantSchema(schema);
    try {
      CriteriaBuilder cb = currentSession().getCriteriaBuilder();
      CriteriaQuery<E> query = cb.createQuery(entityClass);
      Root<E> root = query.from(entityClass);
      query.select(root);

      if (TenantEntity.class.isAssignableFrom(entityClass)) {
        query.where(cb.isNull(root.get("deletedOn")));
      } else {
        query.where(cb.isNull(root.get("deletedAt")));
      }

      return currentSession().createQuery(query).getResultList();
    } finally {
      restoreSchema(oldSchema);
    }
  }

  private void restoreSchema(String oldSchema) {
    if (oldSchema == null) {
      TenantContext.clear();
    } else {
      TenantContext.setTenantSchema(oldSchema);
    }
  }
}
