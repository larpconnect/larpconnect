package com.larpconnect.njall.api.studios.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class StudioResourceUriSupportTest {

  private static final String ALIAS = "valiant";
  private static final String SYSTEMS = StudioResourceUriSupport.SYSTEMS_COLLECTION;
  private static final String CAMPAIGNS = StudioResourceUriSupport.CAMPAIGNS_COLLECTION;
  private static final String GAMES = StudioResourceUriSupport.GAMES_COLLECTION;
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private final UUID tenantId = UUID.randomUUID();
  private final UUID studioId = UUID.randomUUID();
  private final UUID resourceId = UUID.randomUUID();
  private final StudioLookup lookup =
      new StudioLookup(tenantId, studioId, ALIAS, NOW, NOW, Optional.empty());

  @Test
  void formatResourceUri_validStudioIdentifierAndCollection_formatsCanonicalPath() {
    var aliasUri = StudioResourceUriSupport.formatResourceUri(ALIAS, SYSTEMS, resourceId);
    var uuidUri =
        StudioResourceUriSupport.formatResourceUri(studioId.toString(), CAMPAIGNS, resourceId);

    assertThat(aliasUri).isEqualTo("/api/studios/valiant/v1/systems/" + resourceId);
    assertThat(uuidUri).isEqualTo("/api/studios/" + studioId + "/v1/campaigns/" + resourceId);
  }

  @Test
  void formatResourceUri_invalidStudioOrCollection_throwsIllegalArgumentException() {
    assertThatThrownBy(() -> StudioResourceUriSupport.formatResourceUri("", SYSTEMS, resourceId))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StudioResourceUriSupport.formatResourceUri("a/b", SYSTEMS, resourceId))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StudioResourceUriSupport.formatResourceUri(ALIAS, "  ", resourceId))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StudioResourceUriSupport.formatResourceUri(ALIAS, "a/b", resourceId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseResourceUri_bareUuid_returnsUuid() {
    var raw = "  " + resourceId.toString().toUpperCase(Locale.ROOT) + "  ";

    assertThat(StudioResourceUriSupport.parseResourceUri(raw, ALIAS, GAMES, lookup))
        .isEqualTo(resourceId);
    assertThat(StudioResourceUriSupport.parseResourceUri(resourceId.toString(), ALIAS, GAMES))
        .isEqualTo(resourceId);
  }

  @Test
  void parseResourceUri_sameStudioCanonicalUri_returnsUuid() {
    var aliasUri = StudioResourceUriSupport.formatResourceUri(ALIAS, SYSTEMS, resourceId);
    var upperAliasUri = StudioResourceUriSupport.formatResourceUri("VALIANT", SYSTEMS, resourceId);
    var uuidUri =
        StudioResourceUriSupport.formatResourceUri(studioId.toString(), SYSTEMS, resourceId);

    assertThat(StudioResourceUriSupport.parseResourceUri(aliasUri, ALIAS, SYSTEMS))
        .isEqualTo(resourceId);
    assertThat(StudioResourceUriSupport.parseResourceUri(upperAliasUri, ALIAS, SYSTEMS))
        .isEqualTo(resourceId);
    assertThat(StudioResourceUriSupport.parseResourceUri(uuidUri, ALIAS, SYSTEMS, lookup))
        .isEqualTo(resourceId);
    assertThat(
            StudioResourceUriSupport.parseResourceUri(
                aliasUri, studioId.toString(), SYSTEMS, lookup))
        .isEqualTo(resourceId);
  }

  @Test
  void parseResourceUri_nullOrBlank_throwsIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseResourceUri(
                    null, ALIAS, SYSTEMS, (StudioLookup) null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseResourceUri(
                    "   ", ALIAS, SYSTEMS, Optional.of(lookup)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StudioResourceUriSupport.parseResourceUri(ALIAS, ALIAS, " ", lookup))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseResourceUri_malformedInput_throwsIllegalArgumentException() {
    assertThatThrownBy(
            () -> StudioResourceUriSupport.parseResourceUri("not-a-uuid", ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> StudioResourceUriSupport.parseResourceUri("1-2-3-4-5", ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseResourceUri(
                    "api/studios/valiant/v1/systems/" + resourceId, ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseResourceUri(
                    "/api/studios/valiant/v1/systems/" + resourceId + "/extra",
                    ALIAS,
                    SYSTEMS,
                    lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseResourceUri(
                    "/api/studios/valiant/v2/systems/" + resourceId, ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseResourceUri_wrongCollectionOrCrossStudio_throwsIllegalArgumentException() {
    var campaignUri = StudioResourceUriSupport.formatResourceUri(ALIAS, CAMPAIGNS, resourceId);
    var otherUri = StudioResourceUriSupport.formatResourceUri("other_studio", SYSTEMS, resourceId);

    assertThatThrownBy(
            () -> StudioResourceUriSupport.parseResourceUri(campaignUri, ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> StudioResourceUriSupport.parseResourceUri(otherUri, ALIAS, SYSTEMS, lookup))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> StudioResourceUriSupport.parseResourceUri(otherUri, ALIAS, SYSTEMS))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void parseOptionalResourceUri_handlesEmptyPresentAndInvalidInputs() {
    var systemUri = StudioResourceUriSupport.formatResourceUri(ALIAS, SYSTEMS, resourceId);

    assertThat(StudioResourceUriSupport.parseOptionalResourceUri(Optional.empty(), ALIAS, SYSTEMS))
        .isEmpty();
    assertThat(
            StudioResourceUriSupport.parseOptionalResourceUri(
                Optional.empty(), ALIAS, SYSTEMS, lookup))
        .isEmpty();
    assertThat(
            StudioResourceUriSupport.parseOptionalResourceUri(
                Optional.of(systemUri), ALIAS, SYSTEMS, Optional.of(lookup)))
        .contains(resourceId);
    assertThat(
            StudioResourceUriSupport.parseOptionalResourceUri(
                Optional.of(systemUri), ALIAS, SYSTEMS, (StudioLookup) null))
        .contains(resourceId);
    assertThatThrownBy(
            () ->
                StudioResourceUriSupport.parseOptionalResourceUri(
                    Optional.of("/api/studios/other/v1/systems/" + resourceId),
                    ALIAS,
                    SYSTEMS,
                    lookup))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
