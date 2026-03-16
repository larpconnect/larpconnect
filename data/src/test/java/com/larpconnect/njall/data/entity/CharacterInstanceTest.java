package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CharacterInstanceTest {

  private CharacterInstance createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(CharacterInstance.class.getModifiers())) {
      return new CharacterInstance() {};
    }
    java.lang.reflect.Constructor<CharacterInstance> ctor =
        CharacterInstance.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Character() throws Exception {
    CharacterInstance obj = createInstance();
    LarpCharacter val = org.mockito.Mockito.mock(LarpCharacter.class);
    obj.setCharacter(val);
    assertThat(obj.getCharacter()).isEqualTo(val);
  }

  @Test
  void test_Player() throws Exception {
    CharacterInstance obj = createInstance();
    Individual val = org.mockito.Mockito.mock(Individual.class);
    obj.setPlayer(val);
    assertThat(obj.getPlayer()).isEqualTo(val);
  }

  @Test
  void test_Game() throws Exception {
    CharacterInstance obj = createInstance();
    Game val = org.mockito.Mockito.mock(Game.class);
    obj.setGame(val);
    assertThat(obj.getGame()).isEqualTo(val);
  }

  @Test
  void test_IndividualName() throws Exception {
    CharacterInstance obj = createInstance();
    String val = "test";
    obj.setIndividualName(val);
    assertThat(obj.getIndividualName()).isEqualTo(val);
  }
}
