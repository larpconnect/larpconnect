package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Studio entity. */
@Entity
@Table(name = "studios")
public final class Studio extends NjallEntity {

  @Column(name = "name", nullable = false)
  private String name;

  public Studio() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
