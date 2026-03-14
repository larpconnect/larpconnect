package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Collection entity. */
@Entity
@Table(name = "collections")
public final class Collection extends NjallEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "name", nullable = false)
  private String name;

  public Collection() {}

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
