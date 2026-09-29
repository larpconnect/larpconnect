package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EntityAndLinkEntityTest {

  @Test
  @DisplayName("EntityBaseEntity getters and setters operate correctly")
  void entityBaseEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var now = Instant.now();

    var entity = new EntityBaseEntity();
    entity.setTenantId(tenantId);
    entity.setId(id);
    entity.setEntityType("Link");
    entity.setSummary("Summary");
    entity.setCreatedOn(now);
    entity.setUpdatedOn(now);
    entity.setDeletedOn(null);

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getEntityType()).isEqualTo("Link");
    assertThat(entity.getSummary()).isEqualTo("Summary");
    assertThat(entity.getCreatedOn()).isEqualTo(now);
    assertThat(entity.getUpdatedOn()).isEqualTo(now);
    assertThat(entity.getDeletedOn()).isNull();

    entity.setDeletedOn(now);
    assertThat(entity.getDeletedOn()).isEqualTo(now);
  }

  @Test
  @DisplayName("LinkEntity getters and setters operate correctly")
  void linkEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();

    var link = new LinkEntity();
    link.setTenantId(tenantId);
    link.setId(id);
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
