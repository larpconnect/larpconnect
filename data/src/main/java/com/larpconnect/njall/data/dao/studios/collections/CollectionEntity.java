package com.larpconnect.njall.data.dao.studios.collections;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA entity mapping the {@code njall_users.collections} Common Table Inheritance subtype table.
 */
@Entity
@Table(name = "collections", schema = "njall_users")
@IdClass(EntityId.class)
class CollectionEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "name", nullable = false)
  private String name;

  CollectionEntity() {}

  CollectionEntity(UUID tenantId, UUID id, UUID ownerId, String name) {
    this.tenantId = tenantId;
    this.id = id;
    this.ownerId = ownerId;
    this.name = name;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  UUID getOwnerId() {
    return ownerId;
  }

  void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }
}
