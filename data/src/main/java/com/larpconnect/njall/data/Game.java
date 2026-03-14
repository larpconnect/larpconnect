package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Game entity. */
@Entity
@Table(name = "games")
public final class Game extends NjallEntity {

  @Column(name = "campaign_id", nullable = false)
  private UUID campaignId;

  public Game() {}

  public UUID getCampaignId() {
    return campaignId;
  }

  public void setCampaignId(UUID campaignId) {
    this.campaignId = campaignId;
  }
}
