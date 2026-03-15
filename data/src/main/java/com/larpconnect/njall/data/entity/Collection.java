package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Entity getOwner() {
    return owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getName() {
    return name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setName(String name) {
    this.name = name;
  }
}
