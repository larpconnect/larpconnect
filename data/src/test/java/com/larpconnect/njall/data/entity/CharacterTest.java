package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CharacterTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new Character();

    var campaignVal = mock(Campaign.class);
    entity.setCampaign(campaignVal);
    assertThat(entity.getCampaign()).isEqualTo(campaignVal);
    String nameTemplateVal = "test";
    entity.setNameTemplate(nameTemplateVal);
    assertThat(entity.getNameTemplate()).isEqualTo(nameTemplateVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
