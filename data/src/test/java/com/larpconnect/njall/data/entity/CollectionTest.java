package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CollectionTest {
  private Collection createInstance() {
    return new Collection();
  }

  @Test
  void test_Owner() {
    Collection obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Name() {
    Collection obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
