package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@jakarta.persistence.Entity
@Table(name = "characters")
@DiscriminatorValue("characters")
public class LarpCharacter extends Entity {
  public LarpCharacter() {}

  @ManyToOne
  @JoinColumn(name = "campaign_id")
  private Campaign campaign;

  @Column(name = "name_template")
  private String nameTemplate;

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
