package com.larpconnect.njall.queue.publisher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable envelope carrying an accepted FeatherPub Activity and routing metadata for queueing.
 *
 * @param tenantId Tenant partition identifier.
 * @param studioId Studio identifier.
 * @param receivingActorId Receiving actor identifier.
 * @param targetActorIds Addressable local descendant recipients.
 * @param receivedAt Ingress timestamp.
 * @param activity Activity JSON payload.
 * @param traceparent Optional W3C distributed trace context header value.
 */
@Immutable
// Jackson's JsonNode is not annotated @Immutable; immutability is enforced by deep-copying
// activity both in the compact constructor and in the activity() accessor.
@SuppressWarnings("Immutable")
public record InboxQueueEnvelope(
    @JsonProperty("tenantId") UUID tenantId,
    @JsonProperty("studioId") UUID studioId,
    @JsonProperty("receivingActorId") UUID receivingActorId,
    @JsonProperty("targetActorIds") ImmutableList<UUID> targetActorIds,
    @JsonProperty("receivedAt") Instant receivedAt,
    @JsonProperty("activity") JsonNode activity,
    @JsonProperty("traceparent") Optional<String> traceparent) {

  public InboxQueueEnvelope {
    activity = activity.deepCopy();
  }

  /** Jackson and convenience constructor accepting a standard {@link List} of target actor IDs. */
  @JsonCreator
  public InboxQueueEnvelope(
      @JsonProperty("tenantId") UUID tenantId,
      @JsonProperty("studioId") UUID studioId,
      @JsonProperty("receivingActorId") UUID receivingActorId,
      @JsonProperty("targetActorIds") @Nullable List<UUID> targetActorIds,
      @JsonProperty("receivedAt") Instant receivedAt,
      @JsonProperty("activity") JsonNode activity,
      @JsonProperty("traceparent") Optional<String> traceparent) {
    this(
        tenantId,
        studioId,
        receivingActorId,
        targetActorIds == null ? ImmutableList.of() : ImmutableList.copyOf(targetActorIds),
        receivedAt,
        activity,
        traceparent);
  }

  /** Convenience constructor creating an envelope without a W3C traceparent. */
  public InboxQueueEnvelope(
      UUID tenantId,
      UUID studioId,
      UUID receivingActorId,
      ImmutableList<UUID> targetActorIds,
      Instant receivedAt,
      JsonNode activity) {
    this(
        tenantId,
        studioId,
        receivingActorId,
        targetActorIds,
        receivedAt,
        activity,
        Optional.empty());
  }

  @Override
  public JsonNode activity() {
    return activity.deepCopy();
  }
}
