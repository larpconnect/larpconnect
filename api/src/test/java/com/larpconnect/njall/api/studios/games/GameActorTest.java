package com.larpconnect.njall.api.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.api.studios.common.StudioResourceUriSupport;
import com.larpconnect.njall.data.dao.studios.games.CreateGameParams;
import com.larpconnect.njall.data.dao.studios.games.GameDAO;
import com.larpconnect.njall.data.dao.studios.games.PatchGameParams;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.EventType;
import com.larpconnect.njall.data.domain.studios.Game;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

final class GameActorTest {

  private static final String ALIAS = "valiant";
  private static final Instant START = Instant.parse("2026-06-01T18:00:00Z");
  private static final Instant END = Instant.parse("2026-06-01T22:00:00Z");
  private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID gameId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID campaignId = UUID.randomUUID();
  private final UUID locationId = UUID.randomUUID();

  private StudioLookup studioLookup() {
    return new StudioLookup(tenantId, studioId, ALIAS, NOW, NOW, Optional.empty());
  }

  private String actorUri(String studioIdentifier) {
    return StudioResourceUriSupport.formatResourceUri(
        studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, actorId);
  }

  private String systemUri(String studioIdentifier) {
    return StudioResourceUriSupport.formatResourceUri(
        studioIdentifier, StudioResourceUriSupport.SYSTEMS_COLLECTION, systemId);
  }

  private String campaignUri(String studioIdentifier) {
    return StudioResourceUriSupport.formatResourceUri(
        studioIdentifier, StudioResourceUriSupport.CAMPAIGNS_COLLECTION, campaignId);
  }

  private Game sampleGame(Optional<UUID> campaign) {
    return new Game(
        gameId,
        actorId,
        systemId,
        campaign,
        Optional.of(locationId),
        "Chronicles of Dusk",
        Optional.of("Season premiere"),
        Optional.of(START),
        Optional.of(END),
        NOW,
        NOW,
        Optional.empty());
  }

  private BehaviorTestKit<GameCommand> createKit(GameDAO dao) {
    return BehaviorTestKit.create(new DefaultGameActorFactory(dao).create());
  }

  @Test
  @DisplayName("CreateGame succeeds with canonical URIs across studio alias and UUID via lookup")
  void onCreateGame_validCanonicalUris_returnsSuccess() {
    var dao = mock(GameDAO.class);
    var domain = sampleGame(Optional.of(campaignId));
    var paramsCaptor = ArgumentCaptor.forClass(CreateGameParams.class);
    when(dao.create(eq(tenantId), paramsCaptor.capture())).thenReturn(domain);

    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();
    var request =
        new CreateGameRequest(
            actorUri(studioId.toString()),
            systemUri(ALIAS),
            campaignUri(studioId.toString()),
            "Chronicles of Dusk",
            "Season premiere",
            locationId,
            START,
            END);

    kit.run(
        new GameCommand.CreateGame(
            tenantId, ALIAS, Optional.of(studioLookup()), request, inbox.getRef()));

    var response = inbox.receiveMessage();
    assertThat(response).isInstanceOf(GameActorResponse.Success.class);
    var body = ((GameActorResponse.Success) response).game();
    assertThat(body.id()).isEqualTo(gameId);
    assertThat(body.generator()).isEqualTo(actorUri(ALIAS));
    assertThat(body.system()).isEqualTo(systemUri(ALIAS));
    assertThat(body.campaign()).contains(campaignUri(ALIAS));
    assertThat(body.eventType()).isEqualTo(EventType.GAME);
    assertThat(body.locationId()).contains(locationId);
    assertThat(body.startTime()).contains(START);
    assertThat(body.endTime()).contains(END);

    var captured = paramsCaptor.getValue();
    assertThat(captured.generator()).isEqualTo(actorId);
    assertThat(captured.systemId()).isEqualTo(systemId);
    assertThat(captured.campaignId()).contains(campaignId);
    assertThat(captured.locationId()).contains(locationId);
  }

