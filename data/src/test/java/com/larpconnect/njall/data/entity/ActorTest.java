package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ActorTest {

  private Actor createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Actor.class.getModifiers())) {
      return new Actor() {};
    }
    java.lang.reflect.Constructor<Actor> ctor = Actor.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Owner() throws Exception {
    Actor obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Summary() throws Exception {
    Actor obj = createInstance();
    String val = "test";
    obj.setSummary(val);
    assertThat(obj.getSummary()).isEqualTo(val);
  }

  @Test
  void test_PreferredUsername() throws Exception {
    Actor obj = createInstance();
    String val = "test";
    obj.setPreferredUsername(val);
    assertThat(obj.getPreferredUsername()).isEqualTo(val);
  }
}
