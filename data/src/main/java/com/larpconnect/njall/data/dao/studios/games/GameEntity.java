package com.larpconnect.njall.data.dao.studios.games;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** JPA entity mapping the {@code njall_users.games} 2nd-level Common Table Inheritance table. */
@Entity
@Table(name = "games", schema = "njall_users")
@IdClass(EntityId.class)
class GameEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "system_id", nullable = false)
  private UUID systemId;

  @Column(name = "campaign_id")
  private @Nullable UUID campaignId;

  GameEntity() {}

  GameEntity(UUID tenantId, UUID id, UUID systemId, @Nullable UUID campaignId) {
    this.tenantId = tenantId;
    this.id = id;
    this.systemId = systemId;
    this.campaignId = campaignId;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  UUID getSystemId() {
    return systemId;
  }

  void setSystemId(UUID systemId) {
    this.systemId = systemId;
  }

  @Nullable UUID getCampaignId() {
    return campaignId;
  }

  void setCampaignId(@Nullable UUID campaignId) {
    this.campaignId = campaignId;
  }
}
