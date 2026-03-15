package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "individuals")
public class Individual extends com.larpconnect.njall.data.entity.Entity {
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
