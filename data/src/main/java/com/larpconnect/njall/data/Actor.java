package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Actor entity. */
@Entity
@Table(name = "actors")
public final class Actor extends NjallEntity {

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "summary")
  private String summary;

  @Column(name = "preferred_username", nullable = false)
  private String preferredUsername;

  public Actor() {}

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
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
