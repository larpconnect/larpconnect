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
import org.larpconnect.data.entity.NjallEntity;
import org.larpconnect.data.entity.SoftDeletable;
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
    checkSchema(schema);
    E entity = currentSession().find(entityClass, id);
    if (entity == null) {
      return Optional.empty();
    }
    if (((SoftDeletable) entity).getDeletedTime() != null) {
      return Optional.empty();
    }
    return Optional.of(entity);
  }

  public final void save(String schema, E entity) {
    checkSchema(schema);
    currentSession().merge(entity);
  }

  public final void delete(String schema, E entity) {
    checkSchema(schema);
    SoftDeletable softDeletable = (SoftDeletable) entity;
    softDeletable.setDeletedTime(Instant.now());
    currentSession().merge(entity);
  }

  public final List<E> findAll(String schema) {
    checkSchema(schema);
    CriteriaBuilder cb = currentSession().getCriteriaBuilder();
    CriteriaQuery<E> query = cb.createQuery(entityClass);
    Root<E> root = query.from(entityClass);
    query.select(root);

    String propertyName =
        TenantEntity.class.isAssignableFrom(entityClass) ? "deletedOn" : "deletedAt";
    query.where(cb.isNull(root.get(propertyName)));

    return currentSession().createQuery(query).getResultList();
  }

  private void checkSchema(String schema) {
    Object tenantId = currentSession().getTenantIdentifier();
    if (!java.util.Objects.equals(schema, tenantId)) {
      throw new IllegalArgumentException(
          String.format("Schema '%s' does not match session tenant '%s'", schema, tenantId));
    }
  }
}
