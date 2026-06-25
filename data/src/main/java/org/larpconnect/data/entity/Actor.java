package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Actor entity scoped to a tenant schema. */
@Entity
@Table(name = "actors")
public final class Actor extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private TenantEntity owner;

  @Column(name = "summary")
  private String summary;

  @Column(name = "preferred_username", nullable = false)
  private String preferredUsername;

  public Actor() {}

  public Actor(UUID id, TenantEntity owner, String summary, String preferredUsername) {
    super(id);
    this.owner = owner;
    this.summary = summary;
    this.preferredUsername = preferredUsername;
  }

  public TenantEntity getOwner() {
    return owner;
  }

  public void setOwner(TenantEntity owner) {
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
