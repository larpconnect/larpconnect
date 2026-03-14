package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Individual entity. */
@Entity
@Table(name = "individuals")
public class Individual extends NjallEntity {

  @Column(name = "name", nullable = false)
  private String name;

  public Individual() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
