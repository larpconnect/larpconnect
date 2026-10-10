package com.larpconnect.njall.api.studios.events;

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

final class EventValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("EventValidation handles create and patch rules")
  void eventValidation_rules() {
    var blankTitle = new CreateEventRequest("");
    assertThat(EventValidation.validateCreate(blankTitle))
        .contains("Event title must not be blank");

    var now = Instant.now();
    var invalidTimeCreate =
        new CreateEventRequest("Fest", "Summary", null, now.plusSeconds(3600), now);
    assertThat(EventValidation.validateCreate(invalidTimeCreate))
        .contains("Event end time must not be earlier than start time");

    var validCreate =
        new CreateEventRequest("Fest", "Summary", UUID.randomUUID(), now, now.plusSeconds(3600));
    assertThat(EventValidation.validateCreate(validCreate)).isEmpty();

    var blankPatch = new UpdateEventRequest("   ", null, null, null, null);
    assertThat(EventValidation.validatePatch(blankPatch)).contains("Event title must not be blank");

    var invalidTimePatch = new UpdateEventRequest("Fest", null, null, now.plusSeconds(3600), now);
    assertThat(EventValidation.validatePatch(invalidTimePatch))
        .contains("Event end time must not be earlier than start time");

    var validPatch = new UpdateEventRequest("Autumn Fest - Rescheduled", null, null, null, null);
    assertThat(EventValidation.validatePatch(validPatch)).isEmpty();
  }

  @Test
  @DisplayName("EventValidation checks tryParseUuid")
  void eventValidation_tryParseUuid() {
    assertThat(EventValidation.tryParseUuid(null)).isEmpty();
    assertThat(EventValidation.tryParseUuid("")).isEmpty();
    assertThat(EventValidation.tryParseUuid("invalid-uuid")).isEmpty();

    var valid = UUID.randomUUID();
    assertThat(EventValidation.tryParseUuid(valid.toString())).contains(valid);
  }

  @Test
  @DisplayName("Event DTOs serialize and deserialize correctly")
  void eventDtos_serialization() throws Exception {
    var createJson =
        """
        {
          "title": "Autumn Harvest Festival",
          "summary": "Annual Gathering",
          "startTime": "2026-10-25T18:00:00Z",
          "endTime": "2026-10-27T12:00:00Z"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateEventRequest.class);
    assertThat(createReq.title()).isEqualTo("Autumn Harvest Festival");
    assertThat(createReq.summary()).contains("Annual Gathering");
    assertThat(createReq.startTime()).isPresent();
    assertThat(createReq.endTime()).isPresent();

    var id = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new EventResponse(
            id,
            "Autumn Harvest Festival",
            Optional.of("Summary"),
            Optional.empty(),
            Optional.of(now),
            Optional.of(now.plusSeconds(3600)),
            now,
            now);
    var resJson = objectMapper.writeValueAsString(response);
    assertThat(resJson).contains(id.toString());
    assertThat(resJson).contains("Autumn Harvest Festival");
    assertThat(resJson).doesNotContain("tenantId");

    var updateJson =
        """
        {
          "title": "Updated Title"
        }
        """;
    var updateReq = objectMapper.readValue(updateJson, UpdateEventRequest.class);
    assertThat(updateReq.title()).contains("Updated Title");
    assertThat(updateReq.summary()).isEmpty();
  }
}
