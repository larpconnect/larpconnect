package com.larpconnect.njall.data.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "campaigns")
@DiscriminatorValue("campaigns")
public class Campaign extends Entity {
  public Campaign() {}

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
