package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CharacterInstanceTest {
  private CharacterInstance createInstance() {
    return new CharacterInstance();
  }

  @Test
  void test_Character() {
    CharacterInstance obj = createInstance();
    LarpCharacter val = org.mockito.Mockito.mock(LarpCharacter.class);
    obj.setCharacter(val);
    assertThat(obj.getCharacter()).isEqualTo(val);
  }

  @Test
  void test_Player() {
    CharacterInstance obj = createInstance();
    Individual val = org.mockito.Mockito.mock(Individual.class);
    obj.setPlayer(val);
    assertThat(obj.getPlayer()).isEqualTo(val);
  }

  @Test
  void test_Game() {
    CharacterInstance obj = createInstance();
    Game val = org.mockito.Mockito.mock(Game.class);
    obj.setGame(val);
    assertThat(obj.getGame()).isEqualTo(val);
  }

  @Test
  void test_IndividualName() {
    CharacterInstance obj = createInstance();
    String val = "test";
    obj.setIndividualName(val);
    assertThat(obj.getIndividualName()).isEqualTo(val);
  }
}
