package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** User entity. */
@Entity
@Table(name = "users")
public final class User extends Individual {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "username", nullable = false)
  private String username;

  public User() {}

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
