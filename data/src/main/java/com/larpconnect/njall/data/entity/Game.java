package com.larpconnect.njall.data.entity;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "games")
public final class Game extends Entity {
  Game() {}

  @ManyToOne
  @JoinColumn(name = "campaign_id")
  private Campaign campaign;

  public Campaign getCampaign() {
    return campaign;
  }

  public void setCampaign(Campaign campaign) {
    this.campaign = campaign;
  }
}
