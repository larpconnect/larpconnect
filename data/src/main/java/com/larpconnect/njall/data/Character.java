package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** Character entity. */
@Entity
@Table(name = "characters")
public final class Character extends NjallEntity {

  @Column(name = "campaign_id", nullable = false)
  private UUID campaignId;

  @Column(name = "name_template", nullable = false)
  private String nameTemplate;

  public Character() {}

  public UUID getCampaignId() {
    return campaignId;
  }

  public void setCampaignId(UUID campaignId) {
    this.campaignId = campaignId;
  }

  public String getNameTemplate() {
    return nameTemplate;
  }

  public void setNameTemplate(String nameTemplate) {
    this.nameTemplate = nameTemplate;
  }
}
