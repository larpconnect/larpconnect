package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends Individual {
  public User() {}

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Individual owner;

  @Column(name = "username")
  private String username;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Individual getOwner() {
    return owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOwner(Individual owner) {
    this.owner = owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getUsername() {
    return username;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setUsername(String username) {
    this.username = username;
  }
}
