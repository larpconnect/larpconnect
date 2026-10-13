package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentEntityTest {

  @Test
  @DisplayName("AgentEntity accessors and mutators operate correctly")
  void agentEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();

    var defaultEntity = new AgentEntity();
    assertThat(defaultEntity.getId()).isNull();

    var entity = new AgentEntity(tenantId, id, ownerId, "valkyrie");
    entity.setPreferredUsername("valkyrie_lead");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getOwnerId()).isEqualTo(ownerId);
    assertThat(entity.getPreferredUsername()).isEqualTo("valkyrie_lead");
  }
}
