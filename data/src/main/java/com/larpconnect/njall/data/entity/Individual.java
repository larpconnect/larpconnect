package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/**
 * Represents an individual person in the system, acting as a concrete representation of a user or a
 * real-world participant. This forms the base for specific roles like {@link User}. It is a
 * separate entity to decouple physical identity from systemic roles.
 */
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
