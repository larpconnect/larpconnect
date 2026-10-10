package com.larpconnect.njall.data.dao.studios;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.Event;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

final class DefaultEventDAO implements EventDAO {

  private static final String ENTITY_TYPE_EVENT = "Event";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";

  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultEventDAO(@NjallUsers Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public Optional<Event> findById(UUID tenantId, UUID eventId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, eventId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        tx.commit();
        if (entity == null || event == null || entity.getDeletedOn() != null) {
          return Optional.empty();
        }
        return Optional.of(toEvent(entity, event));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public ImmutableList<Event> listAll(UUID tenantId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var rows =
            session
                .createQuery(
                    "SELECT e, ev FROM EntityBaseEntity e, EventEntity ev "
                        + "WHERE e.tenantId = :tenantId AND ev.tenantId = :tenantId "
                        + "AND e.id = ev.id AND e.deletedOn IS NULL "
                        + "ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC",
                    Object[].class)
                .setParameter("tenantId", tenantId)
                .getResultList();
        tx.commit();
        var list = new ArrayList<Event>(rows.size());
        for (var row : rows) {
          list.add(toEvent((EntityBaseEntity) row[0], (EventEntity) row[1]));
        }
        return ImmutableList.copyOf(list);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Event create(
      UUID tenantId,
      String title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var eventId = UUID.randomUUID();
        var now = Instant.now();
        var entity =
            new EntityBaseEntity(
                tenantId, eventId, ENTITY_TYPE_EVENT, summary.orElse(null), now, now, null);
        session.persist(entity);

        var event =
            new EventEntity(
                tenantId,
                eventId,
                locationId.orElse(null),
                title,
                startTime.orElse(null),
                endTime.orElse(null));
        session.persist(event);

        tx.commit();
        return toEvent(entity, event);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Optional<Event> patch(
      UUID tenantId,
      UUID eventId,
      Optional<String> title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, eventId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        if (entity == null || event == null || entity.getDeletedOn() != null) {
          tx.commit();
          return Optional.empty();
        }
        title.ifPresent(event::setTitle);
        summary.ifPresent(entity::setSummary);
        locationId.ifPresent(event::setLocationId);
        startTime.ifPresent(event::setStartTime);
        endTime.ifPresent(event::setEndTime);
        entity.setUpdatedOn(Instant.now());
        session.merge(entity);
        session.merge(event);
        tx.commit();
        return Optional.of(toEvent(entity, event));
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public boolean softDelete(UUID tenantId, UUID eventId) {
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = new EntityId(tenantId, eventId);
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

  private static Event toEvent(EntityBaseEntity entity, EventEntity event) {
    return new Event(
        entity.getId(),
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
