package com.larpconnect.njall.data.dao.studios.actors;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link AgentEndpointEntity}. */
public class AgentEndpointId implements Serializable {

  private static final long serialVersionUID = 1L;

  private UUID tenantId;
  private UUID actorId;
  private String name;

  public AgentEndpointId() {}

  public AgentEndpointId(UUID tenantId, UUID actorId, String name) {
    this.tenantId = tenantId;
    this.actorId = actorId;
    this.name = name;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getActorId() {
    return actorId;
  }

  public void setActorId(UUID actorId) {
    this.actorId = actorId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AgentEndpointId that)) {
      return false;
    }
    return Objects.equals(tenantId, that.tenantId)
        && Objects.equals(actorId, that.actorId)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, actorId, name);
  }
}
