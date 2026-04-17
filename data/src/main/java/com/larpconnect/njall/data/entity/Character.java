package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a character in a campaign.
 *
 * <p>Characters are distinct from {@link Individual} entities as they represent a fictional persona
 * within a specific game or campaign context, rather than the physical player themselves.
 */
@jakarta.persistence.Entity
@Table(name = "characters")
@SuppressWarnings("JavaLangClash") // Intentional naming mirroring the schema
public final class Character extends Entity {

  @ManyToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Column(name = "name_template", nullable = false)
  private String nameTemplate;

  Character() {}

  public Campaign getCampaign() {
    return campaign;
  }

  public void setCampaign(Campaign campaign) {
    this.campaign = campaign;
  }

  public String getNameTemplate() {
    return nameTemplate;
  }

  public void setNameTemplate(String nameTemplate) {
    this.nameTemplate = nameTemplate;
  }
}
