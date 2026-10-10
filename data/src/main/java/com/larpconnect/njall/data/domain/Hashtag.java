package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable domain representation of a tenanted hashtag entity extending links via CTI.
 *
 * @param id The unique hashtag UUID identifier.
 * @param tag The case-preserved hashtag string.
 * @param linkType The semantic type of the underlying link (e.g. hashtag).
 * @param url The canonical destination URL.
 * @param mediaType The IANA media type.
 * @param summary Optional human-readable description from the base entity.
 * @param createdOn Creation timestamp.
 * @param updatedOn Last updated timestamp.
 * @param deletedOn Soft deletion timestamp, or empty if active.
 */
@Immutable
public record Hashtag(
    UUID id,
    String tag,
    String linkType,
    String url,
    String mediaType,
    Optional<String> summary,
    Instant createdOn,
    Instant updatedOn,
    Optional<Instant> deletedOn)
    implements DatabaseObject {

  public Hashtag(
      UUID id,
      String tag,
      String linkType,
      String url,
      String mediaType,
      @Nullable String summary,
      Instant createdOn,
      Instant updatedOn,
      @Nullable Instant deletedOn) {
    this(
        id,
        tag,
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
