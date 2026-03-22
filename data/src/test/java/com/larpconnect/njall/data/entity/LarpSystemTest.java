package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SystemTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new LarpSystem();

    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
