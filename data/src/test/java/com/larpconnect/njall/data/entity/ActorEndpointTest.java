package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class ActorEndpointTest {

  @Test
  void testGettersAndSetters() {
    ActorEndpoint entity = new ActorEndpoint();

    UUID actorVal = UUID.randomUUID();
    entity.setActor(actorVal);
    assertThat(entity.getActor()).isEqualTo(actorVal);
    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    Actor actorVal = Mockito.mock(Actor.class);
    entity.setActor(actorVal);
    assertThat(entity.getActor()).isEqualTo(actorVal);
    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    String endpointVal = "test";
    entity.setEndpoint(endpointVal);
    assertThat(entity.getEndpoint()).isEqualTo(endpointVal);
  }
}
