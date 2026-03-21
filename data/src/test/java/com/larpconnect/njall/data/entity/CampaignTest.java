package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CampaignTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    Campaign entity = new Campaign();

    LarpSystem systemVal = Mockito.mock(LarpSystem.class);
    entity.setLarpSystem(systemVal);
    assertThat(entity.getLarpSystem()).isEqualTo(systemVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
