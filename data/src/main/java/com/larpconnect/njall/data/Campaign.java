package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Campaign entity. */
@Entity
@Table(name = "campaigns")
public final class Campaign extends NjallEntity {

  @Column(name = "system_id")
  private UUID systemId;

  public Campaign() {}

  public UUID getSystemId() {
    return systemId;
  }

  public void setSystemId(UUID systemId) {
    this.systemId = systemId;
  }
}
