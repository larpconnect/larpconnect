package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Request DTO for AIP-233 batch activity ingress. */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = false)
public record BatchInboxActivitiesRequest(
    @JsonProperty("requests") ImmutableList<JsonNode> requests) {

  public BatchInboxActivitiesRequest {
    requests = copyRequests(requests);
  }

  @JsonCreator
  public BatchInboxActivitiesRequest(
      @JsonProperty(value = "requests", required = true)
          @Nullable List<@Nullable JsonNode> requests) {
    this(normalizeRequests(requests));
  }

  @Override
  public ImmutableList<JsonNode> requests() {
    return copyRequests(requests);
  }

  private static ImmutableList<JsonNode> normalizeRequests(
      @Nullable List<@Nullable JsonNode> items) {
    if (items == null) {
      return ImmutableList.of();
    }
    return items.stream()
        .<JsonNode>map(item -> item == null ? NullNode.getInstance() : item)
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<JsonNode> copyRequests(List<JsonNode> items) {
    return items.stream()
        .<JsonNode>map(JsonNode::deepCopy)
        .collect(ImmutableList.toImmutableList());
  }
}
