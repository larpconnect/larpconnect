package com.larpconnect.njall.data.dao.studios.actors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class AgentEndpointIdTest {

  @Test
  @DisplayName("AgentEndpointId equals and hashCode contract")
  void agentEndpointId_contract() {
    var tenantId = UUID.randomUUID();
    var actorId = UUID.randomUUID();

    var id1 = new AgentEndpointId(tenantId, actorId, "inbox");
    var id2 = new AgentEndpointId(tenantId, actorId, "inbox");
    var id3 = new AgentEndpointId(tenantId, actorId, "outbox");

    assertThat(id1.equals(id1)).isTrue();
    assertThat(id1).isEqualTo(id2);
    assertThat(id1).isNotEqualTo(id3);
    assertThat(id1).isNotEqualTo(new AgentEndpointId(UUID.randomUUID(), actorId, "inbox"));
    assertThat(id1).isNotEqualTo(new AgentEndpointId(tenantId, UUID.randomUUID(), "inbox"));
    assertThat(id1).isNotEqualTo(null);
    assertThat(id1).isNotEqualTo(new Object());
    assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

    var defaultId = new AgentEndpointId();
    defaultId.setTenantId(tenantId);
    defaultId.setActorId(actorId);
    defaultId.setName("inbox");

    assertThat(defaultId.getTenantId()).isEqualTo(tenantId);
    assertThat(defaultId.getActorId()).isEqualTo(actorId);
    assertThat(defaultId.getName()).isEqualTo("inbox");
  }
}
