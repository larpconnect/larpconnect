package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LocationTest {

  private Location createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Location.class.getModifiers())) {
      return new Location() {};
    }
    java.lang.reflect.Constructor<Location> ctor = Location.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Address() throws Exception {
    Location obj = createInstance();
    String val = "test";
    obj.setAddress(val);
    assertThat(obj.getAddress()).isEqualTo(val);
  }
}
