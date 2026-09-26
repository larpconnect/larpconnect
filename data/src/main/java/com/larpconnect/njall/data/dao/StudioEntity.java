package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Package-private Hibernate entity mapping the {@code njall_users.studios} table. */
@Entity
@Table(name = "studios", schema = "njall_users")
class StudioEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  StudioEntity() {}

  StudioEntity(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }
}
