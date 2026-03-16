package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CampaignTest {

  private Campaign createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Campaign.class.getModifiers())) {
      return new Campaign() {};
    }
    java.lang.reflect.Constructor<Campaign> ctor = Campaign.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_System() throws Exception {
    Campaign obj = createInstance();
    LarpSystem val = org.mockito.Mockito.mock(LarpSystem.class);
    obj.setSystem(val);
    assertThat(obj.getSystem()).isEqualTo(val);
  }
}
