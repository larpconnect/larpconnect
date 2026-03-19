package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Represents endpoints for an actor. */
@jakarta.persistence.Entity
@Table(name = "actor_endpoints")
@IdClass(ActorEndpoint.ActorEndpointId.class)
public final class ActorEndpoint {

  public static class ActorEndpointId implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID actor;
    private String name;

    public ActorEndpointId() {}

    public ActorEndpointId(UUID actor, String name) {
      this.actor = actor;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ActorEndpointId that)) return false;
      return Objects.equals(actor, that.actor) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(actor, name);
    }
  }

  @Id
  @ManyToOne
  @JoinColumn(name = "actor_id", nullable = false)
  private Actor actor;

  @Id
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  ActorEndpoint() {}

  public Actor getActor() {
    return actor;
  }

  public void setActor(Actor actor) {
    this.actor = actor;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
