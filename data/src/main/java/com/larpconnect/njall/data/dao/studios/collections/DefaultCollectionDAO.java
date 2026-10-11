package com.larpconnect.njall.data.dao.studios.collections;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Collection;
import com.larpconnect.njall.data.domain.ExpandedEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultCollectionDAO implements CollectionDAO {

  private static final String ENTITY_TYPE_COLLECTION = "OrderedCollection";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String EXPANDED_ITEMS_QUERY =
      """
      SELECT e.id, e.entity_type, e.summary,
             COALESCE(i.name, ev.title, loc.name, l.url) AS display_name
      FROM njall_users.collection_items ci
      JOIN njall_users.entities e
        ON ci.refers_to_tenant_id = e.tenant_id AND ci.refers_to_id = e.id
      LEFT JOIN njall_users.individuals i
        ON e.tenant_id = i.tenant_id AND e.id = i.id
      LEFT JOIN njall_users.events ev
        ON e.tenant_id = ev.tenant_id AND e.id = ev.id
      LEFT JOIN njall_users.locations loc
        ON e.tenant_id = loc.tenant_id AND e.id = loc.id
      LEFT JOIN njall_users.links l
        ON e.tenant_id = l.tenant_id AND e.id = l.id
      WHERE ci.tenant_id = :tenantId
        AND ci.collection_id = :collectionId
        AND e.deleted_on IS NULL
      ORDER BY ci.ordinal ASC
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCollectionDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Collection> findById(UUID tenantId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, collectionId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var collection = session.find(CollectionEntity.class, entityId);
        if (entity == null || collection == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }

        var itemIds = queryActiveItemIds(session, tenantId, collectionId);
        tx.commit();
        return Optional.of(mapToDomain(entity, collection, itemIds));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<ExpandedEntity> findExpandedItems(UUID tenantId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows =
            session
                .createNativeQuery(EXPANDED_ITEMS_QUERY, Object[].class)
                .setParameter("tenantId", tenantId)
                .setParameter("collectionId", collectionId)
                .getResultList();
        tx.commit();
        return mapExpandedRows(rows);
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
        var hql =
            "SELECT COUNT(c) FROM CollectionEntity c, EntityBaseEntity e "
                + "WHERE c.tenantId = :tenantId AND c.ownerId = :ownerId AND c.name = :name "
                + "AND c.tenantId = e.tenantId AND c.id = e.id AND e.deletedOn IS NULL"
                + (excludeId.isPresent() ? " AND c.id != :excludeId" : "");
        var query =
            session
                .createQuery(hql, Long.class)
                .setParameter("tenantId", tenantId)
                .setParameter("ownerId", ownerId)
                .setParameter("name", name);
        excludeId.ifPresent(id -> query.setParameter("excludeId", id));
        long count = query.getSingleResult();
        tx.commit();
        return count > 0;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Collection create(UUID tenantId, UUID ownerId, String name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var collectionId = UUID.randomUUID();
        var now = Instant.now();
        var baseEntity = createBaseEntity(tenantId, collectionId, summary, now);
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
      UUID tenantId, UUID collectionId, Optional<String> name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, collectionId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var collection = session.find(CollectionEntity.class, entityId);
        if (entity == null || collection == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }

        name.ifPresent(collection::setName);
        summary.ifPresent(entity::setSummary);
        entity.setUpdatedOn(Instant.now());

        session.merge(entity);
        session.merge(collection);
        var itemIds = queryActiveItemIds(session, tenantId, collectionId);
        tx.commit();
        return Optional.of(mapToDomain(entity, collection, itemIds));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID collectionId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, collectionId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (entity == null || entity.getDeletedOn() != null) {
          tx.commit();
          return false;
        }

        entity.setDeletedOn(Instant.now());
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
      UUID collectionId,
      UUID refersToId,
      Optional<UUID> before,
      Optional<Integer> index) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        if (!validateCollectionAndEntityExist(session, tenantId, collectionId, refersToId)) {
          tx.commit();
          return false;
        }

        if (isItemAlreadyPresent(session, tenantId, collectionId, refersToId)) {
          tx.commit();
          return true;
        }

        int targetOrdinal = resolveTargetOrdinal(session, tenantId, collectionId, before, index);
        shiftOrdinalsUp(session, tenantId, collectionId, targetOrdinal);

        var itemId = UUID.randomUUID();
        var item =
            createCollectionItemEntity(
                itemId, tenantId, collectionId, targetOrdinal, Instant.now(), tenantId, refersToId);
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
  public boolean removeItem(UUID tenantId, UUID collectionId, UUID refersToId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var collectionEntityId = createEntityId(tenantId, collectionId);
        var collectionBase = session.find(EntityBaseEntity.class, collectionEntityId);
        if (collectionBase == null || collectionBase.getDeletedOn() != null) {
          tx.commit();
          return false;
        }

        var item = findItemByRefersToId(session, tenantId, collectionId, refersToId);
        if (item.isEmpty()) {
          tx.commit();
          return true;
        }

        int removedOrdinal = item.get().getOrdinal();
        session.remove(item.get());
        shiftOrdinalsDown(session, tenantId, collectionId, removedOrdinal);
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
      Session session, UUID tenantId, UUID collectionId, UUID refersToId) {
    var collectionBase =
        session.find(EntityBaseEntity.class, createEntityId(tenantId, collectionId));
    if (collectionBase == null || collectionBase.getDeletedOn() != null) {
      return false;
    }
    var targetBase = session.find(EntityBaseEntity.class, createEntityId(tenantId, refersToId));
    return targetBase != null && targetBase.getDeletedOn() == null;
  }

  private boolean isItemAlreadyPresent(
      Session session, UUID tenantId, UUID collectionId, UUID refersToId) {
    var hql =
        "SELECT COUNT(ci) FROM CollectionItemEntity ci "
            + "WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId "
            + "AND ci.refersToId = :refersToId";
    long count =
        session
            .createQuery(hql, Long.class)
            .setParameter("tenantId", tenantId)
            .setParameter("collectionId", collectionId)
            .setParameter("refersToId", refersToId)
            .getSingleResult();
    return count > 0;
  }

  private int resolveTargetOrdinal(
      Session session,
      UUID tenantId,
      UUID collectionId,
      Optional<UUID> before,
      Optional<Integer> index) {
    if (before.isPresent()) {
      var beforeItem = findItemByRefersToId(session, tenantId, collectionId, before.get());
      return beforeItem.map(CollectionItemEntity::getOrdinal).orElse(0);
    }
    return index.map(i -> Math.max(0, i)).orElse(0);
  }

  private Optional<CollectionItemEntity> findItemByRefersToId(
      Session session, UUID tenantId, UUID collectionId, UUID refersToId) {
    var hql =
        "FROM CollectionItemEntity ci "
            + "WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId "
            + "AND ci.refersToId = :refersToId";
    return session
        .createQuery(hql, CollectionItemEntity.class)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("refersToId", refersToId)
        .uniqueResultOptional();
  }

  private void shiftOrdinalsUp(
      Session session, UUID tenantId, UUID collectionId, int targetOrdinal) {
    var updateHql =
        "UPDATE CollectionItemEntity ci SET ci.ordinal = ci.ordinal + 1 "
            + "WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId "
            + "AND ci.ordinal >= :targetOrdinal";
    session
        .createMutationQuery(updateHql)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("targetOrdinal", targetOrdinal)
        .executeUpdate();
  }

  private void shiftOrdinalsDown(
      Session session, UUID tenantId, UUID collectionId, int removedOrdinal) {
    var updateHql =
        "UPDATE CollectionItemEntity ci SET ci.ordinal = ci.ordinal - 1 "
            + "WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId "
            + "AND ci.ordinal > :removedOrdinal";
    session
        .createMutationQuery(updateHql)
        .setParameter("tenantId", tenantId)
        .setParameter("collectionId", collectionId)
        .setParameter("removedOrdinal", removedOrdinal)
        .executeUpdate();
  }

  private ImmutableList<UUID> queryActiveItemIds(
      Session session, UUID tenantId, UUID collectionId) {
    var hql =
        "SELECT ci.refersToId FROM CollectionItemEntity ci, EntityBaseEntity e "
            + "WHERE ci.tenantId = :tenantId AND ci.collectionId = :collectionId "
            + "AND ci.refersToTenantId = e.tenantId AND ci.refersToId = e.id "
            + "AND e.deletedOn IS NULL "
            + "ORDER BY ci.ordinal ASC";
    List<UUID> results =
        session
            .createQuery(hql, UUID.class)
            .setParameter("tenantId", tenantId)
            .setParameter("collectionId", collectionId)
            .getResultList();
    return ImmutableList.copyOf(results);
  }

  private ImmutableList<ExpandedEntity> mapExpandedRows(List<Object[]> rows) {
    return rows.stream()
        .map(
            row ->
                new ExpandedEntity(
                    (UUID) row[0], (String) row[1], (String) row[3], (String) row[2]))
        .collect(ImmutableList.toImmutableList());
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
      UUID tenantId, UUID collectionId, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, collectionId, ENTITY_TYPE_COLLECTION, summary.orElse(null), now, now, null);
  }

  private static CollectionEntity createCollectionEntity(
      UUID tenantId, UUID collectionId, UUID ownerId, String name) {
    return new CollectionEntity(tenantId, collectionId, ownerId, name);
  }

  private static CollectionItemEntity createCollectionItemEntity(
      UUID id,
      UUID tenantId,
      UUID collectionId,
      int ordinal,
      Instant addedOn,
      UUID refersToTenantId,
      UUID refersToId) {
    return new CollectionItemEntity(
        id, tenantId, collectionId, ordinal, addedOn, refersToTenantId, refersToId);
  }

  private Collection mapToDomain(
      EntityBaseEntity entity, CollectionEntity collection, ImmutableList<UUID> itemIds) {
    return new Collection(
        collection.getId(),
        collection.getOwnerId(),
        collection.getName(),
        entity.getSummary(),
        itemIds.size(),
        itemIds,
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
