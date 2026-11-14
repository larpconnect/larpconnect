package com.larpconnect.njall.data.dao.studios.actors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.Agent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultAgentDAO implements AgentDAO {

  private static final String ENTITY_TYPE_ACTOR = "Actor";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String CHECK_USERNAME_SQL =
      "SELECT 1 FROM njall_users.actors "
          + "WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username)";
  private static final String CHECK_USERNAME_EXCLUDE_SQL =
      "SELECT 1 FROM njall_users.actors "
          + "WHERE tenant_id = :tenantId AND lower(preferred_username) = lower(:username) "
          + "AND id != :excludeId";
  private static final String QUERY_ENDPOINTS_SQL =
      "SELECT name, endpoint FROM njall_users.actor_endpoints "
          + "WHERE tenant_id = :tenantId AND actor_id = :actorId ORDER BY name ASC";
  private static final String FIND_ID_BY_USERNAME_SQL =
      "SELECT a.id FROM njall_users.actors a "
          + "JOIN njall_users.entities e ON e.tenant_id = a.tenant_id AND e.id = a.id "
          + "WHERE a.tenant_id = :tenantId AND lower(a.preferred_username) = lower(:username) "
          + "AND e.deleted_on IS NULL";

  private final Provider<SessionFactory> sessionFactoryProvider;
  private final Clock clock;

  @Inject
  DefaultAgentDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider, Clock clock) {
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.clock = clock;
  }

  DefaultAgentDAO(Provider<SessionFactory> sessionFactoryProvider) {
    this(sessionFactoryProvider, Clock.systemUTC());
  }

  @Override
  public Optional<Agent> findById(UUID tenantId, UUID actorId) {
    return inTenantTransaction(tenantId, session -> loadAgent(session, tenantId, actorId));
  }

  @Override
  public Optional<Agent> findByPreferredUsername(UUID tenantId, String preferredUsername) {
    return inTenantTransaction(
        tenantId,
        session -> findByPreferredUsernameInSession(session, tenantId, preferredUsername));
  }

  private Optional<Agent> findByPreferredUsernameInSession(
      Session session, UUID tenantId, String preferredUsername) {
    var query = session.createNativeQuery(FIND_ID_BY_USERNAME_SQL, UUID.class);
    query.setParameter("tenantId", tenantId).setParameter("username", preferredUsername);
    var results = query.getResultList();
    return results.isEmpty() ? Optional.empty() : loadAgent(session, tenantId, results.getFirst());
  }

  private Optional<Agent> loadAgent(Session session, UUID tenantId, UUID actorId) {
    var entityId = createEntityId(tenantId, actorId);
    var baseEntity = session.find(EntityBaseEntity.class, entityId);
    var agentEntity = session.find(AgentEntity.class, entityId);
    if (baseEntity == null || agentEntity == null || baseEntity.getDeletedOn() != null) {
      return Optional.empty();
    }

    var endpoints = queryEndpoints(session, tenantId, actorId);
    var inboxId = AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "inbox");
    var outboxId = AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, "outbox");

    return Optional.of(
        mapToDomain(
            baseEntity, agentEntity, endpoints, inboxId.orElse(actorId), outboxId.orElse(actorId)));
  }

  @Override
  public Optional<Agent> findByIdentifier(UUID tenantId, String identifier) {
    var parsedUuid = tryParseUuid(identifier);
    return inTenantTransaction(
        tenantId,
        session ->
            parsedUuid
                .flatMap(uuid -> loadAgent(session, tenantId, uuid))
                .or(() -> findByPreferredUsernameInSession(session, tenantId, identifier)));
  }

  private static Optional<UUID> tryParseUuid(String identifier) {
    try {
      return Optional.of(UUID.fromString(identifier));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  @Override
  public boolean existsByPreferredUsername(
      UUID tenantId, String preferredUsername, Optional<UUID> excludeActorId) {
    return inTenantTransaction(
        tenantId,
        session -> existsByPreferredUsername(session, tenantId, preferredUsername, excludeActorId));
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

        generator.ifPresent(
            gen -> ActorGeneratorValidator.validateActorGenerator(session, tenantId, gen));
        validateUsernameNotTaken(session, tenantId, preferredUsername);

        var inboxId = UUID.randomUUID();
        var outboxId = UUID.randomUUID();
        var now = clock.instant();

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

        var now = clock.instant();
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
    return findCollectionId(tenantId, actorId, "inbox");
  }

  @Override
  public Optional<UUID> findOutboxId(UUID tenantId, UUID actorId) {
    return findCollectionId(tenantId, actorId, "outbox");
  }

  private Optional<UUID> findCollectionId(UUID tenantId, UUID actorId, String collectionType) {
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
        var collectionId =
            AgentCollectionsHelper.queryCollectionId(session, tenantId, actorId, collectionType);
        tx.commit();
        return collectionId;
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  private void validateUsernameNotTaken(Session session, UUID tenantId, String preferredUsername) {
    if (existsByPreferredUsername(session, tenantId, preferredUsername, Optional.empty())) {
      throw createUsernameTakenException(preferredUsername, tenantId);
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
    boolean updatedUsername =
        preferredUsername
            .map(
                newUsername -> {
                  if (!newUsername.equalsIgnoreCase(agentEntity.getPreferredUsername())
                      && existsByPreferredUsername(
                          session, tenantId, newUsername, Optional.of(actorId))) {
                    throw createUsernameTakenException(newUsername, tenantId);
                  }
                  agentEntity.setPreferredUsername(newUsername);
                  session.merge(agentEntity);
                  return true;
                })
            .orElse(false);

    boolean updatedSummary =
        summary
            .map(
                newSummary -> {
                  baseEntity.setSummary(newSummary);
                  return true;
                })
            .orElse(false);

    if (updatedUsername || updatedSummary) {
      baseEntity.setUpdatedOn(clock.instant());
      session.merge(baseEntity);
    }
  }

  private static IllegalArgumentException createUsernameTakenException(
      String username, UUID tenantId) {
    return new IllegalArgumentException(
        "Preferred username '" + username + "' is already taken in tenant " + tenantId);
  }

  private ImmutableMap<String, String> queryEndpoints(
      Session session, UUID tenantId, UUID actorId) {
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

  private <T> T inTenantTransaction(UUID tenantId, Function<Session, T> work) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var result = work.apply(session);
        tx.commit();
        return result;
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
