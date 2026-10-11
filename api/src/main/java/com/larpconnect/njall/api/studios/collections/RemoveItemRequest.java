package com.larpconnect.njall.api.studios.collections;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/** Request payload for removing an entity reference from a collection (AIP-136). */
@Immutable
public record RemoveItemRequest(@JsonProperty("refersToId") UUID refersToId) {

  @JsonCreator
  public RemoveItemRequest {}
}
