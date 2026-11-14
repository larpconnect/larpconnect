package com.larpconnect.njall.data.dao.studios.games;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameParamsTest {

  @Test
  @DisplayName("CreateGameParams exposes all configured creation fields")
  void createGameParams_accessors() {
    var generator = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var campaignId = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var start = Instant.parse("2026-11-20T19:00:00Z");
    var end = Instant.parse("2026-11-20T23:00:00Z");

    var params =
        new CreateGameParams(
            generator,
            systemId,
            Optional.of(campaignId),
            "Midnight Court",
            Optional.of("Opening court session"),
            Optional.of(locationId),
            Optional.of(start),
            Optional.of(end));

    assertThat(params.generator()).isEqualTo(generator);
    assertThat(params.systemId()).isEqualTo(systemId);
    assertThat(params.campaignId()).contains(campaignId);
    assertThat(params.title()).isEqualTo("Midnight Court");
    assertThat(params.summary()).contains("Opening court session");
    assertThat(params.locationId()).contains(locationId);
    assertThat(params.startTime()).contains(start);
    assertThat(params.endTime()).contains(end);
  }

  @Test
  @DisplayName("PatchGameParams 7-arg constructor defaults all clear flags to false")
  void patchGameParams_convenienceConstructor_disablesClearFlags() {
    var systemId = UUID.randomUUID();
    var campaignId = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var start = Instant.parse("2026-11-21T19:00:00Z");
    var end = Instant.parse("2026-11-21T23:00:00Z");

    var params =
        new PatchGameParams(
            Optional.of(systemId),
            Optional.of(campaignId),
            Optional.of("Renamed"),
            Optional.of("Summary"),
            Optional.of(locationId),
            Optional.of(start),
            Optional.of(end));

    assertThat(params)
        .isEqualTo(
            new PatchGameParams(
                Optional.of(systemId),
                Optional.of(campaignId),
                false,
                Optional.of("Renamed"),
                Optional.of("Summary"),
                false,
                Optional.of(locationId),
                false,
                Optional.of(start),
                false,
                Optional.of(end),
                false));
  }
}
