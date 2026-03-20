package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ObjectStreamClass;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class ActorEndpointTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    ActorEndpoint entity = new ActorEndpoint();

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

  @Test
  void actorEndpointId_equalsAndHashCode_validInput_returnsExpected() {
    UUID actor = UUID.randomUUID();
    String name = "endpoint";

    ActorEndpoint.ActorEndpointId id1 = new ActorEndpoint.ActorEndpointId();
    ActorEndpoint.ActorEndpointId id2 = new ActorEndpoint.ActorEndpointId(actor, name);
    ActorEndpoint.ActorEndpointId id3 = new ActorEndpoint.ActorEndpointId(actor, name);
    ActorEndpoint.ActorEndpointId id4 = new ActorEndpoint.ActorEndpointId(UUID.randomUUID(), name);
    ActorEndpoint.ActorEndpointId id5 = new ActorEndpoint.ActorEndpointId(actor, "otherEndpoint");
    ActorEndpoint.ActorEndpointId id6 = new ActorEndpoint.ActorEndpointId(null, name);
    ActorEndpoint.ActorEndpointId id7 = new ActorEndpoint.ActorEndpointId(actor, null);
    ActorEndpoint.ActorEndpointId id8 = new ActorEndpoint.ActorEndpointId();
    ActorEndpoint.ActorEndpointId id9 = new ActorEndpoint.ActorEndpointId(actor, name);
    ActorEndpoint.ActorEndpointId id10 = new ActorEndpoint.ActorEndpointId(null, null);

    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).isEqualTo(id8);
    assertThat(id1).isNotEqualTo(new Actor()); // Test against different class type

    assertThat(id2).isEqualTo(id3);
    assertThat(id2).isEqualTo(id9);
    assertThat(id2).isNotEqualTo(id4);
    assertThat(id2).isNotEqualTo(id5);
    assertThat(id2).isNotEqualTo(id6);
    assertThat(id2).isNotEqualTo(id7);
    assertThat(id2).isNotEqualTo(id10);
    assertThat(id6).isNotEqualTo(id2);
    assertThat(id7).isNotEqualTo(id2);
    assertThat(id10).isNotEqualTo(id2);
    assertThat(id2).isNotEqualTo(null);
    assertThat(id2).isNotEqualTo(new Object());

    // Explicitly hit identical self check
    assertThat(id2.equals(id2)).isTrue();

    assertThat(id2.hashCode()).isEqualTo(id3.hashCode());
    assertThat(id2.hashCode()).isNotEqualTo(id4.hashCode());
  }

  @Test
  void actorEndpointId_serialVersionUID_validInput_returnsExpected() {
    long serialVersionUID =
        ObjectStreamClass.lookup(ActorEndpoint.ActorEndpointId.class).getSerialVersionUID();
    assertThat(serialVersionUID).isEqualTo(1L);
  }
}
