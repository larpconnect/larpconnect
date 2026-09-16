package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Package-private Hibernate entity mapping the {@code njall.servers} table. */
@Entity
@Immutable
@Table(name = "servers", schema = "njall")
class ServerEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, length = 64, unique = true)
  private String name;

  @Column(name = "primary_domain", nullable = false, length = 255)
  private String primaryDomain;

  @Column(name = "created_on", nullable = false, updatable = false)
  private Instant createdOn;

  ServerEntity() {}

  ServerEntity(UUID id, String name, String primaryDomain, Instant createdOn) {
    this.id = id;
    this.name = name;
    this.primaryDomain = primaryDomain;
    this.createdOn = createdOn;
  }

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }

  String getPrimaryDomain() {
    return primaryDomain;
  }

  Instant getCreatedOn() {
    return createdOn;
  }
}
