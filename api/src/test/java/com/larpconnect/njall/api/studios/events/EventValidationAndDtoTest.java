package com.larpconnect.njall.api.studios.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.domain.studios.EventType;
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
    var gen = "019273c5-9271-7000-8000-000000000001";
    var blankGen = new CreateEventRequest("", "Fest");
    assertThat(EventValidation.validateCreate(blankGen))
        .contains("Generator URI or actor identifier must not be blank");

    var blankTitle = new CreateEventRequest(gen, "");
    assertThat(EventValidation.validateCreate(blankTitle))
        .contains("Event title must not be blank");

    var now = Instant.now();
    var invalidTimeCreate =
        new CreateEventRequest(gen, "Fest", "Summary", null, now.plusSeconds(3600), now);
    assertThat(EventValidation.validateCreate(invalidTimeCreate))
        .contains("Event end time must not be earlier than start time");

    var validCreate =
        new CreateEventRequest(
            gen, "Fest", "Summary", UUID.randomUUID(), now, now.plusSeconds(3600));
    assertThat(EventValidation.validateCreate(validCreate)).isEmpty();

    var blankPatch = new UpdateEventRequest("   ", null, null, null, null);
    assertThat(EventValidation.validatePatch(blankPatch, Optional.empty()))
        .contains("Event title must not be blank");

    var invalidTimePatch = new UpdateEventRequest("Fest", null, null, now.plusSeconds(3600), now);
    assertThat(EventValidation.validatePatch(invalidTimePatch, Optional.empty()))
        .contains("Event end time must not be earlier than start time");

    var summaryOnlyMask = Optional.of(ImmutableSet.of("summary"));
    var unmaskedInvalid =
        new UpdateEventRequest(
            Optional.of("   "),
            Optional.of("Updated"),
            Optional.of(EventType.GAME),
            Optional.empty(),
            Optional.of(now.plusSeconds(3600)),
            Optional.of(now));
    assertThat(EventValidation.validatePatch(unmaskedInvalid, summaryOnlyMask)).isEmpty();

    var validPatch = new UpdateEventRequest("Autumn Fest - Rescheduled", null, null, null, null);
    assertThat(EventValidation.validatePatch(validPatch, Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("EventValidation rejects GAME eventType and permits physical event types")
  void eventValidation_eventTypeRules() {
    var gen = "019273c5-9271-7000-8000-000000000001";
    var gameCreate =
        new CreateEventRequest(
            gen,
            "Sunday Game",
            Optional.empty(),
            Optional.of(EventType.GAME),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(EventValidation.validateCreate(gameCreate))
        .contains("Events with eventType GAME must be created via /v1/games");

    var workshopCreate =
        new CreateEventRequest(
            gen,
            "Leathercraft Workshop",
            Optional.empty(),
            Optional.of(EventType.WORKSHOP),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(EventValidation.validateCreate(workshopCreate)).isEmpty();

    var gamePatch =
        new UpdateEventRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.of(EventType.GAME),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(EventValidation.validatePatch(gamePatch, Optional.empty()))
        .contains("Events cannot be updated to eventType GAME");

    var meetupPatch =
        new UpdateEventRequest(
            Optional.empty(),
            Optional.empty(),
            Optional.of(EventType.MEETUP),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    assertThat(EventValidation.validatePatch(meetupPatch, Optional.empty())).isEmpty();
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
          "generator": "019273c5-9271-7000-8000-000000000001",
          "title": "Autumn Harvest Festival",
          "summary": "Annual Gathering",
          "eventType": "WORKSHOP",
          "startTime": "2026-10-25T18:00:00Z",
          "endTime": "2026-10-27T12:00:00Z"
        }
        """;
    var createReq = objectMapper.readValue(createJson, CreateEventRequest.class);
    assertThat(createReq.generator()).isEqualTo("019273c5-9271-7000-8000-000000000001");
    assertThat(createReq.title()).isEqualTo("Autumn Harvest Festival");
    assertThat(createReq.summary()).contains("Annual Gathering");
    assertThat(createReq.eventType()).contains(EventType.WORKSHOP);
    assertThat(createReq.startTime()).isPresent();
    assertThat(createReq.endTime()).isPresent();

    var omittedTypeReq =
        objectMapper.readValue(
            "{\"generator\":\"019273c5-9271-7000-8000-000000000001\",\"title\":\"Fest\"}",
            CreateEventRequest.class);
    assertThat(omittedTypeReq.eventType()).isEmpty();

    var emptyCreateReq = objectMapper.readValue("{}", CreateEventRequest.class);
    assertThat(emptyCreateReq.generator()).isEmpty();
    assertThat(emptyCreateReq.title()).isEmpty();
  }

  @Test
  @DisplayName("EventResponse and UpdateEventRequest serialize and deserialize eventType")
  void eventResponseAndUpdateDto_serialization() throws Exception {

    var id = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new EventResponse(
            id,
            "/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001",
            "Autumn Harvest Festival",
            Optional.of("Summary"),
            EventType.CONFERENCE,
            Optional.empty(),
            Optional.of(now),
            Optional.of(now.plusSeconds(3600)),
            now,
            now);
    var resJson = objectMapper.writeValueAsString(response);
    assertThat(resJson).contains(id.toString());
    assertThat(resJson).contains("Autumn Harvest Festival");
    assertThat(resJson).contains("\"eventType\":\"CONFERENCE\"");
    assertThat(resJson)
        .contains("/api/studios/valiant/v1/actors/019273c5-9271-7000-8000-000000000001");
    assertThat(resJson).doesNotContain("tenantId");

    var updateJson =
        """
        {
          "title": "Updated Title",
          "eventType": "MEETUP"
        }
        """;
    var updateReq = objectMapper.readValue(updateJson, UpdateEventRequest.class);
    assertThat(updateReq.title()).contains("Updated Title");
    assertThat(updateReq.eventType()).contains(EventType.MEETUP);
    assertThat(updateReq.summary()).isEmpty();

    var emptyUpdateReq = new UpdateEventRequest();
    assertThat(emptyUpdateReq.title()).isEmpty();
    assertThat(emptyUpdateReq.eventType()).isEmpty();
  }
}
