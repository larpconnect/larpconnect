package com.larpconnect.njall.api.studios.collections;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Public HTTP representation of a tenanted studio OrderedCollection. */
@Immutable
public record OrderedCollectionResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("type") String type,
    @JsonProperty("name") String name,
    @JsonProperty("summary") Optional<String> summary,
    @JsonProperty("ownerId") UUID ownerId,
    @JsonProperty("totalItems") int totalItems,
    @JsonProperty("orderedItems") ImmutableList<Object> orderedItems,
    @JsonProperty("createdOn") Instant createdOn,
    @JsonProperty("updatedOn") Instant updatedOn) {

  public OrderedCollectionResponse {
    orderedItems = ImmutableList.copyOf(orderedItems);
  }

  public OrderedCollectionResponse(
      UUID id,
      String type,
      String name,
      Optional<String> summary,
      UUID ownerId,
      int totalItems,
      List<?> orderedItems,
      Instant createdOn,
      Instant updatedOn) {
    this(
        id,
        type,
        name,
        summary,
        ownerId,
        totalItems,
        ImmutableList.copyOf(orderedItems),
        createdOn,
        updatedOn);
  }
}
