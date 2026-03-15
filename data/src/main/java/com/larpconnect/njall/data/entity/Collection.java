package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "collections")
public class Collection extends com.larpconnect.njall.data.entity.Entity {
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
