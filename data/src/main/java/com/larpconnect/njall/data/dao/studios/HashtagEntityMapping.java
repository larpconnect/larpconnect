package com.larpconnect.njall.data.dao.studios;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.hashtags_entity} intersect join table. */
@Entity
@Table(name = "hashtags_entity", schema = "njall_users")
@IdClass(EntityId.class)
class HashtagEntityMapping {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "entity_id", nullable = false, updatable = false)
  private UUID entityId;

  @Column(name = "hashtag_id", nullable = false, updatable = false)
  private UUID hashtagId;

  HashtagEntityMapping() {}

  HashtagEntityMapping(UUID tenantId, UUID id, UUID entityId, UUID hashtagId) {
    this.tenantId = tenantId;
    this.id = id;
    this.entityId = entityId;
    this.hashtagId = hashtagId;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  UUID getEntityId() {
    return entityId;
  }

  UUID getHashtagId() {
    return hashtagId;
  }
}
