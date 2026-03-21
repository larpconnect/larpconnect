package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents server metadata. */
@jakarta.persistence.Entity
@Table(name = "server_metadata")
public final class ServerMetadata extends Entity {

  @Column(name = "name", nullable = false)
  private String name;

  ServerMetadata() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
