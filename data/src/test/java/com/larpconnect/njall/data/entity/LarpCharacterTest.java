package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LarpCharacterTest {
  private LarpCharacter createInstance() {
    return new LarpCharacter();
  }

  @Test
  void test_Campaign() {
    LarpCharacter obj = createInstance();
    Campaign val = org.mockito.Mockito.mock(Campaign.class);
    obj.setCampaign(val);
    assertThat(obj.getCampaign()).isEqualTo(val);
  }

  @Test
  void test_NameTemplate() {
    LarpCharacter obj = createInstance();
    String val = "test";
    obj.setNameTemplate(val);
    assertThat(obj.getNameTemplate()).isEqualTo(val);
  }
}
