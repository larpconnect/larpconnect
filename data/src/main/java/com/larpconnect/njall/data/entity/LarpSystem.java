package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents a LARP system. */
@jakarta.persistence.Entity
@Table(name = "systems")
public final class LarpSystem extends Entity {

  @Column(name = "name", nullable = false)
  private String name;

  LarpSystem() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
