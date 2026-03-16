package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ActorTest {
  private Actor createInstance() {
    return new Actor();
  }

  @Test
  void test_Owner() {
    Actor obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Summary() {
    Actor obj = createInstance();
    String val = "test";
    obj.setSummary(val);
    assertThat(obj.getSummary()).isEqualTo(val);
  }

  @Test
  void test_PreferredUsername() {
    Actor obj = createInstance();
    String val = "test";
    obj.setPreferredUsername(val);
    assertThat(obj.getPreferredUsername()).isEqualTo(val);
  }
}
