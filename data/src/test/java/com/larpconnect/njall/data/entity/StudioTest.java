package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StudioTest {

  private Studio createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Studio.class.getModifiers())) {
      return new Studio() {};
    }
    java.lang.reflect.Constructor<Studio> ctor = Studio.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Name() throws Exception {
    Studio obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
