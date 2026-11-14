package com.larpconnect.njall.api.studios.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GeneratorUriSupportTest {

  private static final String ALIAS = "valiant";
  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final StudioLookup lookup =
      new StudioLookup(tenantId, studioId, ALIAS, Instant.now(), Instant.now(), Optional.empty());

  @Test
  @DisplayName("formatGeneratorUri formats canonical studio actor URI path")
  void formatGeneratorUri_success() {
    var uri = GeneratorUriSupport.formatGeneratorUri(ALIAS, actorId);
    assertThat(uri).isEqualTo("/api/studios/valiant/v1/actors/" + actorId);

    var uuidUri = GeneratorUriSupport.formatGeneratorUri(studioId.toString(), actorId);
    assertThat(uuidUri).isEqualTo("/api/studios/" + studioId + "/v1/actors/" + actorId);
  }

  @Test
  @DisplayName("parseGenerator accepts bare UUID string")
  void parseGenerator_bareUuid_success() {
    var result = GeneratorUriSupport.parseGenerator(actorId.toString(), ALIAS, lookup);
    assertThat(result).contains(actorId);

    var withoutLookup = GeneratorUriSupport.parseGenerator(actorId.toString(), ALIAS);
    assertThat(withoutLookup).contains(actorId);
  }

  @Test
  @DisplayName("parseGenerator accepts canonical URI matching studio alias")
  void parseGenerator_canonicalUriMatchingAlias_success() {
    var uri = "/api/studios/valiant/v1/actors/" + actorId;
    var result = GeneratorUriSupport.parseGenerator(uri, ALIAS, lookup);
    assertThat(result).contains(actorId);
  }

  @Test
  @DisplayName("parseGenerator accepts canonical URI matching studio public UUID")
  void parseGenerator_canonicalUriMatchingStudioUuid_success() {
    var uri = "/api/studios/" + studioId + "/v1/actors/" + actorId;
    var result = GeneratorUriSupport.parseGenerator(uri, ALIAS, lookup);
    assertThat(result).contains(actorId);
  }

  @Test
  @DisplayName("parseGenerator accepts canonical URI matching studio identifier without lookup")
  void parseGenerator_canonicalUriMatchingIdentifierWithoutLookup_success() {
    var uri = "/api/studios/valiant/v1/actors/" + actorId;
    var result = GeneratorUriSupport.parseGenerator(uri, ALIAS);
    assertThat(result).contains(actorId);
  }

  @Test
  @DisplayName("parseGenerator rejects null or blank inputs")
  void parseGenerator_blankOrNull_empty() {
    assertThat(GeneratorUriSupport.parseGenerator(null, ALIAS, lookup)).isEmpty();
    assertThat(GeneratorUriSupport.parseGenerator("", ALIAS, lookup)).isEmpty();
    assertThat(GeneratorUriSupport.parseGenerator("   ", ALIAS, lookup)).isEmpty();
  }

  @Test
  @DisplayName("parseGenerator rejects malformed input")
  void parseGenerator_malformed_empty() {
    assertThat(GeneratorUriSupport.parseGenerator("not-a-uuid", ALIAS, lookup)).isEmpty();
    assertThat(
            GeneratorUriSupport.parseGenerator(
                "/api/studios/valiant/v1/actors/invalid-uuid", ALIAS, lookup))
        .isEmpty();
  }

  @Test
  @DisplayName("parseGenerator rejects cross-studio generator URI")
  void parseGenerator_crossStudio_empty() {
    var otherStudioUri = "/api/studios/other-studio/v1/actors/" + actorId;
    assertThat(GeneratorUriSupport.parseGenerator(otherStudioUri, ALIAS, lookup)).isEmpty();
    assertThat(GeneratorUriSupport.parseGenerator(otherStudioUri, ALIAS)).isEmpty();
  }

  @Test
  @DisplayName("parseGenerator rejects non-actor canonical URI")
  void parseGenerator_nonActorUri_empty() {
    var eventUri = "/api/studios/valiant/v1/events/" + actorId;
    assertThat(GeneratorUriSupport.parseGenerator(eventUri, ALIAS, lookup)).isEmpty();
  }

  @Test
  @DisplayName("parseGenerator accepts Optional<StudioLookup> overload")
  void parseGenerator_optionalLookup_success() {
    var uri = "/api/studios/" + studioId + "/v1/actors/" + actorId;
    var result = GeneratorUriSupport.parseGenerator(uri, ALIAS, Optional.of(lookup));
    assertThat(result).contains(actorId);

    var emptyResult = GeneratorUriSupport.parseGenerator(uri, ALIAS, Optional.empty());
    assertThat(emptyResult).isEmpty();
  }
}
