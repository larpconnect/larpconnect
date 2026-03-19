package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class StudioRoleTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    StudioRole entity = new StudioRole();

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
