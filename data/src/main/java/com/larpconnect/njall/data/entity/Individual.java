package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Represents an individual person. */
@jakarta.persistence.Entity
@Table(name = "individuals")
public class Individual extends Entity {

  @Column(name = "name", nullable = false)
  private String name;

  Individual() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
