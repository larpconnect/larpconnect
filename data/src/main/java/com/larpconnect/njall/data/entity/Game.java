package com.larpconnect.njall.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game extends com.larpconnect.njall.data.entity.Entity {
  public Game() {}

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
