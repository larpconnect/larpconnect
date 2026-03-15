package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Campaign getCampaign() {
    return campaign;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setCampaign(Campaign campaign) {
    this.campaign = campaign;
  }
}
