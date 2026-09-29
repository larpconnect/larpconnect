package com.larpconnect.njall.data.dao;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class EntityId implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID tenantId;
  private UUID id;

  EntityId() {}

  EntityId(UUID tenantId, UUID id) {
    this.tenantId = tenantId;
    this.id = id;
  }

  UUID getTenantId() {
    return tenantId;
  }

  void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityId that)) {
      return false;
    }
    return Objects.equals(tenantId, that.tenantId) && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, id);
  }
}
