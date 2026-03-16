package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class IndividualTest {
  private Individual createInstance() {
    return new Individual() {};
  }

  @Test
  void test_Name() {
    Individual obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
