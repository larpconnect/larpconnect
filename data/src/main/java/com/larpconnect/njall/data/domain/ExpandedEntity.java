package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of an expanded entity within a collection.
 *
 * @param id The entity UUID identifier.
 * @param type The entity type (e.g. "Individual", "Event", "Link").
 * @param name The optional display name or title.
 * @param summary Optional description summary.
 */
@Immutable
public record ExpandedEntity(
    UUID id, String type, Optional<String> name, Optional<String> summary) {

  public ExpandedEntity(UUID id, String type, @Nullable String name, @Nullable String summary) {
    this(id, type, Optional.ofNullable(name), Optional.ofNullable(summary));
  }
}
