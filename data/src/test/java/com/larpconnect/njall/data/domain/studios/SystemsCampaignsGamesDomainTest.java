package com.larpconnect.njall.data.domain.studios;

import static org.assertj.core.api.Assertions.assertThat;

import com.larpconnect.njall.data.domain.common.DatabaseObject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class SystemsCampaignsGamesDomainTest {

  @Test
  @DisplayName("GameSystem constructors retain fields and track soft deletion")
  void gameSystem_constructorsAndHelpers() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var now = Instant.now();

    var active = new GameSystem(id, generator, "Call of Cthulhu", "Horror RPG", now, now, null);
    assertThat(active).isInstanceOf(DatabaseObject.class);
    assertThat(active.id()).isEqualTo(id);
    assertThat(active.generator()).isEqualTo(generator);
    assertThat(active.name()).isEqualTo("Call of Cthulhu");
    assertThat(active.summary()).contains("Horror RPG");
    assertThat(active.createdOn()).isEqualTo(now);
    assertThat(active.updatedOn()).isEqualTo(now);
    assertThat(active.deletedOn()).isEmpty();
    assertThat(active.isDeleted()).isFalse();

    var deleted =
        new GameSystem(id, generator, "Delta Green", Optional.empty(), now, now, Optional.of(now));
    assertThat(deleted.summary()).isEmpty();
    assertThat(deleted.isDeleted()).isTrue();
    assertThat(deleted.deletedOn()).contains(now);
  }

  @Test
  @DisplayName("Campaign constructors retain fields and track optional system and deletion")
  void campaign_constructorsAndHelpers() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var now = Instant.now();

    var withSystem =
        new Campaign(id, generator, systemId, "Seattle By Night", "Chronicle", now, now, null);
    assertThat(withSystem).isInstanceOf(DatabaseObject.class);
    assertThat(withSystem.id()).isEqualTo(id);
    assertThat(withSystem.generator()).isEqualTo(generator);
    assertThat(withSystem.systemId()).contains(systemId);
    assertThat(withSystem.title()).isEqualTo("Seattle By Night");
    assertThat(withSystem.summary()).contains("Chronicle");
    assertThat(withSystem.createdOn()).isEqualTo(now);
    assertThat(withSystem.updatedOn()).isEqualTo(now);
    assertThat(withSystem.isDeleted()).isFalse();

    var systemlessDeleted =
        new Campaign(
            id,
            generator,
            Optional.empty(),
            "Anthology",
            Optional.empty(),
            now,
            now,
            Optional.of(now));
    assertThat(systemlessDeleted.systemId()).isEmpty();
    assertThat(systemlessDeleted.summary()).isEmpty();
    assertThat(systemlessDeleted.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("Game nullable constructor retains fields and exposes EventType.GAME")
  void game_nullableConstructorAndHelpers() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var campaignId = UUID.randomUUID();
    var locationId = UUID.randomUUID();
    var now = Instant.now();
    var start = now.plusSeconds(3600);
    var end = start.plusSeconds(7200);

    var game =
        new Game(
            id,
            generator,
            systemId,
            campaignId,
            locationId,
            "Court Session 1",
            "Opening court",
            start,
            end,
            now,
            now,
            null);
    assertThat(game).isInstanceOf(DatabaseObject.class);
    assertThat(game.id()).isEqualTo(id);
    assertThat(game.generator()).isEqualTo(generator);
    assertThat(game.systemId()).isEqualTo(systemId);
    assertThat(game.campaignId()).contains(campaignId);
    assertThat(game.locationId()).contains(locationId);
    assertThat(game.title()).isEqualTo("Court Session 1");
    assertThat(game.summary()).contains("Opening court");
    assertThat(game.startTime()).contains(start);
    assertThat(game.endTime()).contains(end);
    assertThat(game.createdOn()).isEqualTo(now);
    assertThat(game.updatedOn()).isEqualTo(now);
    assertThat(game.eventType()).isEqualTo(EventType.GAME);
    assertThat(game.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Game canonical constructor supports empty optionals and deletion")
  void game_canonicalConstructorMinimalAndDeleted() {
    var id = UUID.randomUUID();
    var generator = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var now = Instant.now();

    var minimalDeleted =
        new Game(
            id,
            generator,
            systemId,
            Optional.empty(),
            Optional.empty(),
            "One-Shot",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            now,
            now,
            Optional.of(now));
    assertThat(minimalDeleted.campaignId()).isEmpty();
    assertThat(minimalDeleted.locationId()).isEmpty();
    assertThat(minimalDeleted.summary()).isEmpty();
    assertThat(minimalDeleted.startTime()).isEmpty();
    assertThat(minimalDeleted.endTime()).isEmpty();
    assertThat(minimalDeleted.eventType()).isEqualTo(EventType.GAME);
    assertThat(minimalDeleted.isDeleted()).isTrue();
  }
}
