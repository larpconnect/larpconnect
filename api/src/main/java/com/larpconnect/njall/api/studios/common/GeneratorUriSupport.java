package com.larpconnect.njall.api.studios.common;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Shared utility for parsing and formatting ActivityStreams generator identifiers.
 *
 * <p>Supports formatting actor UUIDs as canonical studio Actor URIs and parsing inputs in either
 * bare UUID form or canonical studio Actor URI path form.
 */
@Immutable
public final class GeneratorUriSupport {

  private GeneratorUriSupport() {}

  /**
   * Formats an actor UUID as a canonical studio Actor URI.
   *
   * @param studioIdentifier The studio alias or UUID segment used in the request path.
   * @param actorId The actor entity UUID.
   * @return Canonical studio Actor URI path.
   */
  public static String formatGeneratorUri(String studioIdentifier, UUID actorId) {
    return StudioResourceUriSupport.formatResourceUri(
        studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, actorId);
  }

  /**
   * Parses an input generator string into an actor UUID.
   *
   * <p>Accepts either a bare UUID or a canonical studio Actor URI for the specified studio.
   *
   * @param rawInput The raw generator string from a request payload.
   * @param studioIdentifier The studio identifier from the request path.
   * @param lookup The resolved active studio lookup entry, or null if unavailable.
   * @return Parsed actor UUID, or empty if malformed, blank, or cross-studio.
   */
  public static Optional<UUID> parseGenerator(
      @Nullable String rawInput, String studioIdentifier, @Nullable StudioLookup lookup) {
    return parseGenerator(rawInput, studioIdentifier, Optional.ofNullable(lookup));
  }

  /**
   * Parses an input generator string into an actor UUID.
   *
   * <p>Accepts either a bare UUID or a canonical studio Actor URI for the specified studio.
   *
   * @param rawInput The raw generator string from a request payload.
   * @param studioIdentifier The studio identifier from the request path.
   * @param lookup The resolved active studio lookup entry.
   * @return Parsed actor UUID, or empty if malformed, blank, or cross-studio.
   */
  public static Optional<UUID> parseGenerator(
      @Nullable String rawInput, String studioIdentifier, Optional<StudioLookup> lookup) {
    try {
      return Optional.of(
          StudioResourceUriSupport.parseResourceUri(
              rawInput, studioIdentifier, StudioResourceUriSupport.ACTORS_COLLECTION, lookup));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /**
   * Parses an input generator string into an actor UUID against a studio identifier.
   *
   * @param rawInput The raw generator string from a request payload.
   * @param studioIdentifier The studio identifier from the request path.
   * @return Parsed actor UUID, or empty if malformed, blank, or cross-studio.
   */
  public static Optional<UUID> parseGenerator(@Nullable String rawInput, String studioIdentifier) {
    return parseGenerator(rawInput, studioIdentifier, Optional.empty());
  }
}
