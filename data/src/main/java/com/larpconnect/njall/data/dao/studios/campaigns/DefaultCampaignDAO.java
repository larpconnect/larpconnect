package com.larpconnect.njall.data.dao.studios.campaigns;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.Campaign;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jspecify.annotations.Nullable;

final class DefaultCampaignDAO implements CampaignDAO {

  private static final String ENTITY_TYPE_CAMPAIGN = "Campaign";
  private static final String ENTITY_TYPE_SYSTEM = "System";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private static final String LIST_CAMPAIGNS_HQL =
      """
      SELECT e, c, sysBase.id
      FROM EntityBaseEntity e
      JOIN CampaignEntity c
        ON e.tenantId = c.tenantId AND e.id = c.id
      LEFT JOIN EntityBaseEntity sysBase
        ON sysBase.tenantId = c.tenantId
        AND sysBase.id = c.systemId
        AND sysBase.entityType = :systemEntityType
        AND sysBase.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
      ORDER BY c.title ASC, e.createdOn DESC
      """;

  private static final String LIST_CAMPAIGNS_BY_SYSTEM_HQL =
      """
      SELECT e, c, sysBase.id
      FROM EntityBaseEntity e
      JOIN CampaignEntity c
        ON e.tenantId = c.tenantId AND e.id = c.id
      JOIN EntityBaseEntity sysBase
        ON sysBase.tenantId = c.tenantId
        AND sysBase.id = c.systemId
        AND sysBase.entityType = :systemEntityType
        AND sysBase.deletedOn IS NULL
      WHERE e.tenantId = :tenantId
        AND e.entityType = :entityType
        AND e.deletedOn IS NULL
        AND c.systemId = :systemId
      ORDER BY c.title ASC, e.createdOn DESC
      """;

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultCampaignDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Campaign> findById(UUID tenantId, UUID campaignId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, campaignId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var campaign = session.find(CampaignEntity.class, entityId);
        if (!isActiveCampaign(entity, campaign)) {
          tx.commit();
          return Optional.empty();
        }
        var effectiveSystemId = resolveActiveSystemId(session, tenantId, campaign.getSystemId());
        tx.commit();
        return Optional.of(toCampaign(entity, campaign, effectiveSystemId));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Campaign> listAll(UUID tenantId, Optional<UUID> systemId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows = queryCampaignRows(session, tenantId, systemId);
        tx.commit();
        return mapRowsToCampaigns(rows);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Campaign create(
      UUID tenantId,
      UUID generator,
      String title,
      Optional<String> summary,
      Optional<UUID> systemId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator);
        if (systemId.isPresent()) {
          validateActiveSystem(session, tenantId, systemId.orElseThrow());
        }
        var campaignId = UUID.randomUUID();
        var now = Instant.now();
        var entity = createBaseEntity(tenantId, campaignId, generator, summary, now);
        session.persist(entity);

        var campaign = createCampaignEntity(tenantId, campaignId, systemId, title);
        session.persist(campaign);

        session.flush();
        tx.commit();
        return toCampaign(entity, campaign, systemId.orElse(null));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Campaign> patch(UUID tenantId, UUID campaignId, PatchCampaignParams params) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, campaignId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var campaign = session.find(CampaignEntity.class, entityId);
        if (!isActiveCampaign(entity, campaign)) {
          tx.commit();
          return Optional.empty();
        }
        if (!params.clearSystem() && params.systemId().isPresent()) {
          validateActiveSystem(session, tenantId, params.systemId().orElseThrow());
        }
        applyCampaignPatch(entity, campaign, params);
        var effectiveSystemId = resolvePatchedSystemId(session, tenantId, campaign, params);
        session.merge(entity);
        session.merge(campaign);
        tx.commit();
        return Optional.of(toCampaign(entity, campaign, effectiveSystemId));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID campaignId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, campaignId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (!isActiveCampaignBase(entity)) {
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

  private static List<Object[]> queryCampaignRows(
      Session session, UUID tenantId, Optional<UUID> systemId) {
    var hql = systemId.isPresent() ? LIST_CAMPAIGNS_BY_SYSTEM_HQL : LIST_CAMPAIGNS_HQL;
    var query =
        session
            .createQuery(hql, Object[].class)
            .setParameter("tenantId", tenantId)
            .setParameter("entityType", ENTITY_TYPE_CAMPAIGN)
            .setParameter("systemEntityType", ENTITY_TYPE_SYSTEM);
    systemId.ifPresent(id -> query.setParameter("systemId", id));
    return query.getResultList();
  }

  private static ImmutableList<Campaign> mapRowsToCampaigns(List<Object[]> rows) {
    return rows.stream()
        .map(DefaultCampaignDAO::mapRowToCampaign)
        .collect(ImmutableList.toImmutableList());
  }

  private static Campaign mapRowToCampaign(Object[] row) {
    return toCampaign((EntityBaseEntity) row[0], (CampaignEntity) row[1], (UUID) row[2]);
  }

  private static boolean isActiveCampaignBase(@Nullable EntityBaseEntity entity) {
    return entity != null
        && entity.getDeletedOn() == null
        && ENTITY_TYPE_CAMPAIGN.equals(entity.getEntityType());
  }

  private static boolean isActiveCampaign(
      @Nullable EntityBaseEntity entity, @Nullable CampaignEntity campaign) {
    return isActiveCampaignBase(entity) && campaign != null;
  }

  private static boolean isActiveSystem(@Nullable EntityBaseEntity systemEntity) {
    return systemEntity != null
        && systemEntity.getDeletedOn() == null
        && ENTITY_TYPE_SYSTEM.equals(systemEntity.getEntityType());
  }

  private static void validateActiveSystem(Session session, UUID tenantId, UUID systemId) {
    var systemEntity = session.find(EntityBaseEntity.class, createEntityId(tenantId, systemId));
    if (!isActiveSystem(systemEntity)) {
      throw createIllegalArgumentException(
          "System entity does not exist or is not active in tenant " + tenantId + ": " + systemId);
    }
  }

  private static @Nullable UUID resolveActiveSystemId(
      Session session, UUID tenantId, @Nullable UUID systemId) {
    if (systemId == null) {
      return null;
    }
    var systemBase = session.find(EntityBaseEntity.class, createEntityId(tenantId, systemId));
    if (isActiveSystem(systemBase)) {
      return systemId;
    }
    return null;
  }

  private static @Nullable UUID resolvePatchedSystemId(
      Session session, UUID tenantId, CampaignEntity campaign, PatchCampaignParams params) {
    if (params.clearSystem()) {
      return null;
    }
    if (params.systemId().isPresent()) {
      return params.systemId().orElseThrow();
    }
    return resolveActiveSystemId(session, tenantId, campaign.getSystemId());
  }

  private static void applyCampaignPatch(
      EntityBaseEntity entity, CampaignEntity campaign, PatchCampaignParams params) {
    params.title().ifPresent(campaign::setTitle);
    applyNullableUpdate(params.summary(), params.clearSummary(), entity::setSummary);
    applyNullableUpdate(params.systemId(), params.clearSystem(), campaign::setSystemId);
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

  private static EntityId createEntityId(UUID tenantId, UUID id) {
    return new EntityId(tenantId, id);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID id, UUID generator, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, id, generator, ENTITY_TYPE_CAMPAIGN, summary.orElse(null), now, now, null);
  }

  private static CampaignEntity createCampaignEntity(
      UUID tenantId, UUID id, Optional<UUID> systemId, String title) {
    return new CampaignEntity(tenantId, id, systemId.orElse(null), title);
  }

  private static Campaign toCampaign(
      EntityBaseEntity entity, CampaignEntity campaign, @Nullable UUID effectiveSystemId) {
    return new Campaign(
        entity.getId(),
        entity.getGenerator(),
        effectiveSystemId,
        campaign.getTitle(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
