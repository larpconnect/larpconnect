package com.larpconnect.njall.data.dao.common;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link EntityBaseEntity}. */
public class EntityId implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID tenantId;
  private UUID id;

  public EntityId() {}

  public EntityId(UUID tenantId, UUID id) {
    this.tenantId = tenantId;
    this.id = id;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
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
