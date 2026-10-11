package com.larpconnect.njall.data.dao.studios.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class CollectionEntityTest {

  @Test
  @DisplayName("CollectionEntity getters and setters operate correctly")
  void collectionEntity_gettersAndSetters() {
    var tenantId = UUID.randomUUID();
    var id = UUID.randomUUID();
    var ownerId = UUID.randomUUID();

    var entity = new CollectionEntity(tenantId, id, ownerId, "Original Name");

    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getOwnerId()).isEqualTo(ownerId);
    assertThat(entity.getName()).isEqualTo("Original Name");

    var newOwnerId = UUID.randomUUID();
    entity.setOwnerId(newOwnerId);
    entity.setName("New Name");

    assertThat(entity.getOwnerId()).isEqualTo(newOwnerId);
    assertThat(entity.getName()).isEqualTo("New Name");
  }

  @Test
  @DisplayName("CollectionItemEntity getters and setters operate correctly")
  void collectionItemEntity_gettersAndSetters() {
    var id = UUID.randomUUID();
    var tenantId = UUID.randomUUID();
    var collectionId = UUID.randomUUID();
    var refersToTenantId = UUID.randomUUID();
    var refersToId = UUID.randomUUID();
    var now = Instant.now();

    var entity =
        new CollectionItemEntity(id, tenantId, collectionId, 0, now, refersToTenantId, refersToId);

    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getTenantId()).isEqualTo(tenantId);
    assertThat(entity.getCollectionId()).isEqualTo(collectionId);
    assertThat(entity.getOrdinal()).isEqualTo(0);
    assertThat(entity.getAddedOn()).isEqualTo(now);
    assertThat(entity.getRefersToTenantId()).isEqualTo(refersToTenantId);
    assertThat(entity.getRefersToId()).isEqualTo(refersToId);

    entity.setOrdinal(3);
    assertThat(entity.getOrdinal()).isEqualTo(3);
  }
}
