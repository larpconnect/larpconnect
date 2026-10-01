package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Package-private Hibernate entity mapping the {@code njall_users.default_studio_roles} table. */
@Entity
@Table(name = "default_studio_roles", schema = "njall_users")
class DefaultStudioRoleEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  DefaultStudioRoleEntity() {}

  DefaultStudioRoleEntity(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }
}
