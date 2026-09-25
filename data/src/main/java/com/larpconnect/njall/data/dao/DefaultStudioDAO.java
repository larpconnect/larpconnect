package com.larpconnect.njall.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallAdmin;
import com.larpconnect.njall.data.domain.DeletionFilter;
import com.larpconnect.njall.data.domain.StudioLookup;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.SessionFactory;

final class DefaultStudioDAO implements StudioDAO {

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultStudioDAO(@NjallAdmin Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<StudioLookup> findById(UUID id) {
    return findById(id, DeletionFilter.ACTIVE_ONLY);
  }

  @Override
  public Optional<StudioLookup> findById(UUID studioId, DeletionFilter filter) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var hql =
          "from StudioLookupEntity where studioId = :studioId"
              + (filter.includesDeleted() ? "" : " and deletedAt is null");
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
      var hql =
          "from StudioLookupEntity where alias = :alias"
              + (filter.includesDeleted() ? "" : " and deletedAt is null");
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
      var hql =
          "from StudioLookupEntity"
              + (filter.includesDeleted() ? "" : " where deletedAt is null")
              + " order by alias asc";
      var entities = session.createQuery(hql, StudioLookupEntity.class).list();
      return entities.stream().map(this::toStudio).collect(ImmutableList.toImmutableList());
    }
  }

  @Override
  public StudioLookup create(String alias) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var sql =
            "INSERT INTO njall_admin.studios_lookup (alias) VALUES (:alias) "
                + "RETURNING tenant_id, studio_id, created_at, updated_at";
        var row =
            session
                .createNativeQuery(sql, Object[].class)
                .setParameter("alias", alias)
                .getSingleResult();
        tx.commit();
        var tenantId = (UUID) row[0];
        var studioId = (UUID) row[1];
        var createdAt = toInstant(row[2]);
        var updatedAt = toInstant(row[3]);
        return new StudioLookup(tenantId, studioId, alias, createdAt, updatedAt, Optional.empty());
      } catch (Exception e) {
        tx.rollback();
        throw e;
      }
    }
  }

  @Override
  public Optional<StudioLookup> softDelete(UUID studioId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        var hql = "from StudioLookupEntity where studioId = :studioId and deletedAt is null";
        var entity =
            session
                .createQuery(hql, StudioLookupEntity.class)
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
        tx.rollback();
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
      case java.sql.Timestamp ts -> ts.toInstant();
      case null, default ->
          throw new IllegalArgumentException("Unsupported timestamp type: " + value);
    };
  }
}
