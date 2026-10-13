package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentEndpointEntityTest {

  @Test
  @DisplayName("AgentEndpointEntity accessors operate correctly")
  void agentEndpointEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var actorId = UUID.randomUUID();

    var defaultEntity = new AgentEndpointEntity();
    assertThat(defaultEntity.getName()).isNull();

    var entity =
        new AgentEndpointEntity(tenantId, actorId, "sharedInbox", "https://example.com/inbox");
    entity.setEndpoint("https://example.com/newInbox");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getActorId()).isEqualTo(actorId);
    assertThat(entity.getName()).isEqualTo("sharedInbox");
    assertThat(entity.getEndpoint()).isEqualTo("https://example.com/newInbox");
  }
}
