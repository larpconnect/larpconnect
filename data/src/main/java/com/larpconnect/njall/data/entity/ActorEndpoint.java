package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "actor_endpoints")
@IdClass(ActorEndpoint.ActorEndpointId.class)
public class ActorEndpoint {
  public ActorEndpoint() {}

  public static class ActorEndpointId implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID actor;
    private String name;

    public ActorEndpointId() {}

    public ActorEndpointId(UUID actor, String name) {
      this.actor = actor;
      this.name = name;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public UUID getActor() {
      return actor;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setActor(UUID actor) {
      this.actor = actor;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public String getName() {
      return name;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setName(String name) {
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
  @JoinColumn(name = "actor_id")
  private Actor actor;

  @Id
  @Column(name = "name")
  private String name;

  @Column(name = "endpoint")
  private String endpoint;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Actor getActor() {
    return actor;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setActor(Actor actor) {
    this.actor = actor;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getName() {
    return name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setName(String name) {
    this.name = name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getEndpoint() {
    return endpoint;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
