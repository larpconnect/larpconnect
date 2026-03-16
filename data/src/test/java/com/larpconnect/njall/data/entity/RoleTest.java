package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RoleTest {
  private Role createInstance() {
    return new Role();
  }

  @Test
  void test_RoleName() {
    Role obj = createInstance();
    String val = "test";
    obj.setRoleName(val);
    assertThat(obj.getRoleName()).isEqualTo(val);
  }
}
