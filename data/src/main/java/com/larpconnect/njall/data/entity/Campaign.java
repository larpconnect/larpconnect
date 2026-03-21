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
  private LarpSystem system;

  Campaign() {}

  public LarpSystem getLarpSystem() {
    return system;
  }

  public void setLarpSystem(LarpSystem system) {
    this.system = system;
  }
}
