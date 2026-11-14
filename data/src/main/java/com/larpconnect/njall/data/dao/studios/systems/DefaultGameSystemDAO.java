package com.larpconnect.njall.data.dao.studios.systems;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.GameSystem;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jspecify.annotations.Nullable;

final class DefaultGameSystemDAO implements GameSystemDAO {

  private static final String ENTITY_TYPE_SYSTEM = "System";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";
  private static final String LIST_SYSTEMS_HQL =
      """
      SELECT e, s FROM EntityBaseEntity e, GameSystemEntity s
      WHERE e.tenantId = :tenantId AND s.tenantId = :tenantId
        AND e.id = s.id AND e.entityType = :entityType AND e.deletedOn IS NULL
      ORDER BY LOWER(s.name) ASC, e.createdOn DESC
      """;
  private static final String EXISTS_BY_NAME_HQL =
      """
      SELECT s.id FROM GameSystemEntity s
      WHERE s.tenantId = :tenantId AND LOWER(s.name) = :lowerName
      """;
  private static final String EXISTS_BY_NAME_EXCLUDING_ID_HQL =
      """
      SELECT s.id FROM GameSystemEntity s
      WHERE s.tenantId = :tenantId AND LOWER(s.name) = :lowerName AND s.id != :excludeId
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultGameSystemDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<GameSystem> findById(UUID tenantId, UUID systemId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, systemId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var system = session.find(GameSystemEntity.class, entityId);
        tx.commit();
        if (!isActiveSystem(entity, system)) {
          return Optional.empty();
        }
        return Optional.of(toGameSystem(entity, system));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<GameSystem> listAll(UUID tenantId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows =
            session
                .createQuery(LIST_SYSTEMS_HQL, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("entityType", ENTITY_TYPE_SYSTEM)
                .getResultList();
        tx.commit();
        return mapRows(rows);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public GameSystem create(UUID tenantId, UUID generator, String name, Optional<String> summary) {
    requireNonBlankName(name);
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator);
        requireUniqueSystemName(session, tenantId, name, Optional.empty());
        var systemId = UUID.randomUUID();
        var now = Instant.now();
        var entity = createBaseEntity(tenantId, systemId, generator, summary, now);
        session.persist(entity);

        var system = createGameSystemEntity(tenantId, systemId, name);
        session.persist(system);
        session.flush();

        tx.commit();
        return toGameSystem(entity, system);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<GameSystem> patch(UUID tenantId, UUID systemId, PatchGameSystemParams params) {
    params.name().ifPresent(DefaultGameSystemDAO::requireNonBlankName);
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, systemId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var system = session.find(GameSystemEntity.class, entityId);
        if (!isActiveSystem(entity, system)) {
          tx.commit();
          return Optional.empty();
        }
        if (params.name().isPresent()) {
          requireUniqueSystemName(
              session, tenantId, params.name().orElseThrow(), Optional.of(systemId));
        }
        applySystemPatch(entity, system, params);
        session.merge(entity);
        session.merge(system);
        session.flush();
        tx.commit();
        return Optional.of(toGameSystem(entity, system));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID systemId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, systemId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (entity == null
            || entity.getDeletedOn() != null
            || !ENTITY_TYPE_SYSTEM.equals(entity.getEntityType())) {
          tx.commit();
          return false;
        }
        var now = Instant.now();
        entity.setDeletedOn(now);
        entity.setUpdatedOn(now);
        session.merge(entity);
        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private void requireUniqueSystemName(
      Session session, UUID tenantId, String name, Optional<UUID> excludeId) {
    if (existsSystemName(session, tenantId, name, excludeId)) {
      throw createIllegalStateException(
          "Game system with name '" + name + "' already exists in tenant " + tenantId);
    }
  }

  private boolean existsSystemName(
      Session session, UUID tenantId, String name, Optional<UUID> excludeId) {
    var hql = excludeId.isPresent() ? EXISTS_BY_NAME_EXCLUDING_ID_HQL : EXISTS_BY_NAME_HQL;
    var query =
        session
            .createQuery(hql, UUID.class)
            .setParameter("tenantId", tenantId)
            .setParameter("lowerName", name.toLowerCase(Locale.ROOT));
    excludeId.ifPresent(id -> query.setParameter("excludeId", id));
    return !query.getResultList().isEmpty();
  }

  private static ImmutableList<GameSystem> mapRows(List<Object[]> rows) {
    return rows.stream()
        .map(DefaultGameSystemDAO::mapRowToSystem)
        .collect(ImmutableList.toImmutableList());
  }

  private static GameSystem mapRowToSystem(Object[] row) {
    return toGameSystem((EntityBaseEntity) row[0], (GameSystemEntity) row[1]);
  }

  private static void requireNonBlankName(String name) {
    if (name.isBlank()) {
      throw createIllegalArgumentException("Game system name must not be blank");
    }
  }

  private static boolean isActiveSystem(
      @Nullable EntityBaseEntity entity, @Nullable GameSystemEntity system) {
    return entity != null
        && system != null
        && entity.getDeletedOn() == null
        && ENTITY_TYPE_SYSTEM.equals(entity.getEntityType());
  }

  private static void applySystemPatch(
      EntityBaseEntity entity, GameSystemEntity system, PatchGameSystemParams params) {
    params.name().ifPresent(system::setName);
    applyNullableUpdate(params.summary(), params.clearSummary(), entity::setSummary);
    entity.setUpdatedOn(Instant.now());
  }

  private static <T> void applyNullableUpdate(
      Optional<T> value, boolean clear, Consumer<@Nullable T> setter) {
    if (clear) {
      setter.accept(null);
      return;
    }
    value.ifPresent(setter);
  }

  private static void rollbackQuietly(Transaction tx, Exception e) {
    try {
      tx.rollback();
    } catch (Exception rollbackException) {
      e.addSuppressed(rollbackException);
    }
  }

  private static IllegalArgumentException createIllegalArgumentException(String message) {
    return new IllegalArgumentException(message);
  }

  private static IllegalStateException createIllegalStateException(String message) {
    return new IllegalStateException(message);
  }

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID id, UUID generator, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, id, generator, ENTITY_TYPE_SYSTEM, summary.orElse(null), now, now, null);
  }

  private static GameSystemEntity createGameSystemEntity(UUID tenantId, UUID id, String name) {
    return new GameSystemEntity(tenantId, id, name);
  }

  private static GameSystem toGameSystem(EntityBaseEntity entity, GameSystemEntity system) {
    return new GameSystem(
        entity.getId(),
        entity.getGenerator(),
        system.getName(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
