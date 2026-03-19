package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class LocationTest {

  @Test
  void testGettersAndSetters() {
    Location entity = new Location();

    String addressVal = "test";
    entity.setAddress(addressVal);
    assertThat(entity.getAddress()).isEqualTo(addressVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
