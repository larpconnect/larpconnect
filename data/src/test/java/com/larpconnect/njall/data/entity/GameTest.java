package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class GameTest {

  @Test
  void testGettersAndSetters() {
    Game entity = new Game();

    Campaign campaignVal = Mockito.mock(Campaign.class);
    entity.setCampaign(campaignVal);
    assertThat(entity.getCampaign()).isEqualTo(campaignVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
