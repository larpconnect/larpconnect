package com.larpconnect.njall.data.dao.studios.systems;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class GameSystemEntityTest {

  @Test
  @DisplayName("No-arg constructor creates entity with default null fields")
  void noArgConstructor_createsDefaultEntity() {
    var entity = createEmptyEntity();

    assertThat(entity.getName()).isNull();
  }

  @Test
  @DisplayName("Parameterized constructor initializes tenantId, id, and name")
  void parameterizedConstructor_setsAllFields() {
    var tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var id = UUID.fromString("00000000-0000-0000-0000-000000000002");

    var entity = createEntity(tenantId, id, "Dungeons & Dragons");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("Dungeons & Dragons");
  }

  @Test
  @DisplayName("setName updates the mutable system name")
  void setName_updatesName() {
    var tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var entity = createEntity(tenantId, id, "Old System Name");

    entity.setName("Updated System Name");

    assertThat(entity.getName()).isEqualTo("Updated System Name");
  }

  private static GameSystemEntity createEmptyEntity() {
    return new GameSystemEntity();
  }

  private static GameSystemEntity createEntity(UUID tenantId, UUID id, String name) {
    return new GameSystemEntity(tenantId, id, name);
  }
}
