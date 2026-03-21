package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

/** Individuals who have individual logins on the system. */
@jakarta.persistence.Entity
@Table(name = "users")
public final class User extends Individual {

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  User() {}

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
