package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class HashtagEntityTest {

  @Test
  @DisplayName("HashtagEntity getters and setters work correctly")
  void hashtagEntity_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var entity = new HashtagEntity(tenantId, id, "solarpunk");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getTag()).isEqualTo("solarpunk");

    entity.setTag("cyberpunk");
    assertThat(entity.getTag()).isEqualTo("cyberpunk");

    var emptyEntity = new HashtagEntity();
    assertThat(emptyEntity.getId()).isNull();
  }

  @Test
  @DisplayName("HashtagEntityMapping accessors work correctly")
  void hashtagEntityMapping_accessors() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var entityId = UUID.randomUUID();
    var hashtagId = UUID.randomUUID();
    var mapping = new HashtagEntityMapping(tenantId, id, entityId, hashtagId);

    assertThat(mapping.getTenantId()).isEqualTo(tenantId);
    assertThat(mapping.getId()).isEqualTo(id);
    assertThat(mapping.getEntityId()).isEqualTo(entityId);
    assertThat(mapping.getHashtagId()).isEqualTo(hashtagId);

    var emptyMapping = new HashtagEntityMapping();
    assertThat(emptyMapping.getId()).isNull();
  }
}
