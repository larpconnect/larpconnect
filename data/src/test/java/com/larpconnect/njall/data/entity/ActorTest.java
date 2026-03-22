package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ActorTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new Actor();

    var ownerVal = mock(Entity.class);
    entity.setOwner(ownerVal);
    assertThat(entity.getOwner()).isEqualTo(ownerVal);
    String summaryVal = "test";
    entity.setSummary(summaryVal);
    assertThat(entity.getSummary()).isEqualTo(summaryVal);
    String preferredUsernameVal = "test";
    entity.setPreferredUsername(preferredUsernameVal);
    assertThat(entity.getPreferredUsername()).isEqualTo(preferredUsernameVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
