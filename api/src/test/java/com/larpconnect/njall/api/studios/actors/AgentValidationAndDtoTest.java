package com.larpconnect.njall.api.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentValidationAndDtoTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new GuavaModule());

  @Test
  @DisplayName("AgentValidation validates create actor rules")
  void agentValidation_createRules() {
    var validOwnerId = UUID.randomUUID();

    var nullUsername = new CreateActorRequest(validOwnerId, null, Optional.empty());
    assertThat(AgentValidation.validateCreate(nullUsername))
        .contains("Preferred username must not be blank");

    var emptyUsername = new CreateActorRequest(validOwnerId, "", Optional.empty());
    assertThat(AgentValidation.validateCreate(emptyUsername))
        .contains("Preferred username must not be blank");

    var blankUsername = new CreateActorRequest(validOwnerId, "   ", Optional.empty());
    assertThat(AgentValidation.validateCreate(blankUsername))
        .contains("Preferred username must not be blank");

    var valid = new CreateActorRequest(validOwnerId, "valkyrie_lead", Optional.of("Summary"));
    assertThat(AgentValidation.validateCreate(valid)).isEmpty();
  }

  @Test
  @DisplayName("AgentValidation validates patch actor rules")
  void agentValidation_patchRules() {
    var blankUsername = new UpdateActorRequest(Optional.of("   "), Optional.empty());
    assertThat(AgentValidation.validatePatch(blankUsername))
        .contains("Preferred username must not be blank");

    var validUsername = new UpdateActorRequest("valkyrie_commander", "Updated bio");
    assertThat(AgentValidation.validatePatch(validUsername)).isEmpty();

    var empty = new UpdateActorRequest();
    assertThat(AgentValidation.validatePatch(empty)).isEmpty();
  }

  @Test
  @DisplayName("AgentValidation validates add and remove endpoint rules")
  void agentValidation_endpointRules() {
    var nullName = new AddEndpointRequest(null, "https://fed.example.com");
    assertThat(AgentValidation.validateAddEndpoint(nullName))
        .contains("Endpoint name must be alphanumeric and less than 32 characters");

    var emptyName = new AddEndpointRequest("", "https://fed.example.com");
    assertThat(AgentValidation.validateAddEndpoint(emptyName))
        .contains("Endpoint name must be alphanumeric and less than 32 characters");

    var invalidChars = new AddEndpointRequest("invalid-hyphens", "https://fed.example.com");
    assertThat(AgentValidation.validateAddEndpoint(invalidChars))
        .contains("Endpoint name must be alphanumeric and less than 32 characters");

    var tooLong = new AddEndpointRequest("a".repeat(32), "https://fed.example.com");
    assertThat(AgentValidation.validateAddEndpoint(tooLong))
        .contains("Endpoint name must be alphanumeric and less than 32 characters");

    var nullEndpoint = new AddEndpointRequest("sharedInbox", null);
    assertThat(AgentValidation.validateAddEndpoint(nullEndpoint))
        .contains("Endpoint URI must not be blank");

    var emptyEndpoint = new AddEndpointRequest("sharedInbox", "");
    assertThat(AgentValidation.validateAddEndpoint(emptyEndpoint))
        .contains("Endpoint URI must not be blank");

    var blankEndpoint = new AddEndpointRequest("sharedInbox", "   ");
    assertThat(AgentValidation.validateAddEndpoint(blankEndpoint))
        .contains("Endpoint URI must not be blank");

    var validAdd = new AddEndpointRequest("sharedInbox", "https://fed.example.com/inbox");
    assertThat(AgentValidation.validateAddEndpoint(validAdd)).isEmpty();

    var nullRemove = new RemoveEndpointRequest(null);
    assertThat(AgentValidation.validateRemoveEndpoint(nullRemove))
        .contains("Endpoint name must not be blank");

    var emptyRemove = new RemoveEndpointRequest("");
    assertThat(AgentValidation.validateRemoveEndpoint(emptyRemove))
        .contains("Endpoint name must not be blank");

    var blankRemove = new RemoveEndpointRequest("  ");
    assertThat(AgentValidation.validateRemoveEndpoint(blankRemove))
        .contains("Endpoint name must not be blank");

    var validRemove = new RemoveEndpointRequest("sharedInbox");
    assertThat(AgentValidation.validateRemoveEndpoint(validRemove)).isEmpty();
  }

  @Test
  @DisplayName("AgentValidation checks tryParseUuid")
  void agentValidation_tryParseUuid() {
    assertThat(AgentValidation.tryParseUuid(null)).isEmpty();
    assertThat(AgentValidation.tryParseUuid("")).isEmpty();
    assertThat(AgentValidation.tryParseUuid("invalid-uuid")).isEmpty();

    var valid = UUID.randomUUID();
    assertThat(AgentValidation.tryParseUuid(valid.toString())).contains(valid);
  }

  @Test
  @DisplayName("Actor request and response DTOs serialize and deserialize correctly")
  void actorRequestAndResponse_serialization() throws Exception {
    var ownerId = UUID.randomUUID();
    var createJson =
        "{\"ownerId\":\""
            + ownerId
            + "\",\"preferredUsername\":\"valkyrie_lead\",\"summary\":\"Lead singer\"}";
    var createReq = objectMapper.readValue(createJson, CreateActorRequest.class);
    assertThat(createReq.ownerId()).isEqualTo(ownerId);
    assertThat(createReq.preferredUsername()).isEqualTo("valkyrie_lead");
    assertThat(createReq.summary()).contains("Lead singer");

    var actorId = UUID.randomUUID();
    var now = Instant.now();
    var response =
        new ActorResponse(
            actorId,
            "Actor",
            ownerId,
            "valkyrie_lead",
            Optional.of("Lead singer"),
            "/api/studios/valiant/v1/actors/" + actorId + "/inbox",
            "/api/studios/valiant/v1/actors/" + actorId + "/outbox",
            ImmutableMap.of("sharedInbox", "https://fed.example.com/inbox"),
            now,
            now);

    var resJson = objectMapper.writeValueAsString(response);
    assertThat(resJson).contains(actorId.toString());
    assertThat(resJson).contains("valkyrie_lead");
    assertThat(resJson).contains("sharedInbox");
    assertThat(resJson).doesNotContain("tenantId");

    var updateJson = "{\"preferredUsername\":\"valkyrie_commander\"}";
    var updateReq = objectMapper.readValue(updateJson, UpdateActorRequest.class);
    assertThat(updateReq.preferredUsername()).contains("valkyrie_commander");
    assertThat(updateReq.summary()).isEmpty();
  }

  @Test
  @DisplayName("Actor endpoint DTOs serialize and deserialize correctly")
  void endpointDtos_serialization() throws Exception {
    var addEndpointJson =
        "{\"name\":\"sharedInbox\",\"endpoint\":\"https://fed.example.com/inbox\"}";
    var addReq = objectMapper.readValue(addEndpointJson, AddEndpointRequest.class);
    assertThat(addReq.name()).isEqualTo("sharedInbox");
    assertThat(addReq.endpoint()).isEqualTo("https://fed.example.com/inbox");

    var removeEndpointJson = "{\"name\":\"sharedInbox\"}";
    var removeReq = objectMapper.readValue(removeEndpointJson, RemoveEndpointRequest.class);
    assertThat(removeReq.name()).isEqualTo("sharedInbox");
  }
}