  @Test
  @DisplayName("CreateGame returns BadRequest on validation error, wrong collection, or DAO error")
  void onCreateGame_invalidInputs_returnsBadRequest() {
    var dao = mock(GameDAO.class);
    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();

    var blankTitle = new CreateGameRequest(actorUri(ALIAS), systemUri(ALIAS), " ");
    kit.run(new GameCommand.CreateGame(tenantId, ALIAS, blankTitle, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(400));
    verify(dao, never()).create(any(), any());

    var wrongCollection = new CreateGameRequest(actorUri(ALIAS), actorUri(ALIAS), "Valid Title");
    kit.run(new GameCommand.CreateGame(tenantId, ALIAS, wrongCollection, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(400));

    when(dao.create(eq(tenantId), any()))
        .thenThrow(new IllegalArgumentException("System not found"));
    var validReq = new CreateGameRequest(actorUri(ALIAS), systemUri(ALIAS), "One Shot");
    kit.run(new GameCommand.CreateGame(tenantId, ALIAS, validReq, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class,
            f -> {
              assertThat(f.status()).isEqualTo(400);
              assertThat(f.message()).isEqualTo("System not found");
            });
  }

  @Test
  @DisplayName("CreateGame returns 500 Failure with fallback message on unexpected exception")
  void onCreateGame_unexpectedException_returnsFailure() {
    var dao = mock(GameDAO.class);
    when(dao.create(eq(tenantId), any())).thenThrow(new RuntimeException());

    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();
    var validReq = new CreateGameRequest(actorUri(ALIAS), systemUri(ALIAS), "One Shot");
    kit.run(new GameCommand.CreateGame(tenantId, ALIAS, validReq, inbox.getRef()));

    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class,
            f -> {
              assertThat(f.status()).isEqualTo(500);
              assertThat(f.message()).isEqualTo("Error executing create game");
            });
  }

  @Test
  @DisplayName("GetGame masks empty campaign while retaining system URI, handles 404 and 500")
  void onGetGame_handlesFoundNotFoundAndError() {
    var dao = mock(GameDAO.class);
    when(dao.findById(tenantId, gameId)).thenReturn(Optional.of(sampleGame(Optional.empty())));
    var otherId = UUID.randomUUID();
    when(dao.findById(tenantId, otherId)).thenReturn(Optional.empty());

    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();

    kit.run(new GameCommand.GetGame(tenantId, ALIAS, gameId, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Success.class,
            s -> {
              assertThat(s.game().system()).isEqualTo(systemUri(ALIAS));
              assertThat(s.game().campaign()).isEmpty();
            });

    kit.run(new GameCommand.GetGame(tenantId, ALIAS, otherId, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(404));

    when(dao.findById(tenantId, gameId)).thenThrow(new RuntimeException("Connection lost"));
    kit.run(new GameCommand.GetGame(tenantId, ALIAS, gameId, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class,
            f -> {
              assertThat(f.status()).isEqualTo(500);
              assertThat(f.message()).isEqualTo("Connection lost");
            });
  }

  @Test
  @DisplayName("ListGames parses optional system/campaign filters and handles empty filters")
  void onListGames_filtersAndEmpty_returnsItems() {
    var dao = mock(GameDAO.class);
    when(dao.listAll(tenantId, Optional.of(systemId), Optional.of(campaignId)))
        .thenReturn(ImmutableList.of(sampleGame(Optional.of(campaignId))));
    when(dao.listAll(tenantId, Optional.empty(), Optional.empty()))
        .thenReturn(ImmutableList.of(sampleGame(Optional.empty())));

    var kit = createKit(dao);
    TestInbox<GameActorResponse> inbox = TestInbox.create();

    kit.run(
        new GameCommand.ListGames(
            tenantId,
            ALIAS,
            Optional.of(studioLookup()),
            Optional.of(systemUri(studioId.toString())),
            Optional.of(campaignId.toString()),
            inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Items.class, i -> assertThat(i.games()).hasSize(1));

    kit.run(
        new GameCommand.ListGames(
            tenantId, ALIAS, Optional.empty(), Optional.empty(), inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Items.class,
            i -> assertThat(i.games().getFirst().campaign()).isEmpty());
  }

  @Test
  @DisplayName("ListGames rejects invalid filter URI with 400 and DAO errors with 500")
  void onListGames_invalidFilterAndError_returns400And500() {
    var dao = mock(GameDAO.class);
    var kit = createKit(dao);
    TestInbox<GameActorResponse> inbox = TestInbox.create();

    kit.run(
        new GameCommand.ListGames(
            tenantId,
            ALIAS,
            Optional.of(studioLookup()),
            Optional.of("/api/studios/other/v1/systems/" + systemId),
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(400));

    when(dao.listAll(eq(tenantId), any(), any())).thenThrow(new RuntimeException(""));
    kit.run(
        new GameCommand.ListGames(
            tenantId, ALIAS, Optional.empty(), Optional.empty(), inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class,
            f -> {
              assertThat(f.status()).isEqualTo(500);
              assertThat(f.message()).isEqualTo("Error executing list games");
            });
  }

  @Test
  @DisplayName("PatchGame sets clear flags for optional fields listed in update_mask")
  void onPatchGame_updateMaskClear_setsClearFlags() {
    var dao = mock(GameDAO.class);
    var paramsCaptor = ArgumentCaptor.forClass(PatchGameParams.class);
    when(dao.patch(eq(tenantId), eq(gameId), paramsCaptor.capture()))
        .thenReturn(Optional.of(sampleGame(Optional.empty())));

    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();
    var clearReq = new UpdateGameRequest("New Title", null, null, null, null, null, null);

    kit.run(
        new GameCommand.PatchGame(
            tenantId,
            ALIAS,
            Optional.of(studioLookup()),
            gameId,
            clearReq,
            Optional.of("title,campaign,summary,location_id,start_time,endTime"),
            inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(GameActorResponse.Success.class);
    var captured = paramsCaptor.getValue();
    assertThat(captured.title()).contains("New Title");
    assertThat(captured.clearCampaign()).isTrue();
    assertThat(captured.clearSummary()).isTrue();
    assertThat(captured.clearLocationId()).isTrue();
    assertThat(captured.clearStartTime()).isTrue();
    assertThat(captured.clearEndTime()).isTrue();
  }

  @Test
  @DisplayName("PatchGame parses and applies targeted system, campaign, location, and times")
  void onPatchGame_partialUpdate_updatesTargetedFields() {
    var dao = mock(GameDAO.class);
    var paramsCaptor = ArgumentCaptor.forClass(PatchGameParams.class);
    when(dao.patch(eq(tenantId), eq(gameId), paramsCaptor.capture()))
        .thenReturn(Optional.of(sampleGame(Optional.empty())));

    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();
    var setReq =
        new UpdateGameRequest(
            null, null, systemUri(ALIAS), campaignUri(ALIAS), locationId, START, END);

    kit.run(
        new GameCommand.PatchGame(
            tenantId,
            ALIAS,
            Optional.of(studioLookup()),
            gameId,
            setReq,
            Optional.empty(),
            inbox.getRef()));

    assertThat(inbox.receiveMessage()).isInstanceOf(GameActorResponse.Success.class);
    var updated = paramsCaptor.getValue();
    assertThat(updated.systemId()).contains(systemId);
    assertThat(updated.campaignId()).contains(campaignId);
    assertThat(updated.locationId()).contains(locationId);
    assertThat(updated.startTime()).contains(START);
    assertThat(updated.endTime()).contains(END);
  }

  @Test
  @DisplayName("PatchGame returns 400 on cleared required fields and cross-studio URIs")
  void onPatchGame_validationAndCrossStudio_returns400() {
    var dao = mock(GameDAO.class);
    var kit = createKit(dao);
    var inbox = TestInbox.<GameActorResponse>create();

    var emptyReq = new UpdateGameRequest();
    kit.run(
        new GameCommand.PatchGame(
            tenantId, ALIAS, gameId, emptyReq, Optional.of("system"), inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(400));

    var crossStudioReq =
        new UpdateGameRequest(
            null, null, "/api/studios/other/v1/systems/" + systemId, null, null, null, null);
    kit.run(
        new GameCommand.PatchGame(
            tenantId,
            ALIAS,
            Optional.of(studioLookup()),
            gameId,
            crossStudioReq,
            Optional.empty(),
            inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(400));
  }

  @Test
  @DisplayName("PatchGame returns 404 when missing and 500 on unexpected DAO error")
  void onPatchGame_missingAndException_returns404And500() {
    var dao = mock(GameDAO.class);
    var kit = createKit(dao);
    TestInbox<GameActorResponse> inbox = TestInbox.create();
    var titleReq = new UpdateGameRequest("Renamed", null, null, null, null, null, null);

    when(dao.patch(eq(tenantId), eq(gameId), any())).thenReturn(Optional.empty());
    kit.run(
        new GameCommand.PatchGame(
            tenantId, ALIAS, gameId, titleReq, Optional.empty(), inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(404));

    when(dao.patch(eq(tenantId), eq(gameId), any())).thenThrow(new RuntimeException("DB fail"));
    kit.run(
        new GameCommand.PatchGame(
            tenantId, ALIAS, gameId, titleReq, Optional.empty(), inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(500));
  }

  @Test
  @DisplayName("DeleteGame returns Deleted, 404 when not found, and 500 on error")
  void onDeleteGame_handlesSuccessNotFoundAndError() {
    var dao = mock(GameDAO.class);
    when(dao.softDelete(tenantId, gameId)).thenReturn(true).thenReturn(false);

    var kit = createKit(dao);
    TestInbox<GameActorResponse> inbox = TestInbox.create();

    kit.run(new GameCommand.DeleteGame(tenantId, gameId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(GameActorResponse.Deleted.class);

    kit.run(new GameCommand.DeleteGame(tenantId, gameId, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(404));

    when(dao.softDelete(tenantId, gameId)).thenThrow(new RuntimeException("Delete error"));
    kit.run(new GameCommand.DeleteGame(tenantId, gameId, inbox.getRef()));
    assertThat(inbox.receiveMessage())
        .isInstanceOfSatisfying(
            GameActorResponse.Failure.class, f -> assertThat(f.status()).isEqualTo(500));
  }
}
