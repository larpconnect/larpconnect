package com.larpconnect.njall.data.dao.studios.actors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
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
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String CHECK_USERNAME_SQL =
      """
      SELECT 1 FROM njall_users.actors
      WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username)
      """;
  private static final String CHECK_USERNAME_EXCLUDE_SQL =
      """
      SELECT 1 FROM njall_users.actors
      WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username)
        AND id != :excludeId
      """;
  private static final String QUERY_ENDPOINTS_SQL =
      """
      SELECT name, endpoint FROM njall_users.actor_endpoints
      WHERE tenant_id = :tenantId AND actor_id = :actorId ORDER BY name ASC
      """;

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
        var inboxId = AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "inbox");
        var outboxId =
            AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "outbox");
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
      UUID tenantId, Optional<UUID> generator, String preferredUsername, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var actorId = UUID.randomUUID();
        var effectiveGenerator = generator.orElse(actorId);

        if (generator.isPresent()) {
          ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator.get());
        }
        validateUsernameNotTaken(session, tenantId, preferredUsername);

        var inboxId = UUID.randomUUID();
        var outboxId = UUID.randomUUID();
        var now = Instant.now();

        var baseEntity = createBaseEntity(tenantId, actorId, effectiveGenerator, summary, now);
        session.persist(baseEntity);

        var agentEntity = createAgentEntity(tenantId, actorId, preferredUsername);
        session.persist(agentEntity);
        session.flush();

        AgentCollectionsHelper.provisionCollection(
            session, tenantId, actorId, inboxId, "inbox", now);
        AgentCollectionsHelper.provisionCollection(
            session, tenantId, actorId, outboxId, "outbox", now);

        tx.commit();
        return createAgentDomain(
            actorId, effectiveGenerator, preferredUsername, summary, inboxId, outboxId, now);
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
        var inboxId = AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "inbox");
        var outboxId =
            AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "outbox");
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

        AgentCollectionsHelper.cascadeDeleteCollections(session, tenantId, actorId, now);
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
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        if (baseEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        var inboxId = AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "inbox");
        tx.commit();
        return inboxId;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<UUID> findOutboxId(UUID tenantId, UUID actorId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, actorId);
        var baseEntity = session.find(EntityBaseEntity.class, entityId);
        if (baseEntity == null || baseEntity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        var outboxId =
            AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "outbox");
        tx.commit();
        return outboxId;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private void validateUsernameNotTaken(Session session, UUID tenantId, String preferredUsername) {
    if (existsByPreferredUsername(session, tenantId, preferredUsername, Optional.empty())) {
      throw new IllegalArgumentException(
          "Preferred username '" + preferredUsername + "' is already taken in tenant " + tenantId);
    }
  }

  private boolean existsByPreferredUsername(
      Session session, UUID tenantId, String preferredUsername, Optional<UUID> excludeActorId) {
    var sql = excludeActorId.isPresent() ? CHECK_USERNAME_EXCLUDE_SQL : CHECK_USERNAME_SQL;
    var query = session.createNativeQuery(sql, Object.class);
    query.setParameter("tenantId", tenantId).setParameter("username", preferredUsername);
    excludeActorId.ifPresent(id -> query.setParameter("excludeId", id));
    return !query.getResultList().isEmpty();
  }

  private void applyUpdates(
      Session session,
      UUID tenantId,
      UUID actorId,
      EntityBaseEntity baseEntity,
      AgentEntity agentEntity,
      Optional<String> preferredUsername,
      Optional<String> summary) {
    var now = Instant.now();
    boolean updated = false;

    if (preferredUsername.isPresent()) {
      var newUsername = preferredUsername.get();
      if (!newUsername.equalsIgnoreCase(agentEntity.getPreferredUsername())
          && existsByPreferredUsername(session, tenantId, newUsername, Optional.of(actorId))) {
        throw new IllegalArgumentException(
            "Preferred username '" + newUsername + "' is already taken in tenant " + tenantId);
      }
      agentEntity.setPreferredUsername(newUsername);
      session.merge(agentEntity);
      updated = true;
    }

    if (summary.isPresent()) {
      baseEntity.setSummary(summary.get());
      updated = true;
    }

    if (updated) {
      baseEntity.setUpdatedOn(now);
      session.merge(baseEntity);
    }
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
      UUID tenantId, UUID id, UUID generator, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, id, generator, ENTITY_TYPE_ACTOR, summary.orElse(null), now, now, null);
  }

  private static AgentEntity createAgentEntity(UUID tenantId, UUID id, String preferredUsername) {
    return new AgentEntity(tenantId, id, preferredUsername);
  }

  private static AgentEndpointEntity createEndpointEntity(
      UUID tenantId, UUID actorId, String name, String endpoint) {
    return new AgentEndpointEntity(tenantId, actorId, name, endpoint);
  }

  private static Agent createAgentDomain(
      UUID id,
      UUID generator,
      String preferredUsername,
      Optional<String> summary,
      UUID inboxId,
      UUID outboxId,
      Instant now) {
    return new Agent(
        id,
        generator,
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
        baseEntity.getGenerator(),
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
