package com.larpconnect.njall.data.dao.studios.actors;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.actors} Common Table Inheritance subtype table. */
@Entity
@Table(name = "actors", schema = "njall_users")
@IdClass(EntityId.class)
class AgentEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private UUID ownerId;

  @Column(name = "preferred_username", nullable = false)
  private String preferredUsername;

  AgentEntity() {}

  AgentEntity(UUID tenantId, UUID id, UUID ownerId, String preferredUsername) {
    this.tenantId = tenantId;
    this.id = id;
    this.ownerId = ownerId;
    this.preferredUsername = preferredUsername;
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

  String getPreferredUsername() {
    return preferredUsername;
  }

  void setPreferredUsername(String preferredUsername) {
    this.preferredUsername = preferredUsername;
  }
}
