package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "characters")
public class LarpCharacter extends com.larpconnect.njall.data.entity.Entity {
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
