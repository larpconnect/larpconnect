package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {
  private User createInstance() {
    return new User();
  }

  @Test
  void test_Owner() {
    User obj = createInstance();
    Individual val = org.mockito.Mockito.mock(Individual.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Username() {
    User obj = createInstance();
    String val = "test";
    obj.setUsername(val);
    assertThat(obj.getUsername()).isEqualTo(val);
  }
}
