package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class CampaignTest {

  @Test
  void testGettersAndSetters() {
    Campaign entity = new Campaign();

    System systemVal = Mockito.mock(System.class);
    entity.setSystem(systemVal);
    assertThat(entity.getSystem()).isEqualTo(systemVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
