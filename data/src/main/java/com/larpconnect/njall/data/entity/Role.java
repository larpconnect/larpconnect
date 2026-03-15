package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role extends com.larpconnect.njall.data.entity.Entity {
  public Role() {}

  @Column(name = "role_name")
  private String roleName;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getRoleName() {
    return roleName;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }
}
