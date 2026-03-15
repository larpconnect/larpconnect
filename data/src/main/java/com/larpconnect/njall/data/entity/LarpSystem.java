package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "systems")
@DiscriminatorValue("systems")
public class LarpSystem extends Entity {
  public LarpSystem() {}

  @Column(name = "name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
