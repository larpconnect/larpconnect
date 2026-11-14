package com.larpconnect.njall.data.dao.studios.collections;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.Collection;
import com.larpconnect.njall.data.domain.studios.ExpandedEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultCollectionDAO implements CollectionDAO {

  private static final String ENTITY_TYPE_COLLECTION = "OrderedCollection";
  static final String SET_TENANT_CONFIG = "SELECT set_config('app.tenant_id', :tenantId, true)";
  static final String EXISTS_BY_OWNER_AND_NAME_HQL =
      """
      SELECT COUNT(c) FROM CollectionEntity c, EntityBaseEntity e
      WHERE c.tenantId = :tenantId AND c.ownerId = :ownerId AND c.name = :name
        AND c.tenantId = e.tenantId AND c.id = e.id AND e.deletedOn IS NULL
      """;
  static final String EXISTS_BY_OWNER_AND_NAME_EXCLUDING_ID_HQL =
      """
      SELECT COUNT(c) FROM CollectionEntity c, EntityBaseEntity e
      WHERE c.tenantId = :tenantId AND c.ownerId = :ownerId AND c.name = :name
        AND c.tenantId = e.tenantId AND c.id = e.id AND e.deletedOn IS NULL
        AND c.id != :excludeId
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCollectionDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public boolean isActiveActor(UUID tenantId, UUID actorId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        boolean active = ActorGeneratorValidator.isActiveActor(session, tenantId, actorId);
        tx.commit();
        return active;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Collection> findById(UUID tenantId, UUID actorId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var scoped = loadScopedCollection(session, tenantId, actorId, collectionId);
        if (scoped.isEmpty()) {
          tx.commit();
          return Optional.empty();
        }
        var found = scoped.orElseThrow();
        var itemIds = CollectionItemHelper.queryActiveItemIds(session, tenantId, collectionId);
        tx.commit();
        return Optional.of(mapToDomain(found.base(), found.collection(), itemIds));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<ExpandedEntity> findExpandedItems(
      UUID tenantId, UUID actorId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        if (loadScopedBase(session, tenantId, actorId, collectionId).isEmpty()) {
          tx.commit();
          return ImmutableList.of();
        }
        var items = CollectionItemHelper.queryExpandedItems(session, tenantId, collectionId);
        tx.commit();
        return items;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean existsByOwnerAndName(
      UUID tenantId, UUID ownerId, String name, Optional<UUID> excludeId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        boolean exists = existsByOwnerAndName(session, tenantId, ownerId, name, excludeId);
        tx.commit();
        return exists;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Collection create(
      UUID tenantId, UUID generator, UUID ownerId, String name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator);
        if (!ownerId.equals(generator)) {
          ActorGeneratorValidator.validateActiveEntity(session, tenantId, ownerId);
        }
        requireUniqueOwnerName(session, tenantId, ownerId, name, Optional.empty());
        var collectionId = UUID.randomUUID();
        var now = Instant.now();
        var baseEntity = createBaseEntity(tenantId, collectionId, generator, summary, now);
        session.persist(baseEntity);

        var collectionEntity = createCollectionEntity(tenantId, collectionId, ownerId, name);
        session.persist(collectionEntity);

        session.flush();
        tx.commit();
        return mapToDomain(baseEntity, collectionEntity, ImmutableList.of());
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Collection> patch(
      UUID tenantId,
      UUID actorId,
      UUID collectionId,
      Optional<String> name,
      Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var scoped = loadScopedCollection(session, tenantId, actorId, collectionId);
        if (scoped.isEmpty()) {
          tx.commit();
          return Optional.empty();
        }
        var found = scoped.orElseThrow();
        if (name.isPresent()) {
          var newName = name.orElseThrow();
          requireUniqueOwnerName(
              session,
              tenantId,
              found.collection().getOwnerId(),
              newName,
              Optional.of(collectionId));
          found.collection().setName(newName);
        }
        summary.ifPresent(found.base()::setSummary);
        found.base().setUpdatedOn(Instant.now());

        session.merge(found.base());
        session.merge(found.collection());
        var itemIds = CollectionItemHelper.queryActiveItemIds(session, tenantId, collectionId);
        tx.commit();
        return Optional.of(mapToDomain(found.base(), found.collection(), itemIds));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID actorId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var baseOpt = loadScopedBase(session, tenantId, actorId, collectionId);
        if (baseOpt.isEmpty()) {
          tx.commit();
          return false;
        }
        var entity = baseOpt.orElseThrow();
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

  @Override
  public boolean addItem(
      UUID tenantId,
      UUID actorId,
      UUID collectionId,
      UUID refersToId,
      Optional<UUID> before,
      Optional<Integer> index) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        if (!validateCollectionAndEntityExist(
            session, tenantId, actorId, collectionId, refersToId)) {
          tx.commit();
          return false;
        }

        if (CollectionItemHelper.isItemAlreadyPresent(
            session, tenantId, collectionId, refersToId)) {
          tx.commit();
          return true;
        }

        int targetOrdinal =
            CollectionItemHelper.resolveTargetOrdinal(
                session, tenantId, collectionId, before, index);
        CollectionItemHelper.shiftOrdinalsUp(session, tenantId, collectionId, targetOrdinal);

        var itemId = UUID.randomUUID();
        var item =
            CollectionItemHelper.createCollectionItemEntity(
                itemId, tenantId, collectionId, targetOrdinal, Instant.now(), refersToId);
        session.persist(item);
        session.flush();
        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean removeItem(UUID tenantId, UUID actorId, UUID collectionId, UUID refersToId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        if (loadScopedBase(session, tenantId, actorId, collectionId).isEmpty()) {
          tx.commit();
          return false;
        }

        var item =
            CollectionItemHelper.findItemByRefersToId(session, tenantId, collectionId, refersToId);
        if (item.isEmpty()) {
          tx.commit();
          return true;
        }

        var collectionItem = item.orElseThrow();
        int removedOrdinal = collectionItem.getOrdinal();
        session.remove(collectionItem);
        CollectionItemHelper.shiftOrdinalsDown(session, tenantId, collectionId, removedOrdinal);
        session.flush();
        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private boolean validateCollectionAndEntityExist(
      Session session, UUID tenantId, UUID actorId, UUID collectionId, UUID refersToId) {
    if (loadScopedBase(session, tenantId, actorId, collectionId).isEmpty()) {
      return false;
    }
    var targetBase = session.find(EntityBaseEntity.class, createEntityId(tenantId, refersToId));
    return targetBase != null && targetBase.getDeletedOn() == null;
  }

  private Optional<EntityBaseEntity> loadScopedBase(
      Session session, UUID tenantId, UUID actorId, UUID collectionId) {
    if (!ActorGeneratorValidator.isActiveActor(session, tenantId, actorId)) {
      return Optional.empty();
    }
    var entity = session.find(EntityBaseEntity.class, createEntityId(tenantId, collectionId));
    if (entity == null
        || entity.getDeletedOn() != null
        || !ENTITY_TYPE_COLLECTION.equals(entity.getEntityType())
        || !actorId.equals(entity.getGenerator())) {
      return Optional.empty();
    }
    return Optional.of(entity);
  }

  private Optional<ScopedCollection> loadScopedCollection(
      Session session, UUID tenantId, UUID actorId, UUID collectionId) {
    var baseOpt = loadScopedBase(session, tenantId, actorId, collectionId);
    if (baseOpt.isEmpty()) {
      return Optional.empty();
    }
    var collection = session.find(CollectionEntity.class, createEntityId(tenantId, collectionId));
    if (collection == null) {
      return Optional.empty();
    }
    return Optional.of(createScopedCollection(baseOpt.orElseThrow(), collection));
  }

  private boolean existsByOwnerAndName(
      Session session, UUID tenantId, UUID ownerId, String name, Optional<UUID> excludeId) {
    var hql =
        excludeId.isPresent()
            ? EXISTS_BY_OWNER_AND_NAME_EXCLUDING_ID_HQL
            : EXISTS_BY_OWNER_AND_NAME_HQL;
    var query =
        session
            .createQuery(hql, Long.class)
            .setParameter("tenantId", tenantId)
            .setParameter("ownerId", ownerId)
            .setParameter("name", name);
    excludeId.ifPresent(id -> query.setParameter("excludeId", id));
    return query.getSingleResult() > 0;
  }

  private void requireUniqueOwnerName(
      Session session, UUID tenantId, UUID ownerId, String name, Optional<UUID> excludeId) {
    if (existsByOwnerAndName(session, tenantId, ownerId, name, excludeId)) {
      throw createDuplicateNameException(name);
    }
  }

  private static IllegalStateException createDuplicateNameException(String name) {
    return new IllegalStateException(
        "Collection with name '" + name + "' already exists for owner");
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static void rollbackQuietly(Transaction tx, Exception e) {
    try {
      tx.rollback();
    } catch (Exception rollbackException) {
      e.addSuppressed(rollbackException);
    }
  }

  private static EntityId createEntityId(UUID tenantId, UUID collectionId) {
    return new EntityId(tenantId, collectionId);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID collectionId, UUID generator, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId,
        collectionId,
        generator,
        ENTITY_TYPE_COLLECTION,
        summary.orElse(null),
        now,
        now,
        null);
  }

  private static CollectionEntity createCollectionEntity(
      UUID tenantId, UUID collectionId, UUID ownerId, String name) {
    return new CollectionEntity(tenantId, collectionId, ownerId, name);
  }

  private Collection mapToDomain(
      EntityBaseEntity entity, CollectionEntity collection, ImmutableList<UUID> itemIds) {
    return new Collection(
        collection.getId(),
        entity.getGenerator(),
        collection.getOwnerId(),
        collection.getName(),
        entity.getSummary(),
        itemIds.size(),
        itemIds,
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }

  private static ScopedCollection createScopedCollection(
      EntityBaseEntity base, CollectionEntity collection) {
    return new ScopedCollection(base, collection);
  }

  private static final class ScopedCollection {

    private final EntityBaseEntity base;
    private final CollectionEntity collection;

    private ScopedCollection(EntityBaseEntity base, CollectionEntity collection) {
      this.base = base;
      this.collection = collection;
    }

    private EntityBaseEntity base() {
      return base;
    }

    private CollectionEntity collection() {
      return collection;
    }
  }
}
