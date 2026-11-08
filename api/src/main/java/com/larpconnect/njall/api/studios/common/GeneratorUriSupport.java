package com.larpconnect.njall.api.studios.common;

import com.larpconnect.njall.data.domain.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Shared utility for parsing and formatting ActivityStreams generator identifiers.
 *
 * <p>Supports formatting actor UUIDs as canonical studio Actor URIs and parsing inputs in either
 * bare UUID form or canonical studio Actor URI path form.
 */
public final class GeneratorUriSupport {

  private static final Pattern CANONICAL_URI_PATTERN =
      Pattern.compile("^/api/studios/([^/]+)/v1/actors/([0-9a-fA-F-]{36})$");

  private GeneratorUriSupport() {}

  /**
   * Formats an actor UUID as a canonical studio Actor URI.
   *
   * @param studioIdentifier The studio alias or UUID segment used in the request path.
   * @param actorId The actor entity UUID.
   * @return Canonical studio Actor URI path.
   */
  public static String formatGeneratorUri(String studioIdentifier, UUID actorId) {
    return "/api/studios/" + studioIdentifier + "/v1/actors/" + actorId;
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
    if (rawInput == null || rawInput.isBlank()) {
      return Optional.empty();
    }
    var trimmed = rawInput.trim();
    var bareUuid = tryParseUuid(trimmed);
    if (bareUuid.isPresent()) {
      return bareUuid;
    }
    return parseCanonicalUri(trimmed, studioIdentifier, lookup);
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

  private static Optional<UUID> parseCanonicalUri(
      String trimmed, String studioIdentifier, Optional<StudioLookup> lookup) {
    var matcher = CANONICAL_URI_PATTERN.matcher(trimmed);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    var studioSegment = matcher.group(1);
    if (!matchesStudio(studioSegment, studioIdentifier, lookup)) {
      return Optional.empty();
    }
    return tryParseUuid(matcher.group(2));
  }

  private static boolean matchesStudio(
      String studioSegment, String studioIdentifier, Optional<StudioLookup> lookup) {
    if (studioSegment.equalsIgnoreCase(studioIdentifier)) {
      return true;
    }
    return lookup
        .filter(
            l ->
                studioSegment.equalsIgnoreCase(l.alias())
                    || studioSegment.equalsIgnoreCase(l.studioId().toString()))
        .isPresent();
  }

  private static Optional<UUID> tryParseUuid(String candidate) {
    try {
      return Optional.of(UUID.fromString(candidate));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
