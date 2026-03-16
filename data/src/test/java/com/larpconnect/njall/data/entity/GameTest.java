package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GameTest {
  private Game createInstance() {
    return new Game();
  }

  @Test
  void test_Campaign() {
    Game obj = createInstance();
    Campaign val = org.mockito.Mockito.mock(Campaign.class);
    obj.setCampaign(val);
    assertThat(obj.getCampaign()).isEqualTo(val);
  }
}
