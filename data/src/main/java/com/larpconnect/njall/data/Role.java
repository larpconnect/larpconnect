package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Role entity. */
@Entity
@Table(name = "roles")
public final class Role extends NjallEntity {

  @Column(name = "role_name", nullable = false)
  private String roleName;

  public Role() {}

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
