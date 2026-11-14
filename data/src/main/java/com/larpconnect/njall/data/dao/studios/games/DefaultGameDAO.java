package com.larpconnect.njall.data.dao.studios.games;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.dao.studios.events.EventEntity;
import com.larpconnect.njall.data.domain.studios.EventType;
import com.larpconnect.njall.data.domain.studios.Game;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jspecify.annotations.Nullable;

final class DefaultGameDAO implements GameDAO {

  private static final String ENTITY_TYPE_GAME = "Game";
  private static final String ENTITY_TYPE_SYSTEM = "System";
  private static final String ENTITY_TYPE_CAMPAIGN = "Campaign";
  private static final String ENTITY_TYPE_LOCATION = "Location";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";
  private static final String LIST_GAMES_HQL =
      """
      SELECT e, ev, g, cmp.id
      FROM EntityBaseEntity e
      JOIN EventEntity ev ON ev.tenantId = e.tenantId AND ev.id = e.id
      JOIN GameEntity g ON g.tenantId = e.tenantId AND g.id = e.id
      LEFT JOIN EntityBaseEntity cmp
        ON cmp.tenantId = g.tenantId
        AND cmp.id = g.campaignId
        AND cmp.entityType = :campaignEntityType
        AND cmp.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
      ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC
      """;
  private static final String LIST_GAMES_BY_SYSTEM_HQL =
      """
      SELECT e, ev, g, cmp.id
      FROM EntityBaseEntity e
      JOIN EventEntity ev ON ev.tenantId = e.tenantId AND ev.id = e.id
      JOIN GameEntity g ON g.tenantId = e.tenantId AND g.id = e.id
      LEFT JOIN EntityBaseEntity cmp
        ON cmp.tenantId = g.tenantId
        AND cmp.id = g.campaignId
        AND cmp.entityType = :campaignEntityType
        AND cmp.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
        AND g.systemId = :systemId
      ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC
      """;
  private static final String LIST_GAMES_BY_CAMPAIGN_HQL =
      """
      SELECT e, ev, g, cmp.id
      FROM EntityBaseEntity e
      JOIN EventEntity ev ON ev.tenantId = e.tenantId AND ev.id = e.id
      JOIN GameEntity g ON g.tenantId = e.tenantId AND g.id = e.id
      LEFT JOIN EntityBaseEntity cmp
        ON cmp.tenantId = g.tenantId
        AND cmp.id = g.campaignId
        AND cmp.entityType = :campaignEntityType
        AND cmp.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
        AND cmp.id = :campaignId
      ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC
      """;
  private static final String LIST_GAMES_BY_SYSTEM_AND_CAMPAIGN_HQL =
      """
      SELECT e, ev, g, cmp.id
      FROM EntityBaseEntity e
      JOIN EventEntity ev ON ev.tenantId = e.tenantId AND ev.id = e.id
      JOIN GameEntity g ON g.tenantId = e.tenantId AND g.id = e.id
      LEFT JOIN EntityBaseEntity cmp
        ON cmp.tenantId = g.tenantId
        AND cmp.id = g.campaignId
        AND cmp.entityType = :campaignEntityType
        AND cmp.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
        AND g.systemId = :systemId
        AND cmp.id = :campaignId
      ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultGameDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Game> findById(UUID tenantId, UUID gameId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, gameId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        var game = session.find(GameEntity.class, entityId);
        if (!isActiveGame(entity, event, game)) {
          tx.commit();
          return Optional.empty();
        }
        var visibleCampaignId = resolveVisibleCampaignId(session, tenantId, game.getCampaignId());
        tx.commit();
        return Optional.of(toGame(entity, event, game, visibleCampaignId));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Game> listAll(
      UUID tenantId, Optional<UUID> systemId, Optional<UUID> campaignId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows = queryGameRows(session, tenantId, systemId, campaignId);
        tx.commit();
        return mapRowsToGames(rows);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Game create(UUID tenantId, CreateGameParams params) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        validateCreateInputs(session, tenantId, params);
        var gameId = UUID.randomUUID();
        var now = Instant.now();
        var entity = createBaseEntity(tenantId, gameId, params, now);
        session.persist(entity);
        var event = createEventEntity(tenantId, gameId, params);
        session.persist(event);
        var game = createGameEntity(tenantId, gameId, params);
        session.persist(game);
        tx.commit();
        return toGame(entity, event, game, game.getCampaignId());
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Game> patch(UUID tenantId, UUID gameId, PatchGameParams params) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, gameId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        var game = session.find(GameEntity.class, entityId);
        if (!isActiveGame(entity, event, game)) {
          tx.commit();
          return Optional.empty();
        }
        validatePatchInputs(session, tenantId, event, params);
        applyGamePatch(entity, event, game, params);
        session.merge(entity);
        session.merge(event);
        session.merge(game);
        var visibleCampaignId = resolvePatchVisibleCampaignId(session, tenantId, game, params);
        tx.commit();
        return Optional.of(toGame(entity, event, game, visibleCampaignId));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID gameId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entity = session.find(EntityBaseEntity.class, createEntityId(tenantId, gameId));
        if (entity == null
            || entity.getDeletedOn() != null
            || !ENTITY_TYPE_GAME.equals(entity.getEntityType())) {
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

  private static List<Object[]> queryGameRows(
      Session session, UUID tenantId, Optional<UUID> systemId, Optional<UUID> campaignId) {
    var hql = selectListGamesHql(systemId.isPresent(), campaignId.isPresent());
    var query =
        session
            .createQuery(hql, Object[].class)
            .setParameter("tenantId", tenantId)
            .setParameter("entityType", ENTITY_TYPE_GAME)
            .setParameter("campaignEntityType", ENTITY_TYPE_CAMPAIGN);
    systemId.ifPresent(id -> query.setParameter("systemId", id));
    campaignId.ifPresent(id -> query.setParameter("campaignId", id));
    return query.getResultList();
  }

  private static String selectListGamesHql(boolean hasSystemId, boolean hasCampaignId) {
    if (hasSystemId && hasCampaignId) {
      return LIST_GAMES_BY_SYSTEM_AND_CAMPAIGN_HQL;
    }
    if (hasSystemId) {
      return LIST_GAMES_BY_SYSTEM_HQL;
    }
    if (hasCampaignId) {
      return LIST_GAMES_BY_CAMPAIGN_HQL;
    }
    return LIST_GAMES_HQL;
  }

  private static ImmutableList<Game> mapRowsToGames(List<Object[]> rows) {
    return rows.stream().map(DefaultGameDAO::mapRowToGame).collect(ImmutableList.toImmutableList());
  }

  private static void validateCreateInputs(
      Session session, UUID tenantId, CreateGameParams params) {
    validateTimeOrder(params.startTime().orElse(null), params.endTime().orElse(null));
    ActorGeneratorValidator.validateActorGenerator(session, tenantId, params.generator());
    validateActiveReference(session, tenantId, params.systemId(), ENTITY_TYPE_SYSTEM);
    params
        .campaignId()
        .ifPresent(id -> validateActiveReference(session, tenantId, id, ENTITY_TYPE_CAMPAIGN));
    params
        .locationId()
        .ifPresent(id -> validateActiveReference(session, tenantId, id, ENTITY_TYPE_LOCATION));
  }

  private static void validatePatchInputs(
      Session session, UUID tenantId, EventEntity event, PatchGameParams params) {
    var nextStart =
        resolvePatchedValue(event.getStartTime(), params.startTime(), params.clearStartTime());
    var nextEnd = resolvePatchedValue(event.getEndTime(), params.endTime(), params.clearEndTime());
    validateTimeOrder(nextStart, nextEnd);
    params
        .systemId()
        .ifPresent(id -> validateActiveReference(session, tenantId, id, ENTITY_TYPE_SYSTEM));
    if (!params.clearCampaign() && params.campaignId().isPresent()) {
      validateActiveReference(
          session, tenantId, params.campaignId().orElseThrow(), ENTITY_TYPE_CAMPAIGN);
    }
    if (!params.clearLocationId() && params.locationId().isPresent()) {
      validateActiveReference(
          session, tenantId, params.locationId().orElseThrow(), ENTITY_TYPE_LOCATION);
    }
  }

  private static void validateTimeOrder(@Nullable Instant startTime, @Nullable Instant endTime) {
    if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
      throw createIllegalArgumentException("endTime must be on or after startTime");
    }
  }

  private static void validateActiveReference(
      Session session, UUID tenantId, UUID entityId, String expectedType) {
    var referenced = session.find(EntityBaseEntity.class, createEntityId(tenantId, entityId));
    if (referenced == null
        || referenced.getDeletedOn() != null
        || !expectedType.equals(referenced.getEntityType())) {
      throw createIllegalArgumentException(
          expectedType
              + " entity does not exist or is not active in tenant "
              + tenantId
              + ": "
              + entityId);
    }
  }

  private static boolean isActiveGame(
      @Nullable EntityBaseEntity entity, @Nullable EventEntity event, @Nullable GameEntity game) {
    return entity != null
        && event != null
        && game != null
        && entity.getDeletedOn() == null
        && ENTITY_TYPE_GAME.equals(entity.getEntityType());
  }

  private static @Nullable UUID resolvePatchVisibleCampaignId(
      Session session, UUID tenantId, GameEntity game, PatchGameParams params) {
    if (!params.clearCampaign() && params.campaignId().isPresent()) {
      return game.getCampaignId();
    }
    return resolveVisibleCampaignId(session, tenantId, game.getCampaignId());
  }

  private static @Nullable UUID resolveVisibleCampaignId(
      Session session, UUID tenantId, @Nullable UUID campaignId) {
    if (campaignId == null) {
      return null;
    }
    var campaignEntity = session.find(EntityBaseEntity.class, createEntityId(tenantId, campaignId));
    if (campaignEntity == null
        || campaignEntity.getDeletedOn() != null
        || !ENTITY_TYPE_CAMPAIGN.equals(campaignEntity.getEntityType())) {
      return null;
    }
    return campaignId;
  }

  private static void applyGamePatch(
      EntityBaseEntity entity, EventEntity event, GameEntity game, PatchGameParams params) {
    params.systemId().ifPresent(game::setSystemId);
    game.setCampaignId(
        resolvePatchedValue(game.getCampaignId(), params.campaignId(), params.clearCampaign()));
    params.title().ifPresent(event::setTitle);
    entity.setSummary(
        resolvePatchedValue(entity.getSummary(), params.summary(), params.clearSummary()));
    event.setLocationId(
        resolvePatchedValue(event.getLocationId(), params.locationId(), params.clearLocationId()));
    event.setStartTime(
        resolvePatchedValue(event.getStartTime(), params.startTime(), params.clearStartTime()));
    event.setEndTime(
        resolvePatchedValue(event.getEndTime(), params.endTime(), params.clearEndTime()));
    entity.setUpdatedOn(Instant.now());
  }

  private static <T> @Nullable T resolvePatchedValue(
      @Nullable T current, Optional<T> update, boolean clear) {
    if (clear) {
      return null;
    }
    if (update.isPresent()) {
      return update.orElseThrow();
    }
    return current;
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

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID id, CreateGameParams params, Instant now) {
    return new EntityBaseEntity(
        tenantId,
        id,
        params.generator(),
        ENTITY_TYPE_GAME,
        params.summary().orElse(null),
        now,
        now,
        null);
  }

  private static EventEntity createEventEntity(UUID tenantId, UUID id, CreateGameParams params) {
    return new EventEntity(
        tenantId,
        id,
        EventType.GAME,
        params.locationId().orElse(null),
        params.title(),
        params.startTime().orElse(null),
        params.endTime().orElse(null));
  }

  private static GameEntity createGameEntity(UUID tenantId, UUID id, CreateGameParams params) {
    return new GameEntity(tenantId, id, params.systemId(), params.campaignId().orElse(null));
  }

  private static Game mapRowToGame(Object[] row) {
    return toGame(
        (EntityBaseEntity) row[0], (EventEntity) row[1], (GameEntity) row[2], (UUID) row[3]);
  }

  private static Game toGame(
      EntityBaseEntity entity,
      EventEntity event,
      GameEntity game,
      @Nullable UUID visibleCampaignId) {
    return new Game(
        entity.getId(),
        entity.getGenerator(),
        game.getSystemId(),
        visibleCampaignId,
        event.getLocationId(),
        event.getTitle(),
        entity.getSummary(),
        event.getStartTime(),
        event.getEndTime(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
