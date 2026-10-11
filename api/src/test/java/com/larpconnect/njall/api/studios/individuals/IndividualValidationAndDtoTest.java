package com.larpconnect.njall.api.studios.individuals;

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

final class IndividualValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("IndividualValidation handles create and patch rules")
  void individualValidation_rules() {
    var nullNameCreate = new CreateIndividualRequest(null);
    assertThat(IndividualValidation.validateCreate(nullNameCreate))
        .contains("Individual name must not be blank");

    var blankNameCreate = new CreateIndividualRequest("   ");
    assertThat(IndividualValidation.validateCreate(blankNameCreate))
        .contains("Individual name must not be blank");

    var validCreate = new CreateIndividualRequest("Jane Eyre", Optional.of("Visiting scholar"));
    assertThat(IndividualValidation.validateCreate(validCreate)).isEmpty();

    var blankPatch = new UpdateIndividualRequest("   ", null);
    assertThat(IndividualValidation.validatePatch(blankPatch))
        .contains("Individual name must not be blank");

    var validPatch = new UpdateIndividualRequest("Jane Rochester", "Updated summary");
    assertThat(IndividualValidation.validatePatch(validPatch)).isEmpty();

    var emptyPatch = new UpdateIndividualRequest();
    assertThat(IndividualValidation.validatePatch(emptyPatch)).isEmpty();
  }

  @Test
  @DisplayName("IndividualValidation checks tryParseUuid")
  void individualValidation_tryParseUuid() {
    assertThat(IndividualValidation.tryParseUuid(null)).isEmpty();
    assertThat(IndividualValidation.tryParseUuid("")).isEmpty();
    assertThat(IndividualValidation.tryParseUuid("invalid-uuid")).isEmpty();

    var valid = UUID.randomUUID();
    assertThat(IndividualValidation.tryParseUuid(valid.toString())).contains(valid);
  }

  @Test
  @DisplayName("Individual DTOs serialize and deserialize correctly")
  void individualDtos_serialization() throws Exception {
    var createJson =
        """
        {
          "name": "Jane Eyre",
          "summary": "Visiting scholar"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateIndividualRequest.class);
    assertThat(createReq.name()).isEqualTo("Jane Eyre");
    assertThat(createReq.summary()).contains("Visiting scholar");

    var id = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new IndividualResponse(id, "Jane Eyre", Optional.of("Visiting scholar"), now, now);
    var resJson = objectMapper.writeValueAsString(response);
    assertThat(resJson).contains(id.toString());
    assertThat(resJson).contains("Jane Eyre");
    assertThat(resJson).doesNotContain("tenantId");

    var updateJson =
        """
        {
          "name": "Jane Rochester"
        }
        """;
    var updateReq = objectMapper.readValue(updateJson, UpdateIndividualRequest.class);
    assertThat(updateReq.name()).contains("Jane Rochester");
    assertThat(updateReq.summary()).isEmpty();
  }
}
