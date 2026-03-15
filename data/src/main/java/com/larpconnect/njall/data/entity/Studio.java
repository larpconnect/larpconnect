package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "studios")
@DiscriminatorValue("studios")
public class Studio extends Entity {
  public Studio() {}

  @Column(name = "name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
