package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "roles")
@DiscriminatorValue("roles")
public class Role extends Entity {
  public Role() {}

  @Column(name = "role_name")
  private String roleName;

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
