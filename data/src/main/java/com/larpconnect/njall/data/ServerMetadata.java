package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Server metadata entity. */
@Entity
@Table(name = "server_metadata")
public final class ServerMetadata extends NjallEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "admin", nullable = false)
  private String admin;

  @Column(name = "security", nullable = false)
  private String security;

  @Column(name = "support", nullable = false)
  private String support;

  public ServerMetadata() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAdmin() {
    return admin;
  }

  public void setAdmin(String admin) {
    this.admin = admin;
  }

  public String getSecurity() {
    return security;
  }

  public void setSecurity(String security) {
    this.security = security;
  }

  public String getSupport() {
    return support;
  }

  public void setSupport(String support) {
    this.support = support;
  }
}
