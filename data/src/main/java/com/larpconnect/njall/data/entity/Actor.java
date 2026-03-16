package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "actors")
public final class Actor extends Entity {
  public Actor() {}

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Entity owner;

  @Column(name = "summary")
  private String summary;

  @Column(name = "preferred_username")
  private String preferredUsername;

  public Entity getOwner() {
    return owner;
  }

  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getPreferredUsername() {
    return preferredUsername;
  }

  public void setPreferredUsername(String preferredUsername) {
    this.preferredUsername = preferredUsername;
  }
}
