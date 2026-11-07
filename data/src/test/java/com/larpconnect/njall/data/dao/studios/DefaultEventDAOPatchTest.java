package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
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
    var event = new EventEntity(tenantId, eventId, locationId, "Old Title", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var updated =
        dao.patch(
            tenantId,
            eventId,
            Optional.of("New Title"),
            Optional.of("New summary"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().title()).isEqualTo("New Title");
    assertThat(updated.orElseThrow().summary()).contains("New summary");
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
    var event = new EventEntity(tenantId, eventId, null, "Old Title", null, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var newLocationId = UUID.randomUUID();
    var newStart = now.plusSeconds(60);
    var newEnd = now.plusSeconds(3600);

    var updated =
        dao.patch(
            tenantId,
            eventId,
            Optional.empty(),
            Optional.empty(),
            Optional.of(newLocationId),
            Optional.of(newStart),
            Optional.of(newEnd));

    assertThat(updated).isPresent();
    assertThat(event.getLocationId()).isEqualTo(newLocationId);
    assertThat(event.getStartTime()).isEqualTo(newStart);
    assertThat(event.getEndTime()).isEqualTo(newEnd);
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
            Optional.of("Title"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

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
    var event = new EventEntity(tenantId, eventId, locationId, "Title", now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(EventEntity.class, entityId)).thenReturn(event);

    var result =
        dao.patch(
            tenantId,
            eventId,
            Optional.of("New Title"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

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
            Optional.of("Title"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

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
    @SuppressWarnings("unchecked")
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
