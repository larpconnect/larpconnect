package com.larpconnect.njall.data.dao.studios;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.locations} Common Table Inheritance subtype table. */
@Entity
@Table(name = "locations", schema = "njall_users")
@IdClass(EntityId.class)
class LocationEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  LocationEntity() {}

  LocationEntity(UUID tenantId, UUID id, String name) {
    this.tenantId = tenantId;
    this.id = id;
    this.name = name;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }
}
