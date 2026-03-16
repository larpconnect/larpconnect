package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StudioTest {
  private Studio createInstance() {
    return new Studio();
  }

  @Test
  void test_Name() {
    Studio obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
