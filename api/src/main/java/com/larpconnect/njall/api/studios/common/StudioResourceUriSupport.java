package com.larpconnect.njall.api.studios.common;

import com.google.errorprone.annotations.Immutable;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Shared utility for formatting and parsing AIP-122 studio resource URIs.
 *
 * <p>Formats resource UUIDs as canonical paths ({@code
 * /api/studios/{studio-id}/v1/<collection>/{id}}) and parses inputs in either bare UUID form or
 * same-studio canonical URI form.
 */
@Immutable
public final class StudioResourceUriSupport {

  public static final String ACTORS_COLLECTION = "actors";
  public static final String SYSTEMS_COLLECTION = "systems";
  public static final String CAMPAIGNS_COLLECTION = "campaigns";
  public static final String GAMES_COLLECTION = "games";
  public static final String EVENTS_COLLECTION = "events";
  public static final String LOCATIONS_COLLECTION = "locations";
  public static final String INDIVIDUALS_COLLECTION = "individuals";
  public static final String LINKS_COLLECTION = "links";
  public static final String TAGS_COLLECTION = "tags";

  private static final String UUID_REGEX =
      "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  private static final Pattern UUID_PATTERN = Pattern.compile("^" + UUID_REGEX + "$");
  private static final Pattern CANONICAL_URI_PATTERN =
      Pattern.compile("^/api/studios/([^/]+)/v1/([^/]+)/(" + UUID_REGEX + ")$");

  private StudioResourceUriSupport() {}

  /**
   * Formats a resource UUID as a canonical studio resource URI.
   *
   * @param studioIdentifier Studio alias or UUID segment from the request path.
   * @param collection Top-level collection name (e.g., {@code "systems"}, {@code "actors"}).
   * @param resourceId Resource UUID.
   * @return Canonical studio resource URI path.
   * @throws IllegalArgumentException if {@code studioIdentifier} or {@code collection} is blank or
   *     contains {@code '/'}.
   */
  public static String formatResourceUri(
      String studioIdentifier, String collection, UUID resourceId) {
    requireSingleSegment(studioIdentifier, "Studio identifier");
    requireSingleSegment(collection, "Collection");
    return "/api/studios/" + studioIdentifier + "/v1/" + collection + "/" + resourceId;
  }

  /**
   * Parses an input resource reference into a UUID against the active studio lookup.
   *
   * @param rawInput Bare UUID string or canonical studio resource URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @param lookup Resolved active studio lookup entry, or {@code null} if unavailable.
   * @return Parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is null, blank, malformed, references
   *     another collection, or references another studio.
   */
  public static UUID parseResourceUri(
      @Nullable String rawInput,
      String studioIdentifier,
      String expectedCollection,
      @Nullable StudioLookup lookup) {
    return parseResourceUri(
        rawInput, studioIdentifier, expectedCollection, Optional.ofNullable(lookup));
  }

  /**
   * Parses an input resource reference into a UUID against an optional studio lookup.
   *
   * @param rawInput Bare UUID string or canonical studio resource URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @param lookup Optional resolved active studio lookup entry.
   * @return Parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is null, blank, malformed, references
   *     another collection, or references another studio.
   */
  public static UUID parseResourceUri(
      @Nullable String rawInput,
      String studioIdentifier,
      String expectedCollection,
      Optional<StudioLookup> lookup) {
    requireSingleSegment(expectedCollection, "Expected collection");
    if (rawInput == null || rawInput.isBlank()) {
      throw createIllegalArgumentException("Resource identifier must not be blank");
    }
    var trimmed = rawInput.trim();
    if (trimmed.startsWith("/")) {
      return parseCanonicalUri(trimmed, studioIdentifier, expectedCollection, lookup);
    }
    return parseBareUuid(trimmed);
  }

  /**
   * Parses an input resource reference into a UUID without an external studio lookup.
   *
   * @param rawInput Bare UUID string or canonical studio resource URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @return Parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is null, blank, malformed, references
   *     another collection, or references another studio.
   */
  public static UUID parseResourceUri(
      @Nullable String rawInput, String studioIdentifier, String expectedCollection) {
    return parseResourceUri(rawInput, studioIdentifier, expectedCollection, Optional.empty());
  }

