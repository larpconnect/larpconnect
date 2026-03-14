package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Actor endpoint entity. */
@Entity
@Table(name = "actor_endpoints")
public final class ActorEndpoint {

  @EmbeddedId private ActorEndpointId id;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  public ActorEndpoint() {}

  public ActorEndpointId getId() {
    return new ActorEndpointId(id.getActorId(), id.getName());
  }

  public void setId(ActorEndpointId id) {
    this.id = new ActorEndpointId(id.getActorId(), id.getName());
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
