package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.annotations.Immutable;
import com.google.inject.Inject;
import com.google.mu.util.Substring;
import com.larpconnect.njall.common.config.ServerConfig;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** Default host-validating implementation of {@link LocalActorRecipientExtractor}. */
final class DefaultLocalActorRecipientExtractor implements LocalActorRecipientExtractor {

  private static final Substring.Prefix API_STUDIOS_PREFIX = Substring.prefix("/api/studios/");
  private static final Substring.Prefix SHORT_STUDIOS_PREFIX = Substring.prefix("/studios/");
  private static final Substring.Pattern ACTORS_DELIMITER =
      Substring.first("/v1/actors/").or(Substring.first("/actors/"));

  private final AuthoritySpec expectedAuthority;

  @Inject
  DefaultLocalActorRecipientExtractor(ServerConfig serverConfig) {
    this.expectedAuthority = parseConfiguredAuthority(serverConfig.primaryDomain());
  }

  private static AuthoritySpec parseConfiguredAuthority(String rawDomain) {
    String trimmed = rawDomain.trim();
    try {
      URI parsed = URI.create("http://" + trimmed);
      String host = parsed.getHost();
      if (host != null && parsed.getRawUserInfo() == null && parsed.getPath().isEmpty()) {
        return createAuthoritySpec(host.toLowerCase(Locale.ROOT), parsed.getPort());
      }
    } catch (IllegalArgumentException _) {
      // Fall back to literal host comparison if primaryDomain cannot parse as URI authority.
    }
    return createAuthoritySpec(trimmed.toLowerCase(Locale.ROOT), -1);
  }

  private static AuthoritySpec createAuthoritySpec(String host, int port) {
    return new AuthoritySpec(host, port);
  }

  @Override
  public ImmutableSet<String> extract(
      JsonNode activity, String studioIdParam, UUID studioId, Optional<StudioLookup> lookup) {
    var validStudioAliases = buildValidStudioIdentifiers(studioIdParam, studioId, lookup);
    return Stream.of(
            streamArray(activity.get("to")),
            streamArray(activity.get("cc")),
            streamEmbeddedObject(activity.get("object")))
        .flatMap(s -> s)
        .flatMap(uri -> parseActorIdentifier(uri, validStudioAliases).stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  private static ImmutableSet<String> buildValidStudioIdentifiers(
      String studioIdParam, UUID studioId, Optional<StudioLookup> lookup) {
    var builder = ImmutableSet.<String>builder();
    builder.add(studioIdParam.toLowerCase(Locale.ROOT));
    builder.add(studioId.toString().toLowerCase(Locale.ROOT));
    lookup.ifPresent(
        l -> {
          builder.add(l.alias().toLowerCase(Locale.ROOT));
          builder.add(l.studioId().toString().toLowerCase(Locale.ROOT));
        });
    return builder.build();
  }

  private static Stream<String> streamEmbeddedObject(@Nullable JsonNode objectNode) {
    if (objectNode == null) {
      return Stream.empty();
    }
    if (objectNode.isObject()) {
      return Stream.concat(streamArray(objectNode.get("to")), streamArray(objectNode.get("cc")));
    }
    return streamScalarNode(objectNode);
  }

  private static Stream<String> streamArray(@Nullable JsonNode arrayNode) {
    if (arrayNode == null || !arrayNode.isArray()) {
      return Stream.empty();
    }
    return Streams.stream(arrayNode).flatMap(DefaultLocalActorRecipientExtractor::streamScalarNode);
  }

  private static Stream<String> streamScalarNode(@Nullable JsonNode node) {
    if (node == null || !node.isTextual() || node.asText().isBlank()) {
      return Stream.empty();
    }
    return Stream.of(node.asText());
  }

  private Optional<String> parseActorIdentifier(
      String uriString, ImmutableSet<String> validStudioAliases) {
    return extractLocalPath(uriString)
        .flatMap(path -> parsePathIdentifier(path, validStudioAliases));
  }

  private Optional<String> extractLocalPath(String uriString) {
    String trimmed = uriString.trim();
    if (trimmed.startsWith("/") && !trimmed.startsWith("//")) {
      return Optional.of(trimmed);
    }
    return extractHttpLocalPath(trimmed);
  }

  private Optional<String> extractHttpLocalPath(String trimmed) {
    URI uri;
    try {
      uri = URI.create(trimmed);
    } catch (IllegalArgumentException _) {
      return Optional.empty();
    }
    if (!isCleanHierarchicalHttp(uri) || !matchesExpectedAuthority(uri)) {
      return Optional.empty();
    }
    return Optional.ofNullable(uri.getPath()).filter(path -> path.startsWith("/"));
  }

  private boolean matchesExpectedAuthority(URI uri) {
    String host = uri.getHost();
    if (host == null || !expectedAuthority.host().equalsIgnoreCase(host)) {
      return false;
    }
    int uriPort = uri.getPort();
    int defaultPort = defaultSchemePort(uri.getScheme());
    if (expectedAuthority.port() != -1) {
      int effectivePort = uriPort != -1 ? uriPort : defaultPort;
      return effectivePort == expectedAuthority.port();
    }
    return uriPort == -1 || uriPort == defaultPort;
  }

  private static int defaultSchemePort(String scheme) {
    return "https".equalsIgnoreCase(scheme) ? 443 : 80;
  }

  private static boolean isCleanHierarchicalHttp(URI uri) {
    String scheme = uri.getScheme();
    return !uri.isOpaque()
        && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
        && uri.getRawUserInfo() == null
        && uri.getRawQuery() == null
        && uri.getRawFragment() == null;
  }

  private static Optional<String> parsePathIdentifier(
      String path, ImmutableSet<String> validStudioAliases) {
    return stripStudiosPrefix(path)
        .flatMap(ACTORS_DELIMITER::in)
        .flatMap(
            match -> {
              String studioSegment = match.before().toLowerCase(Locale.ROOT);
              String actorSegment = match.after();
              if (!validStudioAliases.contains(studioSegment)
                  || actorSegment.isBlank()
                  || actorSegment.indexOf('/') >= 0) {
                return Optional.empty();
              }
              return Optional.of(actorSegment);
            });
  }

  private static Optional<String> stripStudiosPrefix(String path) {
    if (path.startsWith("/api/studios/")) {
      return Optional.of(API_STUDIOS_PREFIX.removeFrom(path));
    }
    if (path.startsWith("/studios/")) {
      return Optional.of(SHORT_STUDIOS_PREFIX.removeFrom(path));
    }
    return Optional.empty();
  }

  @Immutable
  private record AuthoritySpec(String host, int port) {}
}
