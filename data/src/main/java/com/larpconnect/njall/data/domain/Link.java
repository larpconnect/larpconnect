package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted external studio link entity.
 *
 * @param id The unique link UUID identifier.
 * @param linkType The semantic type of the link (e.g. website, discord).
 * @param url The target destination URL.
 * @param mediaType The IANA media type of the target link.
 * @param summary Optional human-readable description from the base entity.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Link(
    UUID id,
    String linkType,
    String url,
    String mediaType,
    Optional<String> summary,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Link(
      UUID id,
      String linkType,
      String url,
      String mediaType,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        linkType,
        url,
        mediaType,
        Optional.ofNullable(summary),
        createdOn,
        updatedOn,
        Optional.ofNullable(deletedOn));
  }

  public boolean isDeleted() {
    return deletedOn.isPresent();
  }
}
