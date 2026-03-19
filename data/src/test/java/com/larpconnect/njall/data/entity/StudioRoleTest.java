package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class StudioRoleTest {

  @Test
  void testGettersAndSetters() {
    StudioRole entity = new StudioRole();

    UUID studioVal = UUID.randomUUID();
    entity.setStudio(studioVal);
    assertThat(entity.getStudio()).isEqualTo(studioVal);
    UUID roleVal = UUID.randomUUID();
    entity.setRole(roleVal);
    assertThat(entity.getRole()).isEqualTo(roleVal);
    UUID individualVal = UUID.randomUUID();
    entity.setIndividual(individualVal);
    assertThat(entity.getIndividual()).isEqualTo(individualVal);
    Studio studioVal = Mockito.mock(Studio.class);
    entity.setStudio(studioVal);
    assertThat(entity.getStudio()).isEqualTo(studioVal);
    Role roleVal = Mockito.mock(Role.class);
    entity.setRole(roleVal);
    assertThat(entity.getRole()).isEqualTo(roleVal);
    Individual individualVal = Mockito.mock(Individual.class);
    entity.setIndividual(individualVal);
    assertThat(entity.getIndividual()).isEqualTo(individualVal);
  }
}
