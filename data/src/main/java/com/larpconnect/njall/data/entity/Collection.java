package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "collections")
@DiscriminatorValue("collections")
public class Collection extends Entity {
  public Collection() {}

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Entity owner;

  @Column(name = "name")
  private String name;

  public Entity getOwner() {
    return owner;
  }

  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
