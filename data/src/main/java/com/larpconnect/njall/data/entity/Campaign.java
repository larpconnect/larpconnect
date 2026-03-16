package com.larpconnect.njall.data.entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "campaigns")
public final class Campaign extends Entity {
  Campaign() {}

  @ManyToOne
  @JoinColumn(name = "system_id")
  private LarpSystem system;

  public LarpSystem getSystem() {
    return system;
  }

  public void setSystem(LarpSystem system) {
    this.system = system;
  }
}