  /**
   * Parses an optional resource reference into an optional UUID.
   *
   * @param rawInput Optional raw resource identifier or URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @param lookup Optional resolved active studio lookup entry.
   * @return Empty if {@code rawInput} is empty; otherwise the parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is present and invalid or cross-studio.
   */
  public static Optional<UUID> parseOptionalResourceUri(
      Optional<String> rawInput,
      String studioIdentifier,
      String expectedCollection,
      Optional<StudioLookup> lookup) {
    return rawInput.map(
        input -> parseResourceUri(input, studioIdentifier, expectedCollection, lookup));
  }

  /**
   * Parses an optional resource reference into an optional UUID.
   *
   * @param rawInput Optional raw resource identifier or URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @param lookup Resolved active studio lookup entry, or {@code null} if unavailable.
   * @return Empty if {@code rawInput} is empty; otherwise the parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is present and invalid or cross-studio.
   */
  public static Optional<UUID> parseOptionalResourceUri(
      Optional<String> rawInput,
      String studioIdentifier,
      String expectedCollection,
      @Nullable StudioLookup lookup) {
    return parseOptionalResourceUri(
        rawInput, studioIdentifier, expectedCollection, Optional.ofNullable(lookup));
  }

  /**
   * Parses an optional resource reference into an optional UUID without external studio lookup.
   *
   * @param rawInput Optional raw resource identifier or URI.
   * @param studioIdentifier Studio identifier from the request path.
   * @param expectedCollection Expected collection path segment.
   * @return Empty if {@code rawInput} is empty; otherwise the parsed resource UUID.
   * @throws IllegalArgumentException if {@code rawInput} is present and invalid or cross-studio.
   */
  public static Optional<UUID> parseOptionalResourceUri(
      Optional<String> rawInput, String studioIdentifier, String expectedCollection) {
    return parseOptionalResourceUri(
        rawInput, studioIdentifier, expectedCollection, Optional.empty());
  }

  private static void requireSingleSegment(String value, String label) {
    if (value.isBlank() || value.contains("/")) {
      throw createIllegalArgumentException(label + " must be a single non-blank path segment");
    }
  }

  private static UUID parseBareUuid(String trimmed) {
    if (!UUID_PATTERN.matcher(trimmed).matches()) {
      throw createIllegalArgumentException("Malformed studio resource identifier: " + trimmed);
    }
    return UUID.fromString(trimmed);
  }

  private static UUID parseCanonicalUri(
      String trimmed,
      String studioIdentifier,
      String expectedCollection,
      Optional<StudioLookup> lookup) {
    var matcher = CANONICAL_URI_PATTERN.matcher(trimmed);
    if (!matcher.matches()) {
      throw createIllegalArgumentException("Malformed studio resource URI: " + trimmed);
    }
    var studioSegment = matcher.group(1);
    var collectionSegment = matcher.group(2);
    var resourceIdSegment = matcher.group(3);
    if (!collectionSegment.equals(expectedCollection)) {
      throw createIllegalArgumentException(
          "Expected collection '" + expectedCollection + "' but got '" + collectionSegment + "'");
    }
    if (!matchesStudio(studioSegment, studioIdentifier, lookup)) {
      throw createIllegalArgumentException(
          "Studio segment '" + studioSegment + "' does not match current studio");
    }
    return UUID.fromString(resourceIdSegment);
  }

  private static IllegalArgumentException createIllegalArgumentException(String message) {
    return new IllegalArgumentException(message);
  }

  private static boolean matchesStudio(
      String studioSegment, String studioIdentifier, Optional<StudioLookup> lookup) {
    if (studioSegment.equalsIgnoreCase(studioIdentifier)) {
      return true;
    }
    return lookup
        .filter(
            entry ->
                studioSegment.equalsIgnoreCase(entry.alias())
                    || studioSegment.equalsIgnoreCase(entry.studioId().toString()))
        .isPresent();
  }
}
