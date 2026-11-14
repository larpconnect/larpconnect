package com.larpconnect.njall.data.dao.studios.games;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameEntityTest {

  @Test
  @DisplayName("GameEntity getters and setters operate correctly")
  void gameEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var newSystemId = UUID.randomUUID();
    var campaignId = UUID.randomUUID();

    var defaultEntity = new GameEntity();
    assertThat(defaultEntity.getId()).isNull();

    var game = new GameEntity(tenantId, id, systemId, campaignId);
    game.setSystemId(newSystemId);
    game.setCampaignId(null);

    assertThat(game.getTenantId()).isEqualTo(tenantId);
    assertThat(game.getId()).isEqualTo(id);
    assertThat(game.getSystemId()).isEqualTo(newSystemId);
    assertThat(game.getCampaignId()).isNull();
  }

  @Test
  @DisplayName("GameEntity constructor preserves system and campaign identifiers")
  void gameEntity_constructor_preservesFields() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var systemId = UUID.randomUUID();
    var campaignId = UUID.randomUUID();

    var game = new GameEntity(tenantId, id, systemId, campaignId);

    assertThat(game.getTenantId()).isEqualTo(tenantId);
    assertThat(game.getId()).isEqualTo(id);
    assertThat(game.getSystemId()).isEqualTo(systemId);
    assertThat(game.getCampaignId()).isEqualTo(campaignId);
  }
}
