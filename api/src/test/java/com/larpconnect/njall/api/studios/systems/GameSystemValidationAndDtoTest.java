package com.larpconnect.njall.api.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameSystemValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .findAndAddModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  @Test
  @DisplayName("validateCreate rejects blank or empty generator")
  void validateCreate_blankGenerator_returnsError() {
    assertThat(GameSystemValidation.validateCreate(new CreateGameSystemRequest("", "D20")))
        .contains("generator is required");
    assertThat(GameSystemValidation.validateCreate(new CreateGameSystemRequest("   ", "D20")))
        .contains("generator is required");
  }

  @Test
  @DisplayName("validateCreate rejects blank or empty name")
  void validateCreate_blankName_returnsError() {
    var gen = UUID.randomUUID().toString();
    assertThat(GameSystemValidation.validateCreate(new CreateGameSystemRequest(gen, "")))
        .contains("name is required");
    assertThat(GameSystemValidation.validateCreate(new CreateGameSystemRequest(gen, "   ")))
        .contains("name is required");
  }

  @Test
  @DisplayName("validateCreate accepts valid payload")
  void validateCreate_valid_returnsEmpty() {
    var request =
        new CreateGameSystemRequest(
            UUID.randomUUID().toString(), "Daggerheart", "Dual-d12 narrative tabletop system");
    assertThat(GameSystemValidation.validateCreate(request)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch rejects clearing required name via update_mask")
  void validatePatch_clearedName_returnsError() {
    var mask = Optional.of(ImmutableSet.of("name"));
    var request = new UpdateGameSystemRequest();
    assertThat(GameSystemValidation.validatePatch(request, mask))
        .contains("name cannot be cleared");
  }

  @Test
  @DisplayName("validatePatch rejects blank name when targeted")
  void validatePatch_targetedBlankName_returnsError() {
    var mask = Optional.of(ImmutableSet.of("name"));
    var request = new UpdateGameSystemRequest("   ", null);
    assertThat(GameSystemValidation.validatePatch(request, mask))
        .contains("name must not be blank");
    assertThat(GameSystemValidation.validatePatch(request, Optional.empty()))
        .contains("name must not be blank");
  }

  @Test
  @DisplayName("validatePatch ignores blank name when omitted from update_mask")
  void validatePatch_unmaskedBlankName_returnsEmpty() {
    var mask = Optional.of(ImmutableSet.of("summary"));
    var request = new UpdateGameSystemRequest("   ", "Updated summary");
    assertThat(GameSystemValidation.validatePatch(request, mask)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch accepts valid name and summary update")
  void validatePatch_valid_returnsEmpty() {
    var mask = Optional.of(ImmutableSet.of("name"));
    var validRequest = new UpdateGameSystemRequest("Daggerheart 1.0", null);
    assertThat(GameSystemValidation.validatePatch(validRequest, mask)).isEmpty();
    assertThat(GameSystemValidation.validatePatch(new UpdateGameSystemRequest(), Optional.empty()))
        .isEmpty();
  }

  @Test
  @DisplayName("tryParseUuid parses valid UUIDs and returns empty otherwise")
  void tryParseUuid_handlesValidAndInvalid() {
    assertThat(GameSystemValidation.tryParseUuid(null)).isEmpty();
    assertThat(GameSystemValidation.tryParseUuid("")).isEmpty();
    assertThat(GameSystemValidation.tryParseUuid("not-a-uuid")).isEmpty();

    var id = UUID.randomUUID();
    assertThat(GameSystemValidation.tryParseUuid(id.toString())).contains(id);
  }

  @Test
  @DisplayName("CreateGameSystemRequest deserializes JSON and normalizes null fields")
  void createGameSystemRequest_deserializes() throws Exception {
    var json =
        """
        {
          "generator": "019273c5-9271-7000-8000-000000000001",
          "name": "Daggerheart",
          "summary": "Fantasy narrative RPG"
        }
        """;
    var req = objectMapper.readValue(json, CreateGameSystemRequest.class);
    assertThat(req.generator()).isEqualTo("019273c5-9271-7000-8000-000000000001");
    assertThat(req.name()).isEqualTo("Daggerheart");
    assertThat(req.summary()).contains("Fantasy narrative RPG");

    var missingFields = objectMapper.readValue("{}", CreateGameSystemRequest.class);
    assertThat(missingFields.generator()).isEmpty();
    assertThat(missingFields.name()).isEmpty();
    assertThat(missingFields.summary()).isEmpty();
  }

  @Test
  @DisplayName("UpdateGameSystemRequest deserializes nullable fields cleanly")
  void updateGameSystemRequest_deserializes() throws Exception {
    var req =
        objectMapper.readValue(
            "{\"name\":\"Updated Name\",\"summary\":null}", UpdateGameSystemRequest.class);
    assertThat(req.name()).contains("Updated Name");
    assertThat(req.summary()).isEmpty();
  }

  @Test
  @DisplayName("GameSystemResponse serializes absent summary as explicit JSON null")
  void gameSystemResponse_absentSummary_serializesAsNull() throws Exception {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-09-29T12:00:00Z");
    var response =
        new GameSystemResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Minimal System",
            Optional.empty(),
            now,
            now);

    var json = objectMapper.writeValueAsString(response);
    var tree = objectMapper.readTree(json);
    assertThat(tree.get("id").asText()).isEqualTo(id.toString());
    assertThat(tree.get("name").asText()).isEqualTo("Minimal System");
    assertThat(tree.has("summary")).isTrue();
    assertThat(tree.get("summary").isNull()).isTrue();
    assertThat(tree.has("tenantId")).isFalse();
  }

  @Test
  @DisplayName("GameSystemResponse serializes present summary and canonical generator URI")
  void gameSystemResponse_presentSummary_serializesFields() throws Exception {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-09-29T12:00:00Z");
    var response =
        new GameSystemResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Vampire 5e",
            Optional.of("Personal horror system"),
            now,
            now);

    var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
    assertThat(tree.get("summary").asText()).isEqualTo("Personal horror system");
    assertThat(tree.get("generator").asText())
        .isEqualTo("/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001");
  }

  @Test
  @DisplayName("GameSystemActorResponse factory helpers create expected statuses")
  void actorResponseHelpers_createExpectedVariants() {
    var id = UUID.randomUUID();
    var now = Instant.now();
    var dto = new GameSystemResponse(id, "gen", "D20", Optional.empty(), now, now);

    assertThat(GameSystemActorResponse.success(dto))
        .isEqualTo(new GameSystemActorResponse.Success(dto));
    assertThat(GameSystemActorResponse.items(ImmutableList.of(dto)))
        .isEqualTo(new GameSystemActorResponse.Items(ImmutableList.of(dto)));
    assertThat(GameSystemActorResponse.deleted()).isEqualTo(new GameSystemActorResponse.Deleted());
    assertThat(GameSystemActorResponse.notFound("missing"))
        .isEqualTo(new GameSystemActorResponse.Failure(404, "missing"));
    assertThat(GameSystemActorResponse.badRequest("bad"))
        .isEqualTo(new GameSystemActorResponse.Failure(400, "bad"));
    assertThat(GameSystemActorResponse.conflict("dup"))
        .isEqualTo(new GameSystemActorResponse.Failure(409, "dup"));
    assertThat(GameSystemActorResponse.failure("err"))
        .isEqualTo(new GameSystemActorResponse.Failure(500, "err"));
  }
}
