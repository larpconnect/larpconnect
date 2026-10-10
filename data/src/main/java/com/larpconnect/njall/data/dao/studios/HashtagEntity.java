package com.larpconnect.njall.data.dao.studios;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.hashtags} CTI subtype table. */
@Entity
@Table(name = "hashtags", schema = "njall_users")
@IdClass(EntityId.class)
class HashtagEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tag", nullable = false)
  private String tag;

  HashtagEntity() {}

  HashtagEntity(UUID tenantId, UUID id, String tag) {
    this.tenantId = tenantId;
    this.id = id;
    this.tag = tag;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  String getTag() {
    return tag;
  }

  void setTag(String tag) {
    this.tag = tag;
  }
}
