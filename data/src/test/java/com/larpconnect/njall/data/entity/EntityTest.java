package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EntityTest {
  private Entity createInstance() {
    return new Entity();
  }

  @Test
  void test_Id() {
    Entity obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setId(val);
    assertThat(obj.getId()).isEqualTo(val);
  }

  @Test
  void test_EntityType() {
    Entity obj = createInstance();
    obj.getEntityType();
  }

  @Test
  void test_ExternalReference() {
    Entity obj = createInstance();
    String val = "test";
    obj.setExternalReference(val);
    assertThat(obj.getExternalReference()).isEqualTo(val);
  }

  @Test
  void test_CreatedOn() {
    Entity obj = createInstance();
    obj.getCreatedOn();
  }

  @Test
  void test_UpdatedOn() {
    Entity obj = createInstance();
    obj.getUpdatedOn();
  }

  @Test
  void test_DeletedOn() {
    Entity obj = createInstance();
    obj.getDeletedOn();
  }
}
