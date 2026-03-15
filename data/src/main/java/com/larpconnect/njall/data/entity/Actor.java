package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "actors")
public class Actor extends com.larpconnect.njall.data.entity.Entity {
  public Actor() {}

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Entity owner;

  @Column(name = "summary")
  private String summary;

  @Column(name = "preferred_username")
  private String preferredUsername;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Entity getOwner() {
    return owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getSummary() {
    return summary;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setSummary(String summary) {
    this.summary = summary;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getPreferredUsername() {
    return preferredUsername;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setPreferredUsername(String preferredUsername) {
    this.preferredUsername = preferredUsername;
  }
}
