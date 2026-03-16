package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CollectionItemTest {

  private CollectionItem createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(CollectionItem.class.getModifiers())) {
      return new CollectionItem() {};
    }
    java.lang.reflect.Constructor<CollectionItem> ctor =
        CollectionItem.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_ImplicitId() throws Exception {
    CollectionItem obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setImplicitId(val);
    assertThat(obj.getImplicitId()).isEqualTo(val);
  }

  @Test
  void test_Collection() throws Exception {
    CollectionItem obj = createInstance();
    Collection val = org.mockito.Mockito.mock(Collection.class);
    obj.setCollection(val);
    assertThat(obj.getCollection()).isEqualTo(val);
  }

  @Test
  void test_AddedOn() throws Exception {
    CollectionItem obj = createInstance();
    obj.getAddedOn();
  }

  @Test
  void test_RefersTo() throws Exception {
    CollectionItem obj = createInstance();
    Entity val = org.mockito.Mockito.mock(Entity.class);
    obj.setRefersTo(val);
    assertThat(obj.getRefersTo()).isEqualTo(val);
  }
}
