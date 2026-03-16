package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LarpCharacterTest {

  private LarpCharacter createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(LarpCharacter.class.getModifiers())) {
      return new LarpCharacter() {};
    }
    java.lang.reflect.Constructor<LarpCharacter> ctor =
        LarpCharacter.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Campaign() throws Exception {
    LarpCharacter obj = createInstance();
    Campaign val = org.mockito.Mockito.mock(Campaign.class);
    obj.setCampaign(val);
    assertThat(obj.getCampaign()).isEqualTo(val);
  }

  @Test
  void test_NameTemplate() throws Exception {
    LarpCharacter obj = createInstance();
    String val = "test";
    obj.setNameTemplate(val);
    assertThat(obj.getNameTemplate()).isEqualTo(val);
  }
}
