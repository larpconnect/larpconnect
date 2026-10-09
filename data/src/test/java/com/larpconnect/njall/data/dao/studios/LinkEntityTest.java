package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class LinkEntityTest {

  @Test
  @DisplayName("LinkEntity getters and setters operate correctly")
  void linkEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var link = new LinkEntity(tenantId, id, "old", "old", "old");
    link.setLinkType("discord");
    link.setUrl("https://discord.gg");
    link.setMediaType("text/html");

    assertThat(link.getTenantId()).isEqualTo(tenantId);
    assertThat(link.getId()).isEqualTo(id);
    assertThat(link.getLinkType()).isEqualTo("discord");
    assertThat(link.getUrl()).isEqualTo("https://discord.gg");
    assertThat(link.getMediaType()).isEqualTo("text/html");
  }
}
