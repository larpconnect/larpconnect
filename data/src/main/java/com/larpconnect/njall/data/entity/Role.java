package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents a role in the system. */
@jakarta.persistence.Entity
@Table(name = "roles")
public final class Role extends Entity {

  @Column(name = "role_name", nullable = false)
  private String roleName;

  Role() {}

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
