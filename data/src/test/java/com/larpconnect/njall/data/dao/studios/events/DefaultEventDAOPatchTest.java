package com.larpconnect.njall.data.dao.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultEventDAOPatchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultEventDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID eventId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultEventDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("patch updates mutable fields and returns updated Event")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity =
        new EntityBaseEntity(tenantId, eventId, owner, "Event", "Old summary", now, now, null);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.EVENT_TYPE_UNSPECIFIED, locationId, "Old Title", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var updated =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.of(EventType.WORKSHOP),
                Optional.of("New Title"),
                Optional.of("New summary"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().eventType()).isEqualTo(EventType.WORKSHOP);
    assertThat(updated.orElseThrow().title()).isEqualTo("New Title");
    assertThat(updated.orElseThrow().summary()).contains("New summary");
    assertThat(event.getEventType()).isEqualTo(EventType.WORKSHOP);
    assertThat(event.getTitle()).isEqualTo("New Title");
    assertThat(entity.getSummary()).isEqualTo("New summary");
    verify(session).merge(entity);
    verify(session).merge(event);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates locationId, startTime, endTime when present")
  void patch_allFieldsPresent() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity =
        new EntityBaseEntity(tenantId, eventId, owner, "Event", "Old summary", now, now, null);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.EVENT_TYPE_UNSPECIFIED, null, "Old Title", null, null);
    var newLocationId = UUID.randomUUID();
    var activeLocation =
        new EntityBaseEntity(tenantId, newLocationId, owner, "Location", "Venue", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, newLocationId)))
        .thenReturn(activeLocation);

    var newStart = now.plusSeconds(60);
    var newEnd = now.plusSeconds(3600);

    var updated =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(newLocationId),
                Optional.of(newStart),
                Optional.of(newEnd)));

    assertThat(updated).isPresent();
    assertThat(event.getLocationId()).isEqualTo(newLocationId);
    assertThat(event.getStartTime()).isEqualTo(newStart);
    assertThat(event.getEndTime()).isEqualTo(newEnd);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch clears nullable fields when clear flags are set")
  void patch_clearFlags_clearsNullableFields() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity =
        new EntityBaseEntity(tenantId, eventId, owner, "Event", "Old summary", now, now, null);
    var event =
        new EventEntity(
            tenantId,
            eventId,
            EventType.CONFERENCE,
            locationId,
            "Conference",
            now,
            now.plusSeconds(3600));

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var updated =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true,
                Optional.of(UUID.randomUUID()),
                true,
                Optional.empty(),
                true,
                Optional.empty(),
                true));

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().summary()).isEmpty();
    assertThat(updated.orElseThrow().locationId()).isEmpty();
    assertThat(updated.orElseThrow().startTime()).isEmpty();
    assertThat(updated.orElseThrow().endTime()).isEmpty();
    assertThat(entity.getSummary()).isNull();
    assertThat(event.getLocationId()).isNull();
    assertThat(event.getStartTime()).isNull();
    assertThat(event.getEndTime()).isNull();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rejects EventType.GAME with IllegalArgumentException")
  void patch_gameEventType_throws() {
    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    eventId,
                    new PatchEventParams(
                        Optional.of(EventType.GAME),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GAME");
  }

  @Test
  @DisplayName("patch throws IllegalArgumentException when locationId is inactive")
  void patch_invalidLocation_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity =
        new EntityBaseEntity(tenantId, eventId, owner, "Event", "Old summary", now, now, null);
    var event =
        new EventEntity(tenantId, eventId, EventType.WORKSHOP, null, "Workshop", null, null);
    var badLocationId = UUID.randomUUID();

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, badLocationId)))
        .thenReturn(null);

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    eventId,
                    new PatchEventParams(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(badLocationId),
                        Optional.empty(),
                        Optional.empty())))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch rejects single-sided timestamp update that inverts persisted schedule")
  void patch_singleSidedInvertedTimestamps_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, null);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.WORKSHOP, null, "Workshop", now, now.plusSeconds(3600));
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    eventId,
                    new PatchEventParams(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(now.minusSeconds(60)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("earlier than start time");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch returns empty when entity_type is Game")
  void patch_gameEntityType_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var gameBase = new EntityBaseEntity(tenantId, eventId, owner, "Game", "Game", now, now, null);
    var event = new EventEntity(tenantId, eventId, EventType.GAME, null, "Game", null, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(gameBase);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.of("Attempted Hijack"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

    assertThat(result).isEmpty();
    verify(session, never()).merge(any());
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when event is missing or soft-deleted")
  void patch_missingOrDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.of("Title"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when event is soft-deleted")
  void patch_whenDeleted_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, now);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.EVENT_TYPE_UNSPECIFIED, locationId, "Title", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.of("New Title"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when event entity is missing")
  void patch_eventEntityMissing() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(
            tenantId,
            eventId,
            new PatchEventParams(
                Optional.empty(),
                Optional.of("Title"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete marks deleted_on on active event")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var deleted = dao.softDelete(tenantId, eventId);

    assertThat(deleted).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity_type is Game")
  void softDelete_gameEntityType_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var gameBase = new EntityBaseEntity(tenantId, eventId, owner, "Game", "Game", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(gameBase);

    var deleted = dao.softDelete(tenantId, eventId);

    assertThat(deleted).isFalse();
    verify(session, never()).merge(any());
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity does not exist")
  void softDelete_entityNotFound_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var deleted = dao.softDelete(tenantId, eventId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when event is already soft-deleted")
  void softDelete_missingOrAlreadyDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, now);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var deleted = dao.softDelete(tenantId, eventId);

    assertThat(deleted).isFalse();
    verify(tx).commit();
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
