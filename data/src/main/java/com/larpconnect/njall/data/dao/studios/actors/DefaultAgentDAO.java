package com.larpconnect.njall.data.dao.studios.actors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Agent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultAgentDAO implements AgentDAO {

  private static final String ENTITY_TYPE_ACTOR = "Actor";
  private static final String ENTITY_TYPE_COLLECTION = "OrderedCollection";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String CHECK_OWNER_SQL =
      "SELECT 1 FROM njall_users.entities "
          + "WHERE tenant_id = :tenantId AND id = :ownerId AND deleted_on IS NULL";

  private static final String CHECK_USERNAME_SQL =
      "SELECT 1 FROM njall_users.actors "
          + "WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username)";

  private static final String CHECK_USERNAME_EXCLUDE_SQL =
      "SELECT 1 FROM njall_users.actors "
          + "WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username)"
          + " AND id != :excludeId";

  private static final String INSERT_BASE_COLLECTION_SQL =
      "INSERT INTO njall_users.entities (id, tenant_id, entity_type, summary, created_on,"
          + " updated_on) VALUES (:id, :tenantId, :entityType, NULL, :now, :now)";

  private static final String INSERT_SUBTYPE_COLLECTION_SQL =
      "INSERT INTO njall_users.collections (tenant_id, id, owner_id, name) "
          + "VALUES (:tenantId, :id, :actorId, :name)";

  private static final String QUERY_COLLECTION_ID_SQL =
      "SELECT id FROM njall_users.collections "
          + "WHERE tenant_id = :tenantId AND owner_id = :actorId AND name = :name";

  private static final String QUERY_ENDPOINTS_SQL =
      "SELECT name, endpoint FROM njall_users.actor_endpoints "
          + "WHERE tenant_id = :tenantId AND actor_id = :actorId ORDER BY name ASC";

  private static final String CASCADE_DELETE_COLLECTIONS_SQL =
      "UPDATE njall_users.entities SET deleted_on = :now, updated_on = :now "
          + "WHERE tenant_id = :tenantId AND id IN ("
          + "SELECT id FROM njall_users.collections WHERE tenant_id = :tenantId AND owner_id ="
          + " :actorId)";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultAgentDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Agent> findById(UUID tenantId, UUID actorId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        var agentEntity = session.find(AgentEntity.class, entityId);
        if (baseEntity == null || agentEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }

        var endpoints = queryEndpoints(session, tenantId, actorId);
        var inboxId = queryCollectionId(session, tenantId, actorId, "inbox");
        var outboxId = queryCollectionId(session, tenantId, actorId, "outbox");
        tx.commit();

        return Optional.of(
            mapToDomain(
                baseEntity,
                agentEntity,
                endpoints,
                inboxId.orElse(actorId),
                outboxId.orElse(actorId)));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean existsByPreferredUsername(
      UUID tenantId, String preferredUsername, Optional<UUID> excludeActorId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var sql = excludeActorId.isPresent() ? CHECK_USERNAME_EXCLUDE_SQL : CHECK_USERNAME_SQL;
        var query = session.createNativeQuery(sql, Object.class);
        query.setParameter("tenantId", tenantId).setParameter("username", preferredUsername);
        excludeActorId.ifPresent(id -> query.setParameter("excludeId", id));
        boolean exists = !query.getResultList().isEmpty();
        tx.commit();
        return exists;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Agent create(
      UUID tenantId, UUID ownerId, String preferredUsername, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        validateOwnerExists(session, tenantId, ownerId);
        validateUsernameNotTaken(session, tenantId, preferredUsername);

        var actorId = UUID.randomUUID();
        var inboxId = UUID.randomUUID();
        var outboxId = UUID.randomUUID();
        var now = Instant.now();

        var baseEntity = createBaseEntity(tenantId, actorId, summary, now);
        session.persist(baseEntity);

        var agentEntity = createAgentEntity(tenantId, actorId, ownerId, preferredUsername);
        session.persist(agentEntity);
        session.flush();

        provisionCollection(session, tenantId, actorId, inboxId, "inbox", now);
        provisionCollection(session, tenantId, actorId, outboxId, "outbox", now);

        tx.commit();
        return createAgentDomain(
            actorId, ownerId, preferredUsername, summary, inboxId, outboxId, now);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Agent> update(
      UUID tenantId, UUID actorId, Optional<String> preferredUsername, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        var agentEntity = session.find(AgentEntity.class, entityId);
        if (baseEntity == null || agentEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }

        applyUpdates(
            session, tenantId, actorId, baseEntity, agentEntity, preferredUsername, summary);

        var endpoints = queryEndpoints(session, tenantId, actorId);
        var inboxId = queryCollectionId(session, tenantId, actorId, "inbox");
        var outboxId = queryCollectionId(session, tenantId, actorId, "outbox");
        tx.commit();

        return Optional.of(
            mapToDomain(
                baseEntity,
                agentEntity,
                endpoints,
                inboxId.orElse(actorId),
                outboxId.orElse(actorId)));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean delete(UUID tenantId, UUID actorId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        if (baseEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return false;
        }

        var now = Instant.now();
        baseEntity.setDeletedOn(now);
        baseEntity.setUpdatedOn(now);
        session.merge(baseEntity);

        cascadeDeleteCollections(session, tenantId, actorId, now);
        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean addEndpoint(UUID tenantId, UUID actorId, String name, String endpoint) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        if (baseEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return false;
        }

        var endpointId = createEndpointId(tenantId, actorId, name);
        var existing = session.find(AgentEndpointEntity.class, endpointId);
        if (existing != null) {
          existing.setEndpoint(endpoint);
          session.merge(existing);
        } else {
          session.persist(createEndpointEntity(tenantId, actorId, name, endpoint));
        }

        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean removeEndpoint(UUID tenantId, UUID actorId, String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        if (baseEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return false;
        }

        var endpointId = createEndpointId(tenantId, actorId, name);
        var existing = session.find(AgentEndpointEntity.class, endpointId);
        if (existing != null) {
          session.remove(existing);
        }

        tx.commit();
        return true;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<UUID> findInboxId(UUID tenantId, UUID actorId) {
    return findCollectionId(tenantId, actorId, "inbox");
  }

  @Override
  public Optional<UUID> findOutboxId(UUID tenantId, UUID actorId) {
    return findCollectionId(tenantId, actorId, "outbox");
  }

  private Optional<UUID> findCollectionId(UUID tenantId, UUID actorId, String name) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var collectionId = queryCollectionId(session, tenantId, actorId, name);
        tx.commit();
        return collectionId;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private void validateOwnerExists(Session session, UUID tenantId, UUID ownerId) {
    var query = session.createNativeQuery(CHECK_OWNER_SQL, Object.class);
    query.setParameter("tenantId", tenantId).setParameter("ownerId", ownerId);
    if (query.getResultList().isEmpty()) {
      throw new IllegalArgumentException("Owner entity not found or deleted: " + ownerId);
    }
  }

  private void validateUsernameNotTaken(Session session, UUID tenantId, String username) {
    var query = session.createNativeQuery(CHECK_USERNAME_SQL, Object.class);
    query.setParameter("tenantId", tenantId).setParameter("username", username);
    if (!query.getResultList().isEmpty()) {
      throw new IllegalStateException("Preferred username already exists: " + username);
    }
  }

  private void applyUpdates(
      Session session,
      UUID tenantId,
      UUID actorId,
      EntityBaseEntity baseEntity,
      AgentEntity agentEntity,
      Optional<String> preferredUsername,
      Optional<String> summary) {
    if (preferredUsername.isPresent()) {
      var newUsername = preferredUsername.orElseThrow();
      var rows =
          session
              .createNativeQuery(CHECK_USERNAME_EXCLUDE_SQL, Object.class)
              .setParameter("tenantId", tenantId)
              .setParameter("username", newUsername)
              .setParameter("excludeId", actorId)
              .getResultList();
      if (!rows.isEmpty()) {
        throw new IllegalStateException("Preferred username already taken: " + newUsername);
      }
      agentEntity.setPreferredUsername(newUsername);
      session.merge(agentEntity);
    }
    if (summary.isPresent()) {
      baseEntity.setSummary(summary.orElseThrow());
    }
    baseEntity.setUpdatedOn(Instant.now());
    session.merge(baseEntity);
  }

  private void provisionCollection(
      Session session, UUID tenantId, UUID actorId, UUID id, String name, Instant now) {
    session
        .createNativeQuery(INSERT_BASE_COLLECTION_SQL, Object.class)
        .setParameter("id", id)
        .setParameter("tenantId", tenantId)
        .setParameter("entityType", ENTITY_TYPE_COLLECTION)
        .setParameter("now", now)
        .executeUpdate();
    session
        .createNativeQuery(INSERT_SUBTYPE_COLLECTION_SQL, Object.class)
        .setParameter("tenantId", tenantId)
        .setParameter("id", id)
        .setParameter("actorId", actorId)
        .setParameter("name", name)
        .executeUpdate();
  }

  private void cascadeDeleteCollections(Session session, UUID tenantId, UUID actorId, Instant now) {
    session
        .createNativeQuery(CASCADE_DELETE_COLLECTIONS_SQL, Object.class)
        .setParameter("now", now)
        .setParameter("tenantId", tenantId)
        .setParameter("actorId", actorId)
        .executeUpdate();
  }

  private Optional<UUID> queryCollectionId(
      Session session, UUID tenantId, UUID actorId, String name) {
    @SuppressWarnings("unchecked")
    List<UUID> rows =
        session
            .createNativeQuery(QUERY_COLLECTION_ID_SQL, UUID.class)
            .setParameter("tenantId", tenantId)
            .setParameter("actorId", actorId)
            .setParameter("name", name)
            .getResultList();
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
  }

  private ImmutableMap<String, String> queryEndpoints(
      Session session, UUID tenantId, UUID actorId) {
    @SuppressWarnings("unchecked")
    List<Object[]> rows =
        session
            .createNativeQuery(QUERY_ENDPOINTS_SQL, Object[].class)
            .setParameter("tenantId", tenantId)
            .setParameter("actorId", actorId)
            .getResultList();
    var builder = ImmutableMap.<String, String>builder();
    for (var row : rows) {
      builder.put((String) row[0], (String) row[1]);
    }
    return builder.build();
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

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static AgentEndpointId createEndpointId(UUID tenantId, UUID actorId, String name) {
    return new AgentEndpointId(tenantId, actorId, name);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID id, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, id, ENTITY_TYPE_ACTOR, summary.orElse(null), now, now, null);
  }

  private static AgentEntity createAgentEntity(
      UUID tenantId, UUID id, UUID ownerId, String preferredUsername) {
    return new AgentEntity(tenantId, id, ownerId, preferredUsername);
  }

  private static AgentEndpointEntity createEndpointEntity(
      UUID tenantId, UUID actorId, String name, String endpoint) {
    return new AgentEndpointEntity(tenantId, actorId, name, endpoint);
  }

  private static Agent createAgentDomain(
      UUID id,
      UUID ownerId,
      String preferredUsername,
      Optional<String> summary,
      UUID inboxId,
      UUID outboxId,
      Instant now) {
    return new Agent(
        id,
        ownerId,
        preferredUsername,
        summary,
        inboxId,
        outboxId,
        ImmutableMap.of(),
        now,
        now,
        Optional.empty());
  }

  private Agent mapToDomain(
      EntityBaseEntity baseEntity,
      AgentEntity agentEntity,
      ImmutableMap<String, String> endpoints,
      UUID inboxId,
      UUID outboxId) {
    return new Agent(
        agentEntity.getId(),
        agentEntity.getOwnerId(),
        agentEntity.getPreferredUsername(),
        Optional.ofNullable(baseEntity.getSummary()),
        inboxId,
        outboxId,
        endpoints,
        baseEntity.getCreatedOn(),
        baseEntity.getUpdatedOn(),
        Optional.ofNullable(baseEntity.getDeletedOn()));
  }
}
