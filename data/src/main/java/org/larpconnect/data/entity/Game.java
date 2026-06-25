package org.larpconnect.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Game entity scoped to a tenant schema. */
@Entity
@Table(name = "games")
public final class Game extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  public Game() {}

  public Game(UUID id, Campaign campaign) {
    super(id);
    this.campaign = campaign;
  }

  public Campaign getCampaign() {
    return campaign;
  }

  public void setCampaign(Campaign campaign) {
    this.campaign = campaign;
  }
}
