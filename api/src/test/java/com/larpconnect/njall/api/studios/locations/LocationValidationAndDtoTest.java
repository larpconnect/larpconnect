package com.larpconnect.njall.api.studios.locations;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LocationValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("LocationValidation handles create and patch rules")
  void locationValidation_rules() {
    var blankGen = new CreateLocationRequest("", "Camp Whispering Pines");
    assertThat(LocationValidation.validateCreate(blankGen))
        .contains("Generator URI or actor identifier must not be blank");

    var blankName = new CreateLocationRequest("019273c5-9271-7000-8000-000000000001", "");
    assertThat(LocationValidation.validateCreate(blankName))
        .contains("Location name must not be blank");

    var longName =
        new CreateLocationRequest("019273c5-9271-7000-8000-000000000001", "a".repeat(256));
    assertThat(LocationValidation.validateCreate(longName))
        .contains("Location name must not exceed 255 characters");

    var validCreate =
        new CreateLocationRequest(
            "019273c5-9271-7000-8000-000000000001", "Camp Whispering Pines", "Main camp");
    assertThat(LocationValidation.validateCreate(validCreate)).isEmpty();

    var blankPatch = new UpdateLocationRequest("   ", null);
    assertThat(LocationValidation.validatePatch(blankPatch))
        .contains("Location name must not be blank");

    var validPatch = new UpdateLocationRequest("Camp Whispering Pines - North", null);
    assertThat(LocationValidation.validatePatch(validPatch)).isEmpty();
  }

  @Test
  @DisplayName("LocationValidation checks patch name length and tryParseUuid")
  void locationValidation_boundaries() {
    var longPatch = new UpdateLocationRequest("b".repeat(256), null);
    assertThat(LocationValidation.validatePatch(longPatch))
        .contains("Location name must not exceed 255 characters");

    assertThat(LocationValidation.tryParseUuid(null)).isEmpty();
    assertThat(LocationValidation.tryParseUuid("")).isEmpty();
    assertThat(LocationValidation.tryParseUuid("not-a-uuid")).isEmpty();
    var validUuid = UUID.randomUUID();
    assertThat(LocationValidation.tryParseUuid(validUuid.toString())).contains(validUuid);
  }

  @Test
  @DisplayName("Location DTOs serialize and deserialize correctly")
  void locationDtos_serialization() throws Exception {
    var createJson =
        """
        {
          "generator": "019273c5-9271-7000-8000-000000000001",
          "name": "Camp Whispering Pines",
          "summary": "Main campsite"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateLocationRequest.class);
    assertThat(createReq.generator()).isEqualTo("019273c5-9271-7000-8000-000000000001");
    assertThat(createReq.name()).isEqualTo("Camp Whispering Pines");
    assertThat(createReq.summary()).contains("Main campsite");

    var id = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new LocationResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Camp Whispering Pines",
            Optional.of("Summary"),
            now,
            now);
    var resJson = objectMapper.writeValueAsString(response);
    assertThat(resJson).contains(id.toString());
    assertThat(resJson)
        .contains("/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001");
    assertThat(resJson).contains("Camp Whispering Pines");
    assertThat(resJson).doesNotContain("tenantId");
  }
}
