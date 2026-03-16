package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EntityTest {

  private Entity createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(Entity.class.getModifiers())) {
      return new Entity() {};
    }
    java.lang.reflect.Constructor<Entity> ctor = Entity.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Id() throws Exception {
    Entity obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setId(val);
    assertThat(obj.getId()).isEqualTo(val);
  }

  @Test
  void test_EntityType() throws Exception {
    Entity obj = createInstance();
    obj.getEntityType();
  }

  @Test
  void test_ExternalReference() throws Exception {
    Entity obj = createInstance();
    String val = "test";
    obj.setExternalReference(val);
    assertThat(obj.getExternalReference()).isEqualTo(val);
  }

  @Test
  void test_CreatedOn() throws Exception {
    Entity obj = createInstance();
    obj.getCreatedOn();
  }

  @Test
  void test_UpdatedOn() throws Exception {
    Entity obj = createInstance();
    obj.getUpdatedOn();
  }

  @Test
  void test_DeletedOn() throws Exception {
    Entity obj = createInstance();
    obj.getDeletedOn();
  }
}
