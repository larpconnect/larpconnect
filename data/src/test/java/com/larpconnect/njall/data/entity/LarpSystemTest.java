package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class LarpSystemTest {
  private LarpSystem createInstance() {
    return new LarpSystem();
  }

  @Test
  void test_Name() {
    LarpSystem obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
