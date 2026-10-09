package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Location;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

final class DefaultLocationDAO implements LocationDAO {

  private static final String ENTITY_TYPE_LOCATION = "Location";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultLocationDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Location> findById(UUID tenantId, UUID locationId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, locationId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var location = session.find(LocationEntity.class, entityId);
        tx.commit();
        if (entity == null || location == null || entity.getDeletedOn() != null) {
          return Optional.empty();
        }
        return Optional.of(toLocation(entity, location));
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public Location create(UUID tenantId, String name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var locationId = UUID.randomUUID();
        var now = Instant.now();
        var entity =
            new EntityBaseEntity(
                tenantId, locationId, ENTITY_TYPE_LOCATION, summary.orElse(null), now, now, null);
        session.persist(entity);

        var location = new LocationEntity(tenantId, locationId, name);
        session.persist(location);

        tx.commit();
        return toLocation(entity, location);
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public Optional<Location> patch(
      UUID tenantId, UUID locationId, Optional<String> name, Optional<String> summary) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, locationId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var location = session.find(LocationEntity.class, entityId);
        if (entity == null || location == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        summary.ifPresent(entity::setSummary);
        entity.setUpdatedOn(Instant.now());
        name.ifPresent(location::setName);
        session.merge(entity);
        session.merge(location);
        tx.commit();
        return Optional.of(toLocation(entity, location));
      } catch (Exception e) {
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID locationId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, locationId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (entity == null || entity.getDeletedOn() != null) {
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
        try {
          tx.rollback();
        } catch (Exception rollbackException) {
          e.addSuppressed(rollbackException);
        }
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Location> list() {
    throw new UnsupportedOperationException(
        "Multi-tenant location listing is not permitted in user space");
  }

  private void setTenantContext(Session session, UUID tenantId) {
    session
        .createNativeQuery(SET_TENANT_CONFIG, String.class)
        .setParameter("tenantId", tenantId.toString())
        .getSingleResult();
  }

  private static Location toLocation(EntityBaseEntity entity, LocationEntity location) {
    return new Location(
        entity.getId(),
        location.getName(),
        entity.getSummary(),
        entity.getCreatedOn(),
        entity.getUpdatedOn(),
        entity.getDeletedOn());
  }
}
