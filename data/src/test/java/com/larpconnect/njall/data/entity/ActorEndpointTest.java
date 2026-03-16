package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ActorEndpointTest {

  private ActorEndpoint createInstance() throws Exception {
    if (java.lang.reflect.Modifier.isAbstract(ActorEndpoint.class.getModifiers())) {
      return new ActorEndpoint() {};
    }
    java.lang.reflect.Constructor<ActorEndpoint> ctor =
        ActorEndpoint.class.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  @Test
  void test_Actor() throws Exception {
    ActorEndpoint obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setActor(val);
    assertThat(obj.getActor()).isEqualTo(val);
  }

  @Test
  void test_Name() throws Exception {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }

  @Test
  void test_Actor() throws Exception {
    ActorEndpoint obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setActor(val);
    assertThat(obj.getActor()).isEqualTo(val);
  }

  @Test
  void test_Name() throws Exception {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }

  @Test
  void test_Endpoint() throws Exception {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setEndpoint(val);
    assertThat(obj.getEndpoint()).isEqualTo(val);
  }
}
