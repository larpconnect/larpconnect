package com.larpconnect.njall.api.studios.games;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.domain.studios.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.pekko.actor.testkit.typed.javadsl.TestInbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameValidationAndDtoTest {

  private static final String VALID_ACTOR_URI =
      "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001";
  private static final String VALID_SYSTEM_URI =
      "/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002";
  private static final String VALID_CAMPAIGN_URI =
      "/api/studios/valiant/v1/campaigns/019273c5-9271-7000-8000-000000000003";
  private static final String VALID_LOCATION_ID = "019273c5-9271-7000-8000-000000000004";
  private static final UUID LOCATION_UUID = UUID.fromString(VALID_LOCATION_ID);
  private static final String START_ISO = "2026-06-01T18:00:00Z";
  private static final String END_ISO = "2026-06-01T22:00:00Z";
  private static final Instant START = Instant.parse(START_ISO);
  private static final Instant END = Instant.parse(END_ISO);

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .findAndAddModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  private static CreateGameRequest validCreateRequest() {
    return new CreateGameRequest(
        VALID_ACTOR_URI,
        VALID_SYSTEM_URI,
        VALID_CAMPAIGN_URI,
        "Chronicles of Dusk",
        "Opening arc",
        LOCATION_UUID,
        START,
        END);
  }

  private static UpdateGameRequest emptyUpdateRequest() {
    return new UpdateGameRequest();
  }

  @Test
  @DisplayName("tryParseUuid handles null, valid, blank, and malformed UUID inputs")
  void tryParseUuid_variousInputs_returnsExpected() {
    assertThat(GameValidation.tryParseUuid(null)).isEmpty();
    assertThat(GameValidation.tryParseUuid(VALID_LOCATION_ID)).contains(LOCATION_UUID);
    assertThat(GameValidation.tryParseUuid("")).isEmpty();
    assertThat(GameValidation.tryParseUuid("   ")).isEmpty();
    assertThat(GameValidation.tryParseUuid("not-a-uuid")).isEmpty();
  }

  @Test
  @DisplayName("validateCreate accepts full, minimal, and equal-timestamp one-shot payloads")
  void validateCreate_validPayloads_returnsEmpty() {
    assertThat(GameValidation.validateCreate(validCreateRequest())).isEmpty();

    var minimal = new CreateGameRequest(VALID_ACTOR_URI, VALID_SYSTEM_URI, "Minimal");
    assertThat(GameValidation.validateCreate(minimal)).isEmpty();

    var sameInstantOneShot =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, null, "One Shot", null, null, START, START);
    assertThat(GameValidation.validateCreate(sameInstantOneShot)).isEmpty();
  }

  @Test
  @DisplayName("validateCreate rejects missing or blank generator, system, title, or campaign")
  void validateCreate_missingOrBlankFields_returnsError() {
    var missingGenerator =
        new CreateGameRequest(
            null, VALID_SYSTEM_URI, (String) null, "Title", null, null, null, null);
    var blankGenerator = new CreateGameRequest("  ", VALID_SYSTEM_URI, "Title");
    var missingSystem =
        new CreateGameRequest(
            VALID_ACTOR_URI, null, (String) null, "Title", null, null, null, null);
    var blankSystem = new CreateGameRequest(VALID_ACTOR_URI, " ", "Title");
    var missingTitle =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, (String) null, null, null, null, null, null);
    var blankTitle = new CreateGameRequest(VALID_ACTOR_URI, VALID_SYSTEM_URI, "   ");
    var blankCampaign =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, "  ", "Title", null, null, null, null);

    assertThat(GameValidation.validateCreate(missingGenerator)).isPresent();
    assertThat(GameValidation.validateCreate(blankGenerator)).isPresent();
    assertThat(GameValidation.validateCreate(missingSystem)).isPresent();
    assertThat(GameValidation.validateCreate(blankSystem)).isPresent();
    assertThat(GameValidation.validateCreate(missingTitle)).isPresent();
    assertThat(GameValidation.validateCreate(blankTitle)).isPresent();
    assertThat(GameValidation.validateCreate(blankCampaign)).isPresent();
  }

  @Test
  @DisplayName("validateCreate rejects inverted range and allows single-sided timestamps")
  void validateCreate_timeRangeRules_returnsExpected() {
    var invertedRange =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, null, "Title", null, null, END, START);
    var startOnly =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, null, "Title", null, null, START, null);
    var endOnly =
        new CreateGameRequest(
            VALID_ACTOR_URI, VALID_SYSTEM_URI, null, "Title", null, null, null, END);

    assertThat(GameValidation.validateCreate(invertedRange))
        .contains("endTime must not be before startTime");
    assertThat(GameValidation.validateCreate(startOnly)).isEmpty();
    assertThat(GameValidation.validateCreate(endOnly)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch rejects clearing title or system under update_mask")
  void validatePatch_clearingRequiredFields_returnsError() {
    var maskTitle = Optional.of(ImmutableSet.of("title"));
    var maskSystem = Optional.of(ImmutableSet.of("system"));

    assertThat(GameValidation.validatePatch(emptyUpdateRequest(), maskTitle))
        .contains("Game title must not be cleared");
    assertThat(GameValidation.validatePatch(emptyUpdateRequest(), maskSystem))
        .contains("Game system must not be cleared");
  }

  @Test
  @DisplayName("validatePatch rejects blank targeted fields and inverted time range")
  void validatePatch_invalidTargetedFields_returnsError() {
    var blankTitle = new UpdateGameRequest(" ", null, null, null, null, null, null);
    var blankSystem = new UpdateGameRequest(null, null, " ", null, null, null, null);
    var blankCampaign = new UpdateGameRequest(null, null, null, "  ", null, null, null);
    var inverted = new UpdateGameRequest(null, null, null, null, null, END, START);

    assertThat(GameValidation.validatePatch(blankTitle, Optional.empty())).isPresent();
    assertThat(GameValidation.validatePatch(blankSystem, Optional.empty())).isPresent();
    assertThat(GameValidation.validatePatch(blankCampaign, Optional.empty())).isPresent();
    assertThat(GameValidation.validatePatch(inverted, Optional.empty())).isPresent();
  }

  @Test
  @DisplayName("validatePatch ignores untargeted fields and allows clearing optional fields")
  void validatePatch_untargetedAndClearOptional_returnsEmpty() {
    var mixed = new UpdateGameRequest("Updated Title", null, " ", "   ", LOCATION_UUID, END, START);
    var titleMask = Optional.of(ImmutableSet.of("title"));
    var clearOptionalMask =
        Optional.of(ImmutableSet.of("campaign", "summary", "location_id", "start_time", "endTime"));

    assertThat(GameValidation.validatePatch(mixed, titleMask)).isEmpty();
    assertThat(GameValidation.validatePatch(emptyUpdateRequest(), clearOptionalMask)).isEmpty();
  }

  @Test
  @DisplayName("Request DTOs deserialize JSON properties, normalize nulls, and reject bad formats")
  void requestDtos_jsonDeserialization_normalizesFields() throws Exception {
    var createReq =
        objectMapper.readValue(
            "{\"generator\":\""
                + VALID_ACTOR_URI
                + "\",\"system\":\""
                + VALID_SYSTEM_URI
                + "\",\"title\":\"Minimal\",\"campaign\":null}",
            CreateGameRequest.class);
    assertThat(createReq.generator()).isEqualTo(VALID_ACTOR_URI);
    assertThat(createReq.system()).isEqualTo(VALID_SYSTEM_URI);
    assertThat(createReq.title()).isEqualTo("Minimal");
    assertThat(createReq.campaign()).isEmpty();
    assertThat(createReq.locationId()).isEmpty();

    var patchReq =
        objectMapper.readValue(
            "{\"title\":\"Renamed\",\"summary\":null,\"locationId\":\""
                + VALID_LOCATION_ID
                + "\",\"startTime\":\""
                + START_ISO
                + "\",\"endTime\":\""
                + END_ISO
                + "\"}",
            UpdateGameRequest.class);
    assertThat(patchReq.title()).contains("Renamed");
    assertThat(patchReq.summary()).isEmpty();
    assertThat(patchReq.locationId()).contains(LOCATION_UUID);
    assertThat(patchReq.startTime()).contains(START);
    assertThat(patchReq.endTime()).contains(END);

    assertThatThrownBy(
            () ->
                objectMapper.readValue("{\"locationId\":\"not-a-uuid\"}", UpdateGameRequest.class))
        .isInstanceOf(JsonProcessingException.class);
    assertThatThrownBy(
            () -> objectMapper.readValue("{\"startTime\":\"not-an-iso\"}", UpdateGameRequest.class))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  @DisplayName("GameResponse serializes required fields, eventType GAME, and explicit JSON nulls")
  void gameResponse_jsonSerialization_preservesFieldsAndNulls() throws Exception {
    var id = UUID.randomUUID();
    var response =
        new GameResponse(
            id,
            VALID_ACTOR_URI,
            VALID_SYSTEM_URI,
            Optional.empty(),
            EventType.GAME,
            "Minimal",
            Optional.of("Short"),
            Optional.of(LOCATION_UUID),
            Optional.of(START),
            Optional.empty(),
            START,
            START);

    var node = objectMapper.readTree(objectMapper.writeValueAsString(response));
    assertThat(node.get("id").asText()).isEqualTo(id.toString());
    assertThat(node.get("eventType").asText()).isEqualTo("GAME");
    assertThat(node.get("system").asText()).isEqualTo(VALID_SYSTEM_URI);
    assertThat(node.get("campaign").isNull()).isTrue();
    assertThat(node.get("locationId").asText()).isEqualTo(VALID_LOCATION_ID);
    assertThat(node.get("startTime").asText()).isEqualTo(START_ISO);
    assertThat(node.get("endTime").isNull()).isTrue();
  }

  @Test
  @DisplayName("Command convenience constructors and response factories populate fields")
  void protocolHelpers_constructExpectedRecords() {
    var tenantId = UUID.randomUUID();
    var gameId = UUID.randomUUID();
    var inbox = TestInbox.<GameActorResponse>create();

    var createCmd =
        new GameCommand.CreateGame(tenantId, "valiant", validCreateRequest(), inbox.getRef());
    var listCmd =
        new GameCommand.ListGames(
            tenantId, "valiant", Optional.empty(), Optional.empty(), inbox.getRef());
    var patchCmd =
        new GameCommand.PatchGame(
            tenantId,
            "valiant",
            gameId,
            emptyUpdateRequest(),
            Optional.of("summary"),
            inbox.getRef());

    assertThat(createCmd.lookup()).isEmpty();
    assertThat(listCmd.lookup()).isEmpty();
    assertThat(patchCmd.lookup()).isEmpty();

    assertThat(GameActorResponse.deleted()).isInstanceOf(GameActorResponse.Deleted.class);
    assertThat(GameActorResponse.items(ImmutableList.of()))
        .isInstanceOf(GameActorResponse.Items.class);
    assertThat(GameActorResponse.notFound("missing"))
        .isEqualTo(new GameActorResponse.Failure(404, "missing"));
    assertThat(GameActorResponse.badRequest("bad"))
        .isEqualTo(new GameActorResponse.Failure(400, "bad"));
    assertThat(GameActorResponse.failure("err"))
        .isEqualTo(new GameActorResponse.Failure(500, "err"));
  }
}
