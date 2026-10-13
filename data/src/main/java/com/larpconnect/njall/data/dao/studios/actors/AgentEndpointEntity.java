package com.larpconnect.njall.data.dao.studios.actors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.actor_endpoints} federated endpoints table. */
@Entity
@Table(name = "actor_endpoints", schema = "njall_users")
@IdClass(AgentEndpointId.class)
class AgentEndpointEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "actor_id", nullable = false, updatable = false)
  private UUID actorId;

  @Id
  @Column(name = "name", length = 31, nullable = false, updatable = false)
  private String name;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  AgentEndpointEntity() {}

  AgentEndpointEntity(UUID tenantId, UUID actorId, String name, String endpoint) {
    this.tenantId = tenantId;
    this.actorId = actorId;
    this.name = name;
    this.endpoint = endpoint;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getActorId() {
    return actorId;
  }

  String getName() {
    return name;
  }

  String getEndpoint() {
    return endpoint;
  }

  void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
