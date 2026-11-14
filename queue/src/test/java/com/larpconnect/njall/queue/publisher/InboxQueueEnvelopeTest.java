package com.larpconnect.njall.queue.publisher;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class InboxQueueEnvelopeTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

  @Test
  @DisplayName(
      "InboxQueueEnvelope preserves fields and defensively isolates targetActorIds and activity"
          + " JsonNode")
  void constructor_validParameters_createsImmutableEnvelope() {
    var tenantId = UUID.randomUUID();
    var studioId = UUID.randomUUID();
    var receivingActorId = UUID.randomUUID();
    var targetId1 = UUID.randomUUID();
    var targetId2 = UUID.randomUUID();
    var targets = new ArrayList<>(List.of(targetId1, targetId2));
    var receivedAt = Instant.parse("2026-11-14T11:00:00Z");
    var activity = objectMapper.createObjectNode().put("id", "https://example.com/act/1");
    var traceparent = Optional.of("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

    var envelope =
        new InboxQueueEnvelope(
            tenantId, studioId, receivingActorId, targets, receivedAt, activity, traceparent);

    // Modifying the original input list or ObjectNode must not affect the envelope
    targets.add(UUID.randomUUID());
    activity.put("mutated", true);
    assertThat(envelope.targetActorIds()).containsExactly(targetId1, targetId2);
    assertThat(envelope.activity().has("mutated")).isFalse();

    // Modifying the returned activity JsonNode must not mutate the envelope
    ((ObjectNode) envelope.activity()).put("postReadMutation", true);
    assertThat(envelope.activity().has("postReadMutation")).isFalse();
    assertThat(envelope.targetActorIds()).isInstanceOf(ImmutableList.class);

    assertThat(envelope.tenantId()).isEqualTo(tenantId);
    assertThat(envelope.studioId()).isEqualTo(studioId);
    assertThat(envelope.receivingActorId()).isEqualTo(receivingActorId);
    assertThat(envelope.receivedAt()).isEqualTo(receivedAt);
    assertThat(envelope.activity().get("id").asText()).isEqualTo("https://example.com/act/1");
    assertThat(envelope.traceparent()).isEqualTo(traceparent);
  }

  @Test
  @DisplayName("Convenience constructor sets traceparent to empty Optional")
  void convenienceConstructor_omitsTraceparent_createsInstanceWithEmptyTraceparent() {
    var tenantId = UUID.randomUUID();
    var studioId = UUID.randomUUID();
    var receivingActorId = UUID.randomUUID();
    var targetId = UUID.randomUUID();
    var receivedAt = Instant.now();
    var activity = objectMapper.createObjectNode().put("id", "https://example.com/act/2");

    var envelope =
        new InboxQueueEnvelope(
            tenantId, studioId, receivingActorId, ImmutableList.of(targetId), receivedAt, activity);

    assertThat(envelope.traceparent()).isEmpty();
  }

  @Test
  @DisplayName("InboxQueueEnvelope serializes and deserializes cleanly via Jackson")
  void jsonRoundtrip_serializesAndDeserializes() throws Exception {
    var tenantId = UUID.randomUUID();
    var studioId = UUID.randomUUID();
    var receivingActorId = UUID.randomUUID();
    var targetActorIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var receivedAt = Instant.parse("2026-11-14T11:00:00Z");
    var activity =
        objectMapper
            .createObjectNode()
            .put("id", "https://remote.example/activities/001")
            .put("type", "Create");
    var traceparent = Optional.of("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

    var original =
        new InboxQueueEnvelope(
            tenantId,
            studioId,
            receivingActorId,
            targetActorIds,
            receivedAt,
            activity,
            traceparent);

    var jsonString = objectMapper.writeValueAsString(original);
    var deserialized = objectMapper.readValue(jsonString, InboxQueueEnvelope.class);

    assertThat(deserialized).isEqualTo(original);
    assertThat(deserialized.tenantId()).isEqualTo(tenantId);
    assertThat(deserialized.targetActorIds()).containsExactlyElementsOf(targetActorIds);
    assertThat(deserialized.traceparent()).isEqualTo(traceparent);
  }

  @Test
  @DisplayName(
      "InboxQueueEnvelope normalizes omitted traceparent and targetActorIds on JSON"
          + " deserialization")
  void jsonDeserialization_omittedOptionalFields_normalizesCleanly() throws Exception {
    var root = objectMapper.createObjectNode();
    root.put("tenantId", UUID.randomUUID().toString());
    root.put("studioId", UUID.randomUUID().toString());
    root.put("receivingActorId", UUID.randomUUID().toString());
    root.put("receivedAt", "2026-11-14T11:00:00Z");
    root.putObject("activity")
        .put("id", "https://remote.example/activities/002")
        .put("type", "Delete");

    var deserialized =
        objectMapper.readValue(objectMapper.writeValueAsString(root), InboxQueueEnvelope.class);

    assertThat(deserialized.targetActorIds()).isEmpty();
    assertThat(deserialized.traceparent()).isEmpty();
  }

  @Test
  @DisplayName("equals and hashCode contract holds")
  void equalsAndHashCode_behaveCorrectly() {
    var tenantId = UUID.randomUUID();
    var studioId = UUID.randomUUID();
    var receivingActorId = UUID.randomUUID();
    var targetActorIds = ImmutableList.of(UUID.randomUUID());
    var receivedAt = Instant.parse("2026-11-14T11:00:00Z");
    var activity = objectMapper.createObjectNode().put("id", "https://example.com/act/1");

    var env1 =
        new InboxQueueEnvelope(
            tenantId, studioId, receivingActorId, targetActorIds, receivedAt, activity);
    var env2 =
        new InboxQueueEnvelope(
            tenantId, studioId, receivingActorId, targetActorIds, receivedAt, activity);

    assertThat(env1).isEqualTo(env2);
    assertThat(env1.hashCode()).isEqualTo(env2.hashCode());
    assertThat(env1.toString()).contains(tenantId.toString());
  }
}
