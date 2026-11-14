package com.larpconnect.njall.data.dao.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

final class DefaultGameDAOPatchTest {

  private Session session;
  private DefaultGameDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID newSystemId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final UUID newCampaignId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    var sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultGameDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("patch updates system, campaign, title, summary, location, and times")
  void patch_allFields_updatesAndMerges() {
    var tx = beginTxWithTenant();
    var rows = mockActiveGameRows(campaignId, now, now.plusSeconds(3600));
    mockRef(newSystemId, "System", null);
    mockRef(newCampaignId, "Campaign", null);
    mockRef(locationId, "Location", null);
    var newStart = now.plusSeconds(600);
    var newEnd = now.plusSeconds(7200);
    var params =
        new PatchGameParams(
            Optional.of(newSystemId),
            Optional.of(newCampaignId),
            Optional.of("Night Two"),
            Optional.of("Updated summary"),
            Optional.of(locationId),
            Optional.of(newStart),
            Optional.of(newEnd));

    var updated = dao.patch(tenantId, gameId, params);

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().systemId()).isEqualTo(newSystemId);
    assertThat(updated.orElseThrow().campaignId()).contains(newCampaignId);
    assertThat(updated.orElseThrow().title()).isEqualTo("Night Two");
    assertThat(rows.event.getLocationId()).isEqualTo(locationId);
    verify(session).merge(rows.base);
    verify(session).merge(rows.event);
    verify(session).merge(rows.game);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch clears nullable fields when clear flags are set")
  void patch_clearFlags_clearsNullableFields() {
    var tx = beginTxWithTenant();
    var rows = mockActiveGameRows(campaignId, now, now.plusSeconds(3600));
    rows.event.setLocationId(locationId);
    var params =
        new PatchGameParams(
            Optional.empty(),
            Optional.of(newCampaignId),
            true,
            Optional.empty(),
            Optional.of("Ignored"),
            true,
            Optional.of(locationId),
            true,
            Optional.empty(),
            true,
            Optional.empty(),
            true);

    var updated = dao.patch(tenantId, gameId, params);

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().campaignId()).isEmpty();
    assertThat(updated.orElseThrow().summary()).isEmpty();
    assertThat(updated.orElseThrow().locationId()).isEmpty();
    assertThat(updated.orElseThrow().startTime()).isEmpty();
    assertThat(updated.orElseThrow().endTime()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch masks untouched soft-deleted campaign on returned Game")
  void patch_untouchedDeletedCampaign_masksCampaignInResult() {
    var tx = beginTxWithTenant();
    mockActiveGameRows(campaignId, null, null);
    mockRef(campaignId, "Campaign", now);
    var params =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.of("Renamed Only"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var updated = dao.patch(tenantId, gameId, params);

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().title()).isEqualTo("Renamed Only");
    assertThat(updated.orElseThrow().campaignId()).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch keeps untouched active campaign and handles partial time updates")
  void patch_untouchedActiveCampaign_preservesCampaign() {
    var tx = beginTxWithTenant();
    mockActiveGameRows(campaignId, now, now.plusSeconds(3600));
    mockRef(campaignId, "Campaign", null);
    var params =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            false,
            Optional.empty(),
            Optional.empty(),
            false,
            Optional.empty(),
            false,
            Optional.empty(),
            true,
            Optional.empty(),
            false);

    var updated = dao.patch(tenantId, gameId, params);

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().campaignId()).contains(campaignId);
    assertThat(updated.orElseThrow().startTime()).isEmpty();
    assertThat(updated.orElseThrow().endTime()).contains(now.plusSeconds(3600));
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rejects invalid system, campaign, or location references")
  void patch_invalidReferences_throws() {
    var tx1 = beginTxWithTenant();
    mockActiveGameRows(null, null, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, newSystemId)))
        .thenReturn(null);
    var badSystem =
        new PatchGameParams(
            Optional.of(newSystemId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThatThrownBy(() -> dao.patch(tenantId, gameId, badSystem))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx1).rollback();

    var tx2 = beginTxWithTenant();
    mockRef(newCampaignId, "Campaign", now);
    var badCampaign =
        new PatchGameParams(
            Optional.empty(),
            Optional.of(newCampaignId),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThatThrownBy(() -> dao.patch(tenantId, gameId, badCampaign))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx2).rollback();

    var tx3 = beginTxWithTenant();
    mockRef(locationId, "Actor", null);
    var badLocation =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(locationId),
            Optional.empty(),
            Optional.empty());
    assertThatThrownBy(() -> dao.patch(tenantId, gameId, badLocation))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx3).rollback();
  }

  @Test
  @DisplayName("patch rejects endTime preceding existing or updated startTime")
  void patch_invalidTimeOrder_throws() {
    var tx1 = beginTxWithTenant();
    mockActiveGameRows(null, now, now.plusSeconds(3600));
    var badEndOnly =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(now.minusSeconds(60)));
    assertThatThrownBy(() -> dao.patch(tenantId, gameId, badEndOnly))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx1).rollback();

    var tx2 = beginTxWithTenant();
    var badStartOnly =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(now.plusSeconds(7200)),
            Optional.empty());
    assertThatThrownBy(() -> dao.patch(tenantId, gameId, badStartOnly))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx2).rollback();
  }

  @Test
  @DisplayName("patch returns empty for missing, deleted, or non-Game entities")
  void patch_missingDeletedOrNonGame_returnsEmpty() {
    var params =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.of("Ignored"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    var tx1 = beginTxWithTenant();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, gameId))).thenReturn(null);
    assertThat(dao.patch(tenantId, gameId, params)).isEmpty();
    verify(tx1).commit();

    var tx2 = beginTxWithTenant();
    mockRef(gameId, "Event", null);
    when(session.find(EventEntity.class, new EntityId(tenantId, gameId)))
        .thenReturn(
            new EventEntity(tenantId, gameId, EventType.MEETUP, null, "Meetup", null, null));
    when(session.find(GameEntity.class, new EntityId(tenantId, gameId))).thenReturn(null);
    assertThat(dao.patch(tenantId, gameId, params)).isEmpty();
    verify(tx2).commit();

    var tx3 = beginTxWithTenant();
    var rows = mockActiveGameRows(null, null, null);
    rows.base.setDeletedOn(now);
    assertThat(dao.patch(tenantId, gameId, params)).isEmpty();
    verify(tx3).commit();
  }

  @Test
  @DisplayName("patch rolls back and rethrows when session throws")
  void patch_exception_rollsBack() {
    var tx = beginTxWithTenant();
    when(session.find(eq(EntityBaseEntity.class), any(EntityId.class)))
        .thenThrow(new IllegalStateException("Patch failure"));
    var params =
        new PatchGameParams(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    assertThatThrownBy(() -> dao.patch(tenantId, gameId, params))
        .isInstanceOf(IllegalStateException.class);
    verify(tx).rollback();
  }

  private GameRows mockActiveGameRows(
      @Nullable UUID rowCampaignId, @Nullable Instant start, @Nullable Instant end) {
    var key = new EntityId(tenantId, gameId);
    var base = new EntityBaseEntity(tenantId, gameId, actorId, "Game", "Summary", now, now, null);
    var event = new EventEntity(tenantId, gameId, EventType.GAME, null, "Night One", start, end);
    var game = new GameEntity(tenantId, gameId, systemId, rowCampaignId);
    when(session.find(EntityBaseEntity.class, key)).thenReturn(base);
    when(session.find(EventEntity.class, key)).thenReturn(event);
    when(session.find(GameEntity.class, key)).thenReturn(game);
    return new GameRows(base, event, game);
  }

  private void mockRef(UUID id, String entityType, @Nullable Instant deletedOn) {
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, id)))
        .thenReturn(
            new EntityBaseEntity(
                tenantId, id, actorId, entityType, "Summary", now, now, deletedOn));
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

  private static final class GameRows {
    final EntityBaseEntity base;
    final EventEntity event;
    final GameEntity game;

    GameRows(EntityBaseEntity base, EventEntity event, GameEntity game) {
      this.base = base;
      this.event = event;
      this.game = game;
    }
  }
}
