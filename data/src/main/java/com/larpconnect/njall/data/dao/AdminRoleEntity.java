package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Package-private Hibernate entity mapping the {@code njall_admin.admin_roles} table. */
@Entity
@Table(name = "admin_roles", schema = "njall_admin")
class AdminRoleEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "role_name", nullable = false, unique = true)
  private String roleName;

  AdminRoleEntity() {}

  AdminRoleEntity(UUID id, String roleName) {
    this.id = id;
    this.roleName = roleName;
  }

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  String getRoleName() {
    return roleName;
  }

  void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
