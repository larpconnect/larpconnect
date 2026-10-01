package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultStudioLookupDAO implements StudioLookupDAO {

  private static final String HQL_FIND_BY_STUDIO_ID_ACTIVE =
      "from StudioLookupEntity where studioId = :studioId and deletedAt is null";
  private static final String HQL_FIND_BY_STUDIO_ID_ALL =
      "from StudioLookupEntity where studioId = :studioId";

  private static final String HQL_FIND_BY_ALIAS_ACTIVE =
      "from StudioLookupEntity where alias = :alias and deletedAt is null";
  private static final String HQL_FIND_BY_ALIAS_ALL =
      "from StudioLookupEntity where alias = :alias";

  private static final String HQL_LIST_ACTIVE =
      "from StudioLookupEntity where deletedAt is null order by alias asc";
  private static final String HQL_LIST_ALL = "from StudioLookupEntity order by alias asc";

  private static final String SQL_INSERT_STUDIO =
      "INSERT INTO njall_users.studios (name) VALUES (:name) RETURNING id";
  private static final String SQL_INSERT_STUDIO_LOOKUP =
      "INSERT INTO njall_admin.studios_lookup (tenant_id, alias) VALUES (:tenantId, :alias) "
          + "RETURNING studio_id, created_at, updated_at";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioLookupDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<StudioLookup> findById(UUID id) {
    return findById(id, DeletionFilter.ACTIVE_ONLY);
  }

  @Override
  public Optional<StudioLookup> findById(UUID studioId, DeletionFilter filter) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = filter.includesDeleted() ? HQL_FIND_BY_STUDIO_ID_ALL : HQL_FIND_BY_STUDIO_ID_ACTIVE;
      var entity =
          session
              .createQuery(hql, StudioLookupEntity.class)
              .setParameter("studioId", studioId)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toStudio);
    }
  }

  @Override
  public Optional<StudioLookup> findByAlias(String alias) {
    return findByAlias(alias, DeletionFilter.ACTIVE_ONLY);
  }

  @Override
  public Optional<StudioLookup> findByAlias(String alias, DeletionFilter filter) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = filter.includesDeleted() ? HQL_FIND_BY_ALIAS_ALL : HQL_FIND_BY_ALIAS_ACTIVE;
      var entity =
          session
              .createQuery(hql, StudioLookupEntity.class)
              .setParameter("alias", alias)
              .uniqueResult();
      return Optional.ofNullable(entity).map(this::toStudio);
    }
  }

  @Override
  public ImmutableList<StudioLookup> list() {
    return list(DeletionFilter.ACTIVE_ONLY);
  }

  @Override
  public ImmutableList<StudioLookup> list(DeletionFilter filter) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql = filter.includesDeleted() ? HQL_LIST_ALL : HQL_LIST_ACTIVE;
      var entities = session.createQuery(hql, StudioLookupEntity.class).list();
      return entities.stream().map(this::toStudio).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public StudioLookup create(String alias, String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var tenantId =
            session
                .createNativeQuery(SQL_INSERT_STUDIO, UUID.class)
                .setParameter("name", name)
                .getSingleResult();

        var lookupRow =
            session
                .createNativeQuery(SQL_INSERT_STUDIO_LOOKUP, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("alias", alias)
                .getSingleResult();
        tx.commit();
        var studioId = (UUID) lookupRow[0];
        var createdAt = toInstant(lookupRow[1]);
        var updatedAt = toInstant(lookupRow[2]);
        return new StudioLookup(tenantId, studioId, alias, createdAt, updatedAt, Optional.empty());
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public Optional<StudioLookup> softDelete(UUID studioId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var entity =
            session
                .createQuery(HQL_FIND_BY_STUDIO_ID_ACTIVE, StudioLookupEntity.class)
                .setParameter("studioId", studioId)
                .uniqueResult();
        if (entity == null) {
          tx.rollback();
          return Optional.empty();
        }
        var now = Instant.now();
        entity.setDeletedAt(now);
        entity.setUpdatedAt(now);
        session.merge(entity);
        tx.commit();
        return Optional.of(toStudio(entity));
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  private StudioLookup toStudio(StudioLookupEntity entity) {
    return new StudioLookup(
        entity.getTenantId(),
        entity.getStudioId(),
        entity.getAlias(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt());
  }

  static Instant toInstant(Object value) {
    return switch (value) {
      case Instant instant -> instant;
      case OffsetDateTime odt -> odt.toInstant();
      case Timestamp ts -> ts.toInstant();
      case null, default ->
          throw new IllegalArgumentException("Unsupported timestamp type: " + value);
    };
  }
}
