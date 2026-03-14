package com.larpconnect.njall.data;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
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
}
