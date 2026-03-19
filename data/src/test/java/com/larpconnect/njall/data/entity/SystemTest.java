package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SystemTest {

  @Test
  void testGettersAndSetters() {
    System entity = new System();

    String nameVal = "test";
    entity.setName(nameVal);
    assertThat(entity.getName()).isEqualTo(nameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
