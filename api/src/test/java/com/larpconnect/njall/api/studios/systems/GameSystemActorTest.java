package com.larpconnect.njall.api.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.larpconnect.njall.data.dao.studios.systems.GameSystemDAO;
import com.larpconnect.njall.data.dao.studios.systems.PatchGameSystemParams;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import com.larpconnect.njall.data.domain.studios.GameSystem;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.BehaviorTestKit;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.apache.pekko.actor.typed.Behavior;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameSystemActorTest {

  private static final String STUDIO_ALIAS = "valiant";

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final Instant now = Instant.now();
  private final StudioLookup lookup =
      new StudioLookup(tenantId, studioId, STUDIO_ALIAS, now, now, Optional.empty());

  private Behavior<GameSystemCommand> createBehavior(GameSystemDAO dao) {
    return new DefaultGameSystemActorFactory(dao).create();
  }

  private GameSystem sampleSystem(String name, Optional<String> summary) {
    return new GameSystem(systemId, actorId, name, summary, now, now, Optional.empty());
  }

  @Test
  @DisplayName("onCreateGameSystem creates system with canonical URI and returns Success")
  void onCreateGameSystem_validUriAndLookup_returnsSuccess() {
    var dao = mock(GameSystemDAO.class);
    var domain = sampleSystem("Daggerheart", Optional.of("Narrative fantasy RPG"));
    when(dao.create(tenantId, actorId, "Daggerheart", Optional.of("Narrative fantasy RPG")))
        .thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();
    var request =
        new CreateGameSystemRequest(
            "/api/studios/valiant/v1/actors/" + actorId, "Daggerheart", "Narrative fantasy RPG");
    testKit.run(
        new GameSystemCommand.CreateGameSystem(
            tenantId, studioId.toString(), request, inbox.getRef(), Optional.of(lookup)));

    var response = (GameSystemActorResponse.Success) inbox.receiveMessage();
    assertThat(response.system().id()).isEqualTo(systemId);
    assertThat(response.system().generator())
        .isEqualTo("/api/studios/" + studioId + "/v1/actors/" + actorId);
    assertThat(response.system().name()).isEqualTo("Daggerheart");
    assertThat(response.system().summary()).contains("Narrative fantasy RPG");
  }

  @Test
  @DisplayName("onCreateGameSystem accepts bare actor UUID without summary or lookup")
  void onCreateGameSystem_bareUuidNoSummary_returnsSuccess() {
    var dao = mock(GameSystemDAO.class);
    var domain = sampleSystem("Minimal", Optional.empty());
    when(dao.create(tenantId, actorId, "Minimal", Optional.empty())).thenReturn(domain);

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();
    var request = new CreateGameSystemRequest(actorId.toString(), "Minimal");
    testKit.run(
        new GameSystemCommand.CreateGameSystem(tenantId, STUDIO_ALIAS, request, inbox.getRef()));

    var response = (GameSystemActorResponse.Success) inbox.receiveMessage();
    assertThat(response.system().summary()).isEmpty();
    assertThat(response.system().generator())
        .isEqualTo("/api/studios/valiant/v1/actors/" + actorId);
  }

  @Test
  @DisplayName("onCreateGameSystem returns 400 on invalid payload or generator URI")
  void onCreateGameSystem_invalidInput_returnsBadRequest() {
    var dao = mock(GameSystemDAO.class);
    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(
        new GameSystemCommand.CreateGameSystem(
            tenantId,
            STUDIO_ALIAS,
            new CreateGameSystemRequest(actorId.toString(), "   "),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new GameSystemCommand.CreateGameSystem(
            tenantId,
            STUDIO_ALIAS,
            new CreateGameSystemRequest("/api/studios/valiant/v1/locations/" + actorId, "D20"),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new GameSystemCommand.CreateGameSystem(
            tenantId,
            STUDIO_ALIAS,
            new CreateGameSystemRequest("/api/studios/other/v1/actors/" + actorId, "D20"),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
    verifyNoInteractions(dao);
  }

  @Test
  @DisplayName("onCreateGameSystem maps IAE to 400 and unexpected errors to 500")
  void onCreateGameSystem_daoValidationAndFailure_maps400And500() {
    var dao = mock(GameSystemDAO.class);
    when(dao.create(any(), any(), eq("Bad"), any()))
        .thenThrow(new IllegalArgumentException("Generator is not an active actor"));
    when(dao.create(any(), any(), eq("NullIae"), any())).thenThrow(new IllegalArgumentException());
    when(dao.create(any(), any(), eq("Fail"), any())).thenThrow(new RuntimeException("DB offline"));
    when(dao.create(any(), any(), eq("NullFail"), any())).thenThrow(new RuntimeException());

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();
    for (var name : ImmutableList.of("Bad", "NullIae", "Fail", "NullFail")) {
      testKit.run(
          new GameSystemCommand.CreateGameSystem(
              tenantId,
              STUDIO_ALIAS,
              new CreateGameSystemRequest(actorId.toString(), name),
              inbox.getRef()));
    }

    assertThat(inbox.receiveMessage())
        .isEqualTo(new GameSystemActorResponse.Failure(400, "Generator is not an active actor"));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);
    assertThat(inbox.receiveMessage())
        .isEqualTo(new GameSystemActorResponse.Failure(500, "DB offline"));
    var nullFail = (GameSystemActorResponse.Failure) inbox.receiveMessage();
    assertThat(nullFail.status()).isEqualTo(500);
    assertThat(nullFail.message()).isNotBlank();
  }

  @Test
  @DisplayName("onCreateGameSystem maps IllegalStateException and CVE variants to 409")
  void onCreateGameSystem_daoConflicts_mapsIllegalStateAndCveTo409() {
    var dao = mock(GameSystemDAO.class);
    var directCve =
        new ConstraintViolationException(
            "Duplicate system", new SQLException("duplicate key", "23505"), "idx_systems_name");
    var wrappedCve =
        new RuntimeException(
            "Outer failure",
            new ConstraintViolationException(
                "duplicate", new SQLException("dup"), "idx_systems_name"));
    when(dao.create(any(), any(), eq("StateConflict"), any()))
        .thenThrow(new IllegalStateException("System name already exists"));
    when(dao.create(any(), any(), eq("NullState"), any())).thenThrow(new IllegalStateException());
    when(dao.create(any(), any(), eq("DirectCve"), any())).thenThrow(directCve);
    when(dao.create(any(), any(), eq("WrappedCve"), any())).thenThrow(wrappedCve);

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();
    for (var name : ImmutableList.of("StateConflict", "NullState", "DirectCve", "WrappedCve")) {
      testKit.run(
          new GameSystemCommand.CreateGameSystem(
              tenantId,
              STUDIO_ALIAS,
              new CreateGameSystemRequest(actorId.toString(), name),
              inbox.getRef()));
    }

    assertThat(inbox.receiveMessage())
        .isEqualTo(new GameSystemActorResponse.Failure(409, "System name already exists"));
    var nullState = (GameSystemActorResponse.Failure) inbox.receiveMessage();
    assertThat(nullState.status()).isEqualTo(409);
    assertThat(nullState.message()).isNotBlank();
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(409);
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(409);
  }

  @Test
  @DisplayName("onGetGameSystem handles found, missing, and error")
  void onGetGameSystem_handlesFoundMissingAndError() {
    var dao = mock(GameSystemDAO.class);
    var missingId = UUID.randomUUID();
    var errorId = UUID.randomUUID();
    when(dao.findById(tenantId, systemId))
        .thenReturn(Optional.of(sampleSystem("Daggerheart", Optional.of("Synopsis"))));
    when(dao.findById(tenantId, missingId)).thenReturn(Optional.empty());
    when(dao.findById(tenantId, errorId)).thenThrow(new RuntimeException("Query error"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(
        new GameSystemCommand.GetGameSystem(tenantId, STUDIO_ALIAS, systemId, inbox.getRef()));
    var found = (GameSystemActorResponse.Success) inbox.receiveMessage();
    assertThat(found.system().name()).isEqualTo("Daggerheart");
    assertThat(found.system().summary()).contains("Synopsis");

    testKit.run(
        new GameSystemCommand.GetGameSystem(tenantId, STUDIO_ALIAS, missingId, inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(
        new GameSystemCommand.GetGameSystem(tenantId, STUDIO_ALIAS, errorId, inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onListGameSystems returns items and handles error")
  void onListGameSystems_handlesSuccessAndError() {
    var dao = mock(GameSystemDAO.class);
    var first = sampleSystem("Daggerheart", Optional.empty());
    when(dao.listAll(tenantId))
        .thenReturn(ImmutableList.of(first))
        .thenThrow(new RuntimeException("List failed"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(new GameSystemCommand.ListGameSystems(tenantId, STUDIO_ALIAS, inbox.getRef()));
    var items = (GameSystemActorResponse.Items) inbox.receiveMessage();
    assertThat(items.systems()).hasSize(1);
    assertThat(items.systems().getFirst().name()).isEqualTo("Daggerheart");

    testKit.run(new GameSystemCommand.ListGameSystems(tenantId, STUDIO_ALIAS, inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onPatchGameSystem clears summary with update_mask and preserves it without mask")
  void onPatchGameSystem_handlesMaskAndClearing() {
    var dao = mock(GameSystemDAO.class);
    var cleared = sampleSystem("Daggerheart", Optional.empty());
    var preserved = sampleSystem("Daggerheart 2e", Optional.of("Existing"));
    when(dao.patch(
            tenantId,
            systemId,
            new PatchGameSystemParams(Optional.empty(), Optional.empty(), true)))
        .thenReturn(Optional.of(cleared));
    when(dao.patch(
            tenantId,
            systemId,
            new PatchGameSystemParams(Optional.of("Daggerheart 2e"), Optional.empty(), false)))
        .thenReturn(Optional.of(preserved));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(
        new GameSystemCommand.PatchGameSystem(
            tenantId,
            STUDIO_ALIAS,
            systemId,
            new UpdateGameSystemRequest(),
            Optional.of("summary"),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Success) inbox.receiveMessage()).system().summary())
        .isEmpty();

    testKit.run(
        new GameSystemCommand.PatchGameSystem(
            tenantId,
            STUDIO_ALIAS,
            systemId,
            new UpdateGameSystemRequest("Daggerheart 2e", null),
            Optional.empty(),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Success) inbox.receiveMessage()).system().summary())
        .contains("Existing");
    verify(dao)
        .patch(
            tenantId,
            systemId,
            new PatchGameSystemParams(Optional.empty(), Optional.empty(), true));
  }

  @Test
  @DisplayName("onPatchGameSystem rejects clearing name and returns 404 when missing")
  void onPatchGameSystem_invalidMaskOrMissing_returns400And404() {
    var dao = mock(GameSystemDAO.class);
    var missingId = UUID.randomUUID();
    when(dao.patch(eq(tenantId), eq(missingId), any())).thenReturn(Optional.empty());

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(
        new GameSystemCommand.PatchGameSystem(
            tenantId,
            STUDIO_ALIAS,
            systemId,
            new UpdateGameSystemRequest(),
            Optional.of("name"),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(400);

    testKit.run(
        new GameSystemCommand.PatchGameSystem(
            tenantId,
            STUDIO_ALIAS,
            missingId,
            new UpdateGameSystemRequest(),
            Optional.empty(),
            inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);
  }

  @Test
  @DisplayName("onPatchGameSystem maps IllegalState, ConstraintViolation, and errors to 409/500")
  void onPatchGameSystem_conflictsAndErrors_maps409And500() {
    var dao = mock(GameSystemDAO.class);
    var conflictId = UUID.randomUUID();
    var cveId = UUID.randomUUID();
    var errorId = UUID.randomUUID();
    var wrappedCve =
        new RuntimeException(
            "Wrap", new ConstraintViolationException("Dup", new SQLException("dup"), "idx"));
    when(dao.patch(eq(tenantId), eq(conflictId), any()))
        .thenThrow(new IllegalStateException("Name conflict"));
    when(dao.patch(eq(tenantId), eq(cveId), any())).thenThrow(wrappedCve);
    when(dao.patch(eq(tenantId), eq(errorId), any()))
        .thenThrow(new RuntimeException("Update fail"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();
    for (var id : ImmutableList.of(conflictId, cveId, errorId)) {
      testKit.run(
          new GameSystemCommand.PatchGameSystem(
              tenantId,
              STUDIO_ALIAS,
              id,
              new UpdateGameSystemRequest("Dup", null),
              Optional.of("name"),
              inbox.getRef()));
    }

    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(409);
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(409);
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }

  @Test
  @DisplayName("onDeleteGameSystem handles deleted, missing, and error")
  void onDeleteGameSystem_handlesDeletedMissingAndError() {
    var dao = mock(GameSystemDAO.class);
    var missingId = UUID.randomUUID();
    var errorId = UUID.randomUUID();
    when(dao.softDelete(tenantId, systemId)).thenReturn(true);
    when(dao.softDelete(tenantId, missingId)).thenReturn(false);
    when(dao.softDelete(tenantId, errorId)).thenThrow(new RuntimeException("Delete error"));

    var testKit = BehaviorTestKit.create(createBehavior(dao));
    TestInbox<GameSystemActorResponse> inbox = TestInbox.create();

    testKit.run(new GameSystemCommand.DeleteGameSystem(tenantId, systemId, inbox.getRef()));
    assertThat(inbox.receiveMessage()).isInstanceOf(GameSystemActorResponse.Deleted.class);

    testKit.run(new GameSystemCommand.DeleteGameSystem(tenantId, missingId, inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(404);

    testKit.run(new GameSystemCommand.DeleteGameSystem(tenantId, errorId, inbox.getRef()));
    assertThat(((GameSystemActorResponse.Failure) inbox.receiveMessage()).status()).isEqualTo(500);
  }
}
