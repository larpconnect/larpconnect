package org.larpconnect.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Character entity scoped to a tenant schema. */
@Entity
@Table(name = "characters")
public final class LarpCharacter extends TenantEntity {
  @ManyToOne
  @JoinColumn(name = "campaign_id", nullable = false)
  private Campaign campaign;

  @Column(name = "name_template", nullable = false)
  private String nameTemplate;

  public LarpCharacter() {}

  public LarpCharacter(UUID id, Campaign campaign, String nameTemplate) {
    super(id);
    this.campaign = campaign;
    this.nameTemplate = nameTemplate;
  }

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
