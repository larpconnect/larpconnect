package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LocationTest {
  private Location createInstance() {
    return new Location();
  }

  @Test
  void test_Address() {
    Location obj = createInstance();
    String val = "test";
    obj.setAddress(val);
    assertThat(obj.getAddress()).isEqualTo(val);
  }
}
