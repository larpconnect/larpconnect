package com.larpconnect.njall.data.dao.studios.events;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.larpconnect.njall.data.annotation.NjallUsers;
import com.larpconnect.njall.data.dao.common.ActorGeneratorValidator;
import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.Event;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jspecify.annotations.Nullable;

final class DefaultEventDAO implements EventDAO {

  private static final String ENTITY_TYPE_EVENT = "Event";
  private static final String ENTITY_TYPE_LOCATION = "Location";
  private static final String SET_TENANT_CONFIG =
      "SELECT set_config('app.tenant_id', :tenantId, true)";
  private static final String LIST_EVENTS_HQL =
      """
      SELECT e, ev FROM EntityBaseEntity e, EventEntity ev
      WHERE e.tenantId = :tenantId AND ev.tenantId = :tenantId
        AND e.id = ev.id AND e.entityType = :entityType AND e.deletedOn IS NULL
      ORDER BY ev.startTime ASC NULLS LAST, e.createdOn DESC
      """;

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
        var entityId = createEntityId(tenantId, eventId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        tx.commit();
        if (!isActiveEvent(entity, event)) {
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
        var rows = queryEventRows(session, tenantId);
        tx.commit();
        return mapRowsToEvents(rows);
      } catch (Exception e) {
        rollbackQuietly(tx, e);
        throw e;
      }
    }
  }

  @Override
  public Event create(
      UUID tenantId,
      UUID generator,
      EventType eventType,
      String title,
      Optional<String> summary,
      Optional<UUID> locationId,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    requireNonGameEventType(eventType);
    validateTimeOrder(startTime.orElse(null), endTime.orElse(null));
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        ActorGeneratorValidator.validateActorGenerator(session, tenantId, generator);
        if (locationId.isPresent()) {
          validateActiveLocation(session, tenantId, locationId.orElseThrow());
        }
        var eventId = UUID.randomUUID();
        var now = Instant.now();
        var entity = createBaseEntity(tenantId, eventId, generator, summary, now);
        session.persist(entity);

        var event =
            createEventEntity(tenantId, eventId, eventType, locationId, title, startTime, endTime);
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
  public Optional<Event> patch(UUID tenantId, UUID eventId, PatchEventParams params) {
    params.eventType().ifPresent(DefaultEventDAO::requireNonGameEventType);
    try (var session = sessionFactoryProvider.get().openSession()) {
      var tx = session.beginTransaction();
      try {
        setTenantContext(session, tenantId);
        var entityId = createEntityId(tenantId, eventId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        var event = session.find(EventEntity.class, entityId);
        if (!isActiveEvent(entity, event)) {
          tx.commit();
          return Optional.empty();
        }
        validatePatchInputs(session, tenantId, event, params);
        applyEventPatch(entity, event, params);
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
        var entityId = createEntityId(tenantId, eventId);
        var entity = session.find(EntityBaseEntity.class, entityId);
        if (entity == null
            || entity.getDeletedOn() != null
            || !ENTITY_TYPE_EVENT.equals(entity.getEntityType())) {
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

  private static List<Object[]> queryEventRows(Session session, UUID tenantId) {
    return session
        .createQuery(LIST_EVENTS_HQL, Object[].class)
        .setParameter("tenantId", tenantId)
        .setParameter("entityType", ENTITY_TYPE_EVENT)
        .getResultList();
  }

  private static ImmutableList<Event> mapRowsToEvents(List<Object[]> rows) {
    return rows.stream()
        .map(DefaultEventDAO::mapRowToEvent)
        .collect(ImmutableList.toImmutableList());
  }

  private static Event mapRowToEvent(Object[] row) {
    return toEvent((EntityBaseEntity) row[0], (EventEntity) row[1]);
  }

  private static void validatePatchInputs(
      Session session, UUID tenantId, EventEntity event, PatchEventParams params) {
    var nextStart =
        resolvePatchedValue(event.getStartTime(), params.startTime(), params.clearStartTime());
    var nextEnd = resolvePatchedValue(event.getEndTime(), params.endTime(), params.clearEndTime());
    validateTimeOrder(nextStart, nextEnd);
    if (!params.clearLocationId() && params.locationId().isPresent()) {
      validateActiveLocation(session, tenantId, params.locationId().orElseThrow());
    }
  }

  private static void validateTimeOrder(@Nullable Instant startTime, @Nullable Instant endTime) {
    if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
      throw createIllegalArgumentException("Event end time must not be earlier than start time");
    }
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

  private static void requireNonGameEventType(EventType eventType) {
    if (eventType == EventType.GAME) {
      throw createIllegalArgumentException(
          "Events with eventType GAME must be managed via games endpoints");
    }
  }

  private static boolean isActiveEvent(
      @Nullable EntityBaseEntity entity, @Nullable EventEntity event) {
    return entity != null
        && event != null
        && entity.getDeletedOn() == null
        && ENTITY_TYPE_EVENT.equals(entity.getEntityType());
  }

  private static void validateActiveLocation(Session session, UUID tenantId, UUID locationId) {
    var locationEntity = session.find(EntityBaseEntity.class, createEntityId(tenantId, locationId));
    if (locationEntity == null
        || locationEntity.getDeletedOn() != null
        || !ENTITY_TYPE_LOCATION.equals(locationEntity.getEntityType())) {
      throw createIllegalArgumentException(
          "Location entity does not exist or is not active in tenant "
              + tenantId
              + ": "
              + locationId);
    }
  }

  private static void applyEventPatch(
      EntityBaseEntity entity, EventEntity event, PatchEventParams params) {
    params.eventType().ifPresent(event::setEventType);
    params.title().ifPresent(event::setTitle);
    applyNullableUpdate(params.summary(), params.clearSummary(), entity::setSummary);
    applyNullableUpdate(params.locationId(), params.clearLocationId(), event::setLocationId);
    applyNullableUpdate(params.startTime(), params.clearStartTime(), event::setStartTime);
    applyNullableUpdate(params.endTime(), params.clearEndTime(), event::setEndTime);
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
        tenantId, id, generator, ENTITY_TYPE_EVENT, summary.orElse(null), now, now, null);
  }

  private static EventEntity createEventEntity(
      UUID tenantId,
      UUID id,
      EventType eventType,
      Optional<UUID> locationId,
      String title,
      Optional<Instant> startTime,
      Optional<Instant> endTime) {
    return new EventEntity(
        tenantId,
        id,
        eventType,
        locationId.orElse(null),
        title,
        startTime.orElse(null),
        endTime.orElse(null));
  }

  private static Event toEvent(EntityBaseEntity entity, EventEntity event) {
    return new Event(
        entity.getId(),
        entity.getGenerator(),
        event.getEventType(),
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
