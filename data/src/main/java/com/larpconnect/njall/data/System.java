package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** System entity. */
@Entity
@Table(name = "systems")
public final class System extends NjallEntity {

  @Column(name = "name", nullable = false)
  private String name;

  public System() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
