package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CollectionTest {

  private Collection createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Collection.class.getModifiers())) {
      return new Collection() {};
    }
    java.lang.reflect.Constructor<Collection> ctor = Collection.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Owner() throws Exception {
    Collection obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setOwner(val);
    assertThat(obj.getOwner()).isEqualTo(val);
  }

  @Test
  void test_Name() throws Exception {
    Collection obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }
}
