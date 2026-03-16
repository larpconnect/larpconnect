package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LarpSystemTest {

  private LarpSystem createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(LarpSystem.class.getModifiers())) {
      return new LarpSystem() {};
    }
    java.lang.reflect.Constructor<LarpSystem> ctor = LarpSystem.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Name() throws Exception {
    LarpSystem obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
