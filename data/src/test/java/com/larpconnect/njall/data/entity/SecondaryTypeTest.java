package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SecondaryTypeTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    SecondaryType entity = new SecondaryType();

    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
    String[] typesListVal = new String[] {"test"};
    entity.setTypesList(typesListVal);
    assertThat(entity.getTypesList()).isEqualTo(typesListVal);
  }
}
