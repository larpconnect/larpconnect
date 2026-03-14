package com.larpconnect.njall.data;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Actor endpoint id. */
@Embeddable
public final class ActorEndpointId implements Serializable {
  private static final long serialVersionUID = 1L;

  private UUID actorId;
  private String name;

  public ActorEndpointId() {}

  public ActorEndpointId(UUID actorId, String name) {
    this.actorId = actorId;
    this.name = name;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ActorEndpointId that = (ActorEndpointId) o;
    return Objects.equals(actorId, that.actorId) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actorId, name);
  }
}
