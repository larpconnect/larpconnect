package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CharacterTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    Character entity = new Character();

    Campaign campaignVal = mock(Campaign.class);
    entity.setCampaign(campaignVal);
    assertThat(entity.getCampaign()).isEqualTo(campaignVal);
    String nameTemplateVal = "test";
    entity.setNameTemplate(nameTemplateVal);
    assertThat(entity.getNameTemplate()).isEqualTo(nameTemplateVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
