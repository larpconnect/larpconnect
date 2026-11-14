package com.larpconnect.njall.data.dao.studios.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DefaultEventDAOTest {

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
  @DisplayName("findById sets app.tenant_id and returns active Event")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity =
        new EntityBaseEntity(tenantId, eventId, owner, "Event", "Festival summary", now, now, null);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.CONFERENCE, locationId, "Autumn Fest", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result = dao.findById(tenantId, eventId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(eventId);
    assertThat(result.orElseThrow().generator()).isEqualTo(owner);
    assertThat(result.orElseThrow().eventType()).isEqualTo(EventType.CONFERENCE);
    assertThat(result.orElseThrow().title()).isEqualTo("Autumn Fest");
    assertThat(result.orElseThrow().locationId()).contains(locationId);
    assertThat(result.orElseThrow().summary()).contains("Festival summary");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity_type is Game rather than Event")
  void findById_gameEntityType_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var gameBase =
        new EntityBaseEntity(tenantId, eventId, owner, "Game", "Game summary", now, now, null);
    var event =
        new EventEntity(
            tenantId, eventId, EventType.GAME, locationId, "Campaign Session", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(gameBase);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result = dao.findById(tenantId, eventId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when event is soft-deleted")
  void findById_softDeleted() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, now);
    var event =
        new EventEntity(tenantId, eventId, EventType.WORKSHOP, locationId, "Autumn Fest", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result = dao.findById(tenantId, eventId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity does not exist")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);
    when(session.find(EventEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, eventId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when event entity is missing")
  void findById_eventEntityMissing() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, eventId);
    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, eventId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("listAll returns active Event-typed events ordered chronologically")
  void listAll_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    Query<Object[]> query = mock();
    when(session.createQuery(any(String.class), eq(Object[].class))).thenReturn(query);
    when(query.setParameter(eq("tenantId"), eq(tenantId))).thenReturn(query);
    when(query.setParameter(eq("entityType"), eq("Event"))).thenReturn(query);

    var entity = new EntityBaseEntity(tenantId, eventId, owner, "Event", "Summary", now, now, null);
    var event =
        new EventEntity(tenantId, eventId, EventType.MEETUP, locationId, "Autumn Fest", now, now);
    when(query.getResultList()).thenReturn(List.<Object[]>of(new Object[] {entity, event}));

    var results = dao.listAll(tenantId);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).title()).isEqualTo("Autumn Fest");
    assertThat(results.get(0).eventType()).isEqualTo(EventType.MEETUP);
    assertThat(results.get(0).generator()).isEqualTo(owner);
    verify(query).setParameter("entityType", "Event");
    verify(tx).commit();
  }

  @Test
  @DisplayName("create persists entity and event records within tenant context")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    var activeLocation =
        new EntityBaseEntity(tenantId, locationId, owner, "Location", "Venue", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, locationId)))
        .thenReturn(activeLocation);

    var created =
        dao.create(
            tenantId,
            owner,
            EventType.WORKSHOP,
            "Spring Gathering",
            Optional.of("New gathering"),
            Optional.of(locationId),
            Optional.of(now),
            Optional.of(now.plusSeconds(3600)));

    assertThat(created.title()).isEqualTo("Spring Gathering");
    assertThat(created.eventType()).isEqualTo(EventType.WORKSHOP);
    assertThat(created.generator()).isEqualTo(owner);
    assertThat(created.summary()).contains("New gathering");
    assertThat(created.locationId()).contains(locationId);
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(EventEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create persists entity and event with empty optionals")
  void create_withEmptyOptionals_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);

    var created =
        dao.create(
            tenantId,
            owner,
            EventType.EVENT_TYPE_UNSPECIFIED,
            "Minimal Gathering",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(created.title()).isEqualTo("Minimal Gathering");
    assertThat(created.eventType()).isEqualTo(EventType.EVENT_TYPE_UNSPECIFIED);
    assertThat(created.generator()).isEqualTo(owner);
    assertThat(created.summary()).isEmpty();
    assertThat(created.locationId()).isEmpty();
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(EventEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rejects EventType.GAME with IllegalArgumentException")
  void create_gameEventType_throws() {
    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.GAME,
                    "Disallowed Game Event",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("GAME");
  }

  @Test
  @DisplayName("create rejects endTime earlier than startTime before opening session")
  void create_invertedTimeRange_throws() {
    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.MEETUP,
                    "Inverted Gathering",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(now.plusSeconds(3600)),
                    Optional.of(now)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("earlier than start time");
  }

  @Test
  @DisplayName("create rejects missing locationId")
  void create_missingLocation_throws() {
    assertInvalidLocationRejected(null, "Missing Venue");
  }

  @Test
  @DisplayName("create rejects soft-deleted locationId")
  void create_deletedLocation_throws() {
    var deletedLocation =
        new EntityBaseEntity(tenantId, locationId, owner, "Location", "Venue", now, now, now);
    assertInvalidLocationRejected(deletedLocation, "Deleted Venue");
  }

  @Test
  @DisplayName("create rejects non-Location entity for locationId")
  void create_nonLocationEntity_throws() {
    var wrongType =
        new EntityBaseEntity(tenantId, locationId, owner, "Link", "Link", now, now, null);
    assertInvalidLocationRejected(wrongType, "Wrong Type Venue");
  }

  private void assertInvalidLocationRejected(
      @Nullable EntityBaseEntity locationEntity, String title) {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, locationId)))
        .thenReturn(locationEntity);

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.MEETUP,
                    title,
                    Optional.empty(),
                    Optional.of(locationId),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when owner does not exist")
  void create_ownerNotFound_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner))).thenReturn(null);

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.EVENT_TYPE_UNSPECIFIED,
                    "Spring Gathering",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when owner is soft deleted")
  void create_ownerDeleted_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var deletedOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(deletedOwner);

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.EVENT_TYPE_UNSPECIFIED,
                    "Spring Gathering",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when owner is not an Actor")
  void create_ownerNotActor_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var nonActorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Location", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(nonActorOwner);

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    owner,
                    EventType.EVENT_TYPE_UNSPECIFIED,
                    "Spring Gathering",
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Database errors trigger rollback and suppress secondary rollback errors")
  void rollbackOnError_withSuppression() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));
    Mockito.doThrow(new RuntimeException("Rollback error")).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(tenantId, eventId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback error");
            });
    verify(tx).rollback();
  }

  @Test
  @DisplayName("Unsupported methods throw UnsupportedOperationException")
  void unsupportedMethods_throwException() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dao.findById(UUID.randomUUID()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
