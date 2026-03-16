package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UserTest {

  private User createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(User.class.getModifiers())) {
      return new User() {};
    }
    java.lang.reflect.Constructor<User> ctor = User.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Owner() throws Exception {
    User obj = createInstance();
    Individual val = org.mockito.Mockito.mock(Individual.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Username() throws Exception {
    User obj = createInstance();
    String val = "test";
    obj.setUsername(val);
    assertThat(obj.getUsername()).isEqualTo(val);
  }
}
