package com.larpconnect.njall.api.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CampaignValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .findAndAddModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();

  @Test
  @DisplayName("validateCreate rejects blank or empty generator")
  void validateCreate_blankGenerator_returnsError() {
    assertThat(CampaignValidation.validateCreate(new CreateCampaignRequest("", "Title")))
        .contains("Generator URI or actor identifier must not be blank");
    assertThat(CampaignValidation.validateCreate(new CreateCampaignRequest("   ", "Title")))
        .contains("Generator URI or actor identifier must not be blank");
  }

  @Test
  @DisplayName("validateCreate rejects blank or empty title")
  void validateCreate_blankTitle_returnsError() {
    var gen = UUID.randomUUID().toString();
    assertThat(CampaignValidation.validateCreate(new CreateCampaignRequest(gen, "")))
        .contains("Campaign title must not be blank");
    assertThat(CampaignValidation.validateCreate(new CreateCampaignRequest(gen, "   ")))
        .contains("Campaign title must not be blank");
  }

  @Test
  @DisplayName("validateCreate accepts valid payload")
  void validateCreate_valid_returnsEmpty() {
    var request =
        new CreateCampaignRequest(
            UUID.randomUUID().toString(),
            "Seattle By Night",
            "Chronicle",
            UUID.randomUUID().toString());
    assertThat(CampaignValidation.validateCreate(request)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch rejects clearing required title via update_mask")
  void validatePatch_clearedTitle_returnsError() {
    var mask = Optional.of(ImmutableSet.of("title"));
    var request = new UpdateCampaignRequest();
    assertThat(CampaignValidation.validatePatch(request, mask))
        .contains("Campaign title cannot be cleared");
  }

  @Test
  @DisplayName("validatePatch rejects blank title when targeted")
  void validatePatch_targetedBlankTitle_returnsError() {
    var mask = Optional.of(ImmutableSet.of("title"));
    var request = new UpdateCampaignRequest("   ", null, null);
    assertThat(CampaignValidation.validatePatch(request, mask))
        .contains("Campaign title must not be blank");
    assertThat(CampaignValidation.validatePatch(request, Optional.empty()))
        .contains("Campaign title must not be blank");
  }

  @Test
  @DisplayName("validatePatch ignores blank title when omitted from update_mask")
  void validatePatch_unmaskedBlankTitle_returnsEmpty() {
    var mask = Optional.of(ImmutableSet.of("summary"));
    var request = new UpdateCampaignRequest("   ", "Updated summary", null);
    assertThat(CampaignValidation.validatePatch(request, mask)).isEmpty();
  }

  @Test
  @DisplayName("validatePatch accepts valid title and empty patch")
  void validatePatch_valid_returnsEmpty() {
    var mask = Optional.of(ImmutableSet.of("title"));
    var validRequest = new UpdateCampaignRequest("Season Two", null, null);
    assertThat(CampaignValidation.validatePatch(validRequest, mask)).isEmpty();
    assertThat(CampaignValidation.validatePatch(new UpdateCampaignRequest(), Optional.empty()))
        .isEmpty();
  }

  @Test
  @DisplayName("tryParseUuid parses valid UUIDs and returns empty otherwise")
  void tryParseUuid_handlesValidAndInvalid() {
    assertThat(CampaignValidation.tryParseUuid(null)).isEmpty();
    assertThat(CampaignValidation.tryParseUuid("")).isEmpty();
    assertThat(CampaignValidation.tryParseUuid("not-a-uuid")).isEmpty();

    var id = UUID.randomUUID();
    assertThat(CampaignValidation.tryParseUuid(id.toString())).contains(id);
  }

  @Test
  @DisplayName("CreateCampaignRequest deserializes JSON and normalizes null fields")
  void createCampaignRequest_deserializes() throws Exception {
    var json =
        """
        {
          "generator": "019273c5-9271-7000-8000-000000000001",
          "title": "Seattle By Night",
          "summary": "Chronicle across the Pacific Northwest",
          "system": "/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002"
        }
        """;
    var req = objectMapper.readValue(json, CreateCampaignRequest.class);
    assertThat(req.generator()).isEqualTo("019273c5-9271-7000-8000-000000000001");
    assertThat(req.title()).isEqualTo("Seattle By Night");
    assertThat(req.summary()).contains("Chronicle across the Pacific Northwest");
    assertThat(req.system())
        .contains("/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002");

    var missingFields = objectMapper.readValue("{}", CreateCampaignRequest.class);
    assertThat(missingFields.generator()).isEmpty();
    assertThat(missingFields.title()).isEmpty();
    assertThat(missingFields.summary()).isEmpty();
    assertThat(missingFields.system()).isEmpty();
  }

  @Test
  @DisplayName("UpdateCampaignRequest deserializes nullable fields cleanly")
  void updateCampaignRequest_deserializes() throws Exception {
    var req =
        objectMapper.readValue(
            "{\"title\":\"Season Two\",\"system\":null}", UpdateCampaignRequest.class);
    assertThat(req.title()).contains("Season Two");
    assertThat(req.summary()).isEmpty();
    assertThat(req.system()).isEmpty();
  }

  @Test
  @DisplayName("CampaignResponse serializes absent optional fields as explicit JSON nulls")
  void campaignResponse_absentOptionalFields_serializesAsNull() throws Exception {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-09-29T12:00:00Z");
    var response =
        new CampaignResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Freeform Anthology",
            Optional.empty(),
            Optional.empty(),
            now,
            now);

    var json = objectMapper.writeValueAsString(response);
    var tree = objectMapper.readTree(json);
    assertThat(tree.get("id").asText()).isEqualTo(id.toString());
    assertThat(tree.get("title").asText()).isEqualTo("Freeform Anthology");
    assertThat(tree.has("summary")).isTrue();
    assertThat(tree.get("summary").isNull()).isTrue();
    assertThat(tree.has("system")).isTrue();
    assertThat(tree.get("system").isNull()).isTrue();
    assertThat(tree.has("tenantId")).isFalse();
  }

  @Test
  @DisplayName("CampaignResponse serializes present summary and canonical system URI")
  void campaignResponse_presentFields_serializesCanonicalUris() throws Exception {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-09-29T12:00:00Z");
    var response =
        new CampaignResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Seattle By Night",
            Optional.of("Camarilla chronicle"),
            Optional.of("/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002"),
            now,
            now);

    var tree = objectMapper.readTree(objectMapper.writeValueAsString(response));
    assertThat(tree.get("summary").asText()).isEqualTo("Camarilla chronicle");
    assertThat(tree.get("system").asText())
        .isEqualTo("/api/studios/valiant/v1/systems/019273c5-9271-7000-8000-000000000002");
  }
}
