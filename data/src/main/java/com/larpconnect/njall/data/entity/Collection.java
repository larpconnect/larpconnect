package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Collection objects associated with entities. */
@jakarta.persistence.Entity
@Table(name = "collections")
public final class Collection extends Entity {

  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private Entity owner;

  @Column(name = "name", nullable = false)
  private String name;

  Collection() {}

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
