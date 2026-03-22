package com.larpconnect.njall.data.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RoleTest {

  @Test
  void gettersAndSetters_validInput_returnsExpected() {
    var entity = new Role();

    String roleNameVal = "test";
    entity.setRoleName(roleNameVal);
    assertThat(entity.getRoleName()).isEqualTo(roleNameVal);
    var idVal = UUID.randomUUID();
    entity.setId(idVal);
    assertThat(entity.getId()).isEqualTo(idVal);
  }
}
