package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents a LARP system. */
@jakarta.persistence.Entity
@Table(name = "systems")
@SuppressWarnings("JavaLangClash") // Intentional naming mirroring the schema
public final class System extends Entity {

  @Column(name = "name", nullable = false)
  private String name;

  System() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
