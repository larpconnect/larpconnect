package com.larpconnect.njall.data.entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Represents a campaign. */
@jakarta.persistence.Entity
@Table(name = "campaigns")
public final class Campaign extends Entity {

  @ManyToOne
  @JoinColumn(name = "system_id")
  private System system;

  Campaign() {}

  public System getSystem() {
    return system;
  }

  public void setSystem(System system) {
    this.system = system;
  }
}
