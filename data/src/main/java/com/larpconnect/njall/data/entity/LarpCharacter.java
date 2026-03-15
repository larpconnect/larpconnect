package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Campaign getCampaign() {
    return campaign;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setCampaign(Campaign campaign) {
    this.campaign = campaign;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getNameTemplate() {
    return nameTemplate;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setNameTemplate(String nameTemplate) {
    this.nameTemplate = nameTemplate;
  }
}
