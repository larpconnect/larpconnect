package com.larpconnect.njall.data.dao.studios.individuals;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Individual;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultIndividualDAO implements IndividualDAO {

  private static final String ENTITY_TYPE_INDIVIDUAL = "Individual";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultIndividualDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Individual> findById(UUID tenantId, UUID individualId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, individualId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var individual = session.find(IndividualEntity.class, entityId);
        tx.commit();
        if (entity == null || individual == null || entity.getDeletedOn() != null) {
          return Optional.empty();
        }
        return Optional.of(mapToDomain(entity, individual));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Individual create(UUID tenantId, String name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var individualId = UUID.randomUUID();
        var now = Instant.now();
        var baseEntity = createBaseEntity(tenantId, individualId, summary, now);
        session.persist(baseEntity);

        var individualEntity = createIndividualEntity(tenantId, individualId, name);
        session.persist(individualEntity);

        session.flush();
        tx.commit();
        return mapToDomain(baseEntity, individualEntity);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Individual> patch(
      UUID tenantId, UUID individualId, Optional<String> name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, individualId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var individual = session.find(IndividualEntity.class, entityId);
        if (entity == null || individual == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }

        name.ifPresent(individual::setName);
        summary.ifPresent(entity::setSummary);
        entity.setUpdatedOn(Instant.now());

        session.merge(entity);
        session.merge(individual);
        tx.commit();
        return Optional.of(mapToDomain(entity, individual));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID individualId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, individualId);
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

  private static EntityId createEntityId(UUID tenantId, UUID individualId) {
    return new EntityId(tenantId, individualId);
  }

  private static EntityBaseEntity createBaseEntity(
      UUID tenantId, UUID individualId, Optional<String> summary, Instant now) {
    return new EntityBaseEntity(
        tenantId, individualId, ENTITY_TYPE_INDIVIDUAL, summary.orElse(null), now, now, null);
  }

  private static IndividualEntity createIndividualEntity(
      UUID tenantId, UUID individualId, String name) {
    return new IndividualEntity(tenantId, individualId, name);
  }

  private Individual mapToDomain(EntityBaseEntity entity, IndividualEntity individual) {
    return new Individual(
        individual.getId(),
        individual.getName(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
