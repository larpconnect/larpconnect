package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** A LARP studio. */
@jakarta.persistence.Entity
@Table(name = "studios")
public final class Studio extends Entity {

  @Column(name = "name", nullable = false)
  private String name;

  Studio() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
