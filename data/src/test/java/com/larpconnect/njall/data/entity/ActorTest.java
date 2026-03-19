package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class ActorTest {

  @Test
  void testGettersAndSetters() {
    Actor entity = new Actor();

    Entity ownerVal = Mockito.mock(Entity.class);
    entity.setOwner(ownerVal);
    assertThat(entity.getOwner()).isEqualTo(ownerVal);
    String summaryVal = "test";
    entity.setSummary(summaryVal);
    assertThat(entity.getSummary()).isEqualTo(summaryVal);
    String preferredUsernameVal = "test";
    entity.setPreferredUsername(preferredUsernameVal);
    assertThat(entity.getPreferredUsername()).isEqualTo(preferredUsernameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
