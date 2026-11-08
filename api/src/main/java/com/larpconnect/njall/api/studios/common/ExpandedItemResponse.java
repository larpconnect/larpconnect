package com.larpconnect.njall.api.studios.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;

/**
 * Public HTTP representation of an expanded entity within a collection or inbox/outbox stream.
 *
 * @param id The entity UUID identifier.
 * @param type The entity type (e.g. "Individual", "Event", "Link").
 * @param generator The canonical studio Actor URI that generated this entity.
 * @param name The optional display name or title.
 * @param summary Optional description summary.
 */
@Immutable
public record ExpandedItemResponse(
    @JsonProperty("id") UUID id,
    @JsonProperty("type") String type,
    @JsonProperty("generator") String generator,
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("summary") Optional<String> summary) {}
