package com.larpconnect.njall.api.studios.actors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Public HTTP representation of a tenanted studio actor. */
@Immutable
public record ActorResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("type") String type,
    @JsonProperty("ownerId") UUID ownerId,
    @JsonProperty("preferredUsername") String preferredUsername,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("inbox") String inbox,
    @JsonProperty("outbox") String outbox,
    @JsonProperty("endpoints") ImmutableMap<String, String> endpoints,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {

  public ActorResponse {
    endpoints = ImmutableMap.copyOf(endpoints);
  }

  public ActorResponse(
      UUID id,
      String type,
      UUID ownerId,
      String preferredUsername,
      Optional<String> summary,
      String inbox,
      String outbox,
      Map<String, String> endpoints,
      Instant createdOn,
      Instant updatedOn) {
    this(
        id,
        type,
        ownerId,
        preferredUsername,
        summary,
        inbox,
        outbox,
        ImmutableMap.copyOf(endpoints),
        createdOn,
        updatedOn);
  }
}
