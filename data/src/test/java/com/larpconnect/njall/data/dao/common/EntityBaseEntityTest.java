package com.larpconnect.njall.data.dao.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EntityBaseEntityTest {

  @Test
  @DisplayName("EntityBaseEntity getters and setters operate correctly")
  void entityBaseEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var now = Instant.now();

    var entity = new EntityBaseEntity(tenantId, id, "Link", null, now, now, null);
    entity.setSummary("Summary");
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
}
