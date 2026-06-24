package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** User entity scoped to a tenant schema, extending Individual. */
@Entity
@Table(name = "users")
public final class User extends Individual {
  @Column(name = "username", nullable = false)
  private String username;

  public User() {}

  public User(UUID id, String name, String username) {
    super(id, name);
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
