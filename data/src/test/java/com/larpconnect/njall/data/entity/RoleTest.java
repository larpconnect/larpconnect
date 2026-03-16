package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class RoleTest {

  private Role createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Role.class.getModifiers())) {
      return new Role() {};
    }
    java.lang.reflect.Constructor<Role> ctor = Role.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_RoleName() throws Exception {
    Role obj = createInstance();
    String val = "test";
    obj.setRoleName(val);
    assertThat(obj.getRoleName()).isEqualTo(val);
  }
}
