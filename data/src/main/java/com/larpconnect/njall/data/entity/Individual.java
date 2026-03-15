package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "individuals")
@DiscriminatorValue("individuals")
public class Individual extends Entity {
  public Individual() {}

  @Column(name = "name")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
