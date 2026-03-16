package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CampaignTest {
  private Campaign createInstance() {
    return new Campaign();
  }

  @Test
  void test_System() {
    Campaign obj = createInstance();
    LarpSystem val = org.mockito.Mockito.mock(LarpSystem.class);
    obj.setSystem(val);
    assertThat(obj.getSystem()).isEqualTo(val);
  }
}
