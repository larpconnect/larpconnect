package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GameTest {

  private Game createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Game.class.getModifiers())) {
      return new Game() {};
    }
    java.lang.reflect.Constructor<Game> ctor = Game.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Campaign() throws Exception {
    Game obj = createInstance();
    Campaign val = org.mockito.Mockito.mock(Campaign.class);
    obj.setCampaign(val);
    assertThat(obj.getCampaign()).isEqualTo(val);
  }
}
