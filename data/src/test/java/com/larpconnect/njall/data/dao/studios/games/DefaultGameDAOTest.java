package com.larpconnect.njall.data.dao.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.dao.studios.events.EventEntity;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultGameDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultGameDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultGameDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById returns active Game with active campaign")
  void findById_activeGameAndCampaign_returnsGame() {
    var tx = beginTxWithTenant();
    mockGameRows(gameId, "Game", null, systemId, campaignId);
    mockRefEntity(campaignId, "Campaign", null);

    var found = dao.findById(tenantId, gameId);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().systemId()).isEqualTo(systemId);
    assertThat(found.orElseThrow().campaignId()).contains(campaignId);
    assertThat(found.orElseThrow().eventType()).isEqualTo(EventType.GAME);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns active Game without campaign")
  void findById_noCampaign_returnsGameWithEmptyCampaign() {
    var tx = beginTxWithTenant();
    mockGameRows(gameId, "Game", null, systemId, null);

    var found = dao.findById(tenantId, gameId);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().campaignId()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById masks soft-deleted campaign while preserving systemId")
  void findById_deletedCampaign_masksCampaignAndKeepsSystem() {
    var tx = beginTxWithTenant();
    mockGameRows(gameId, "Game", null, systemId, campaignId);
    mockRefEntity(campaignId, "Campaign", now);

    var found = dao.findById(tenantId, gameId);

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().campaignId()).isEmpty();
    assertThat(found.orElseThrow().systemId()).isEqualTo(systemId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById masks campaign when referenced entity is missing or wrong type")
  void findById_missingOrWrongTypeCampaign_masksCampaign() {
    var tx1 = beginTxWithTenant();
    mockGameRows(gameId, "Game", null, systemId, campaignId);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, campaignId))).thenReturn(null);
    assertThat(dao.findById(tenantId, gameId).orElseThrow().campaignId()).isEmpty();
    verify(tx1).commit();

    var tx2 = beginTxWithTenant();
    mockRefEntity(campaignId, "System", null);
    assertThat(dao.findById(tenantId, gameId).orElseThrow().campaignId()).isEmpty();
    verify(tx2).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity, event, or game is missing, deleted, or Event")
  void findById_missingDeletedOrNonGame_returnsEmpty() {
    var entityKey = new EntityId(tenantId, gameId);
    beginTxWithTenant();
    when(session.find(EntityBaseEntity.class, entityKey)).thenReturn(null);
    assertThat(dao.findById(tenantId, gameId)).isEmpty();

    beginTxWithTenant();
    mockRefEntity(gameId, "Game", null);
    when(session.find(EventEntity.class, entityKey)).thenReturn(null);
    assertThat(dao.findById(tenantId, gameId)).isEmpty();

    beginTxWithTenant();
    when(session.find(EventEntity.class, entityKey))
        .thenReturn(
            new EventEntity(tenantId, gameId, EventType.GAME, locationId, "Game", now, now));
    when(session.find(GameEntity.class, entityKey)).thenReturn(null);
    assertThat(dao.findById(tenantId, gameId)).isEmpty();

    beginTxWithTenant();
    mockGameRows(gameId, "Game", now, systemId, null);
    assertThat(dao.findById(tenantId, gameId)).isEmpty();

    beginTxWithTenant();
    mockGameRows(gameId, "Event", null, systemId, null);
    assertThat(dao.findById(tenantId, gameId)).isEmpty();
  }

  @Test
  @DisplayName("create persists EntityBaseEntity, EventEntity, and GameEntity atomically")
  void create_fullInputs_persistsCtiHierarchy() {
    var tx = beginTxWithTenant();
    mockActiveReferences();
    var end = now.plusSeconds(3600);
    var params =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.of(campaignId),
            "Midnight Court",
            Optional.of("Court session"),
            Optional.of(locationId),
            Optional.of(now),
            Optional.of(end));

    var created = dao.create(tenantId, params);

    assertThat(created.title()).isEqualTo("Midnight Court");
    assertThat(created.systemId()).isEqualTo(systemId);
    assertThat(created.campaignId()).contains(campaignId);
    assertThat(created.eventType()).isEqualTo(EventType.GAME);
    verify(session)
        .persist(
            argThat(
                (Object obj) ->
                    obj instanceof EntityBaseEntity base && "Game".equals(base.getEntityType())));
    verify(session)
        .persist(
            argThat(
                (Object obj) ->
                    obj instanceof EventEntity ev && ev.getEventType() == EventType.GAME));
    verify(session)
        .persist(
            argThat(
                (Object obj) -> obj instanceof GameEntity ge && ge.getSystemId().equals(systemId)));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create allows equal startTime and endTime and empty optional fields")
  void create_equalStartEndAndEmptyOptionals_succeeds() {
    var tx1 = beginTxWithTenant();
    mockActiveReferences();
    var minimal =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.empty(),
            "One Shot",
            Optional.empty(),
            Optional.empty(),
            Optional.of(now),
            Optional.of(now));
    var created = dao.create(tenantId, minimal);
    assertThat(created.campaignId()).isEmpty();
    verify(tx1).commit();

    var tx2 = beginTxWithTenant();
    var startOnly =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.empty(),
            "Start Only",
            Optional.empty(),
            Optional.empty(),
            Optional.of(now),
            Optional.empty());
    assertThat(dao.create(tenantId, startOnly).endTime()).isEmpty();
    verify(tx2).commit();
  }

  @Test
  @DisplayName("create rejects endTime strictly before startTime")
  void create_invalidTimeOrder_throws() {
    var tx = beginTxWithTenant();
    var params =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.empty(),
            "Bad Schedule",
            Optional.empty(),
            Optional.empty(),
            Optional.of(now.plusSeconds(60)),
            Optional.of(now));

    assertThatThrownBy(() -> dao.create(tenantId, params))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("endTime");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rejects non-Actor generator")
  void create_nonActorGenerator_throws() {
    var tx = beginTxWithTenant();
    mockRefEntity(actorId, "Location", null);
    var params =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.empty(),
            "Bad Actor",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(() -> dao.create(tenantId, params))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create rejects missing, soft-deleted, and non-System systemId")
  void create_invalidSystem_throws() {
    assertInvalidReferenceRejected(
        null, Optional.empty(), Optional.empty(), "System entity does not exist");
    assertInvalidReferenceRejected(
        new EntityBaseEntity(tenantId, systemId, actorId, "System", "Sys", now, now, now),
        Optional.empty(),
        Optional.empty(),
        "System entity does not exist");
    assertInvalidReferenceRejected(
        new EntityBaseEntity(tenantId, systemId, actorId, "Location", "Loc", now, now, null),
        Optional.empty(),
        Optional.empty(),
        "System entity does not exist");
  }

  @Test
  @DisplayName("create rejects missing, soft-deleted, and non-Campaign campaignId")
  void create_invalidCampaign_throws() {
    var activeSystem =
        new EntityBaseEntity(tenantId, systemId, actorId, "System", "Sys", now, now, null);
    assertInvalidReferenceRejected(
        activeSystem, Optional.of(campaignId), Optional.empty(), "Campaign entity does not exist");
    mockRefEntity(campaignId, "Campaign", now);
    var paramsDeleted =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.of(campaignId),
            "Game",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    beginTxWithTenant();
    mockRefEntity(actorId, "Actor", null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(activeSystem);
    assertThatThrownBy(() -> dao.create(tenantId, paramsDeleted))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Campaign");
  }

  @Test
  @DisplayName("create rejects missing, soft-deleted, and wrong-type locationId")
  void create_invalidLocation_throws() {
    var activeSystem =
        new EntityBaseEntity(tenantId, systemId, actorId, "System", "Sys", now, now, null);
    assertInvalidReferenceRejected(
        activeSystem, Optional.empty(), Optional.of(locationId), "Location entity does not exist");
    mockRefEntity(locationId, "Event", null);
    beginTxWithTenant();
    mockRefEntity(actorId, "Actor", null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(activeSystem);
    var paramsWrongType =
        new CreateGameParams(
            actorId,
            systemId,
            Optional.empty(),
            "Game",
            Optional.empty(),
            Optional.of(locationId),
            Optional.empty(),
            Optional.empty());
    assertThatThrownBy(() -> dao.create(tenantId, paramsWrongType))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Location");
  }

  @Test
  @DisplayName("softDelete marks active Game entity deleted and skips missing, deleted, or Event")
  void softDelete_handlesActiveAndInactiveEntities() {
    var tx1 = beginTxWithTenant();
    mockRefEntity(gameId, "Game", null);
    assertThat(dao.softDelete(tenantId, gameId)).isTrue();
    verify(session).merge(any(EntityBaseEntity.class));
    verify(tx1).commit();

    var tx2 = beginTxWithTenant();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, gameId))).thenReturn(null);
    assertThat(dao.softDelete(tenantId, gameId)).isFalse();
    verify(tx2).commit();

    beginTxWithTenant();
    mockRefEntity(gameId, "Game", now);
    assertThat(dao.softDelete(tenantId, gameId)).isFalse();

    beginTxWithTenant();
    mockRefEntity(gameId, "Event", null);
    assertThat(dao.softDelete(tenantId, gameId)).isFalse();
    verify(session, never()).remove(any());
  }

  @Test
  @DisplayName("GameDAO unscoped findById and list throw UnsupportedOperationException")
  void daoDefaults_unscopedMethods_throwUnsupported() {
    assertThatThrownBy(() -> dao.findById(gameId))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("transaction exceptions trigger rollback and suppress rollback errors")
  void transactionFailure_rollsBackAndAddsSuppressed() {
    var tx1 = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx1);
    when(session.createNativeQuery(any(String.class), eq(String.class)))
        .thenThrow(new IllegalStateException("DB error"));
    assertThatThrownBy(() -> dao.findById(tenantId, gameId))
        .isInstanceOf(IllegalStateException.class);
    verify(tx1).rollback();

    var tx2 = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx2);
    doThrow(new IllegalStateException("Rollback error")).when(tx2).rollback();
    assertThatThrownBy(() -> dao.softDelete(tenantId, gameId))
        .isInstanceOf(IllegalStateException.class)
        .satisfies(
            err ->
                assertThat(err.getSuppressed())
                    .hasSize(1)
                    .allSatisfy(sup -> assertThat(sup).hasMessage("Rollback error")));
  }

  private void assertInvalidReferenceRejected(
      @Nullable EntityBaseEntity systemEntity,
      Optional<UUID> refCampaignId,
      Optional<UUID> refLocationId,
      String expectedMessage) {
    var tx = beginTxWithTenant();
    mockRefEntity(actorId, "Actor", null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(systemEntity);
    refCampaignId.ifPresent(this::mockMissingRef);
    refLocationId.ifPresent(this::mockMissingRef);
    var params =
        new CreateGameParams(
            actorId,
            systemId,
            refCampaignId,
            "Invalid Ref Game",
            Optional.empty(),
            refLocationId,
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(() -> dao.create(tenantId, params))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(expectedMessage);
    verify(tx).rollback();
  }

  private Transaction beginTxWithTenant() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(any(String.class), eq(String.class))).thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), eq(tenantId.toString()))).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn("ok");
    return tx;
  }

  private void mockActiveReferences() {
    mockRefEntity(actorId, "Actor", null);
    mockRefEntity(systemId, "System", null);
    mockRefEntity(campaignId, "Campaign", null);
    mockRefEntity(locationId, "Location", null);
  }

  private void mockMissingRef(UUID id) {
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, id))).thenReturn(null);
  }

  private void mockRefEntity(UUID id, String entityType, @Nullable Instant deletedOn) {
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, id)))
        .thenReturn(
            new EntityBaseEntity(
                tenantId, id, actorId, entityType, "Summary", now, now, deletedOn));
  }

  private void mockGameRows(
      UUID id,
      String entityType,
      @Nullable Instant deletedOn,
      UUID rowSystemId,
      @Nullable UUID rowCampaignId) {
    var key = new EntityId(tenantId, id);
    when(session.find(EntityBaseEntity.class, key))
        .thenReturn(
            new EntityBaseEntity(
                tenantId, id, actorId, entityType, "Summary", now, now, deletedOn));
    when(session.find(EventEntity.class, key))
        .thenReturn(
            new EventEntity(tenantId, id, EventType.GAME, locationId, "Midnight Court", now, now));
    when(session.find(GameEntity.class, key))
        .thenReturn(new GameEntity(tenantId, id, rowSystemId, rowCampaignId));
  }
}
