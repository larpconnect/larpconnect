package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ActorEndpointTest {
  private ActorEndpoint createInstance() {
    return new ActorEndpoint();
  }

  @Test
  void test_Actor() {
    ActorEndpoint obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setActor(val);
    assertThat(obj.getActor()).isEqualTo(val);
  }

  @Test
  void test_Name() {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }

  @Test
  void test_Actor() {
    ActorEndpoint obj = createInstance();
    UUID val = java.util.UUID.randomUUID();
    obj.setActor(val);
    assertThat(obj.getActor()).isEqualTo(val);
  }

  @Test
  void test_Name() {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setName(val);
    assertThat(obj.getName()).isEqualTo(val);
  }

  @Test
  void test_Endpoint() {
    ActorEndpoint obj = createInstance();
    String val = "test";
    obj.setEndpoint(val);
    assertThat(obj.getEndpoint()).isEqualTo(val);
  }
}
