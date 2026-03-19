package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class UserTest {

  @Test
  void testGettersAndSetters() {
    User entity = new User();

    String usernameVal = "test";
    entity.setUsername(usernameVal);
    assertThat(entity.getUsername()).isEqualTo(usernameVal);
    UUID idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
