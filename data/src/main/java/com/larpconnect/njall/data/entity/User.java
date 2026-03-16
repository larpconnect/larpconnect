package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "users")
public final class User extends Individual {
  User() {}

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Individual owner;

  @Column(name = "username")
  private String username;

  public Individual getOwner() {
    return owner;
  }

  public void setOwner(Individual owner) {
    this.owner = owner;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
