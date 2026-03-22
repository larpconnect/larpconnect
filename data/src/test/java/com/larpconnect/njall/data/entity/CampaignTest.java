package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CampaignTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new Campaign();

    var systemVal = mock(LarpSystem.class);
    entity.setLarpSystem(systemVal);
    assertThat(entity.getLarpSystem()).isEqualTo(systemVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
