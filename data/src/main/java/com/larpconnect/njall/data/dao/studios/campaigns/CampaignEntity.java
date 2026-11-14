package com.larpconnect.njall.data.dao.studios.campaigns;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** JPA entity mapping the {@code njall_users.campaigns} Common Table Inheritance subtype table. */
@Entity
@Table(name = "campaigns", schema = "njall_users")
@IdClass(EntityId.class)
class CampaignEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "system_id")
  private @Nullable UUID systemId;

  @Column(name = "title", nullable = false)
  private String title;

  /** Required no-arg constructor for Hibernate. */
  CampaignEntity() {}

  /**
   * Constructs a populated {@link CampaignEntity}.
   *
   * @param tenantId The internal tenant UUID.
   * @param id The campaign UUID identifier.
   * @param systemId Optional associated game system UUID.
   * @param title The required campaign title.
   */
  CampaignEntity(UUID tenantId, UUID id, @Nullable UUID systemId, String title) {
    this.tenantId = tenantId;
    this.id = id;
    this.systemId = systemId;
    this.title = title;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  @Nullable UUID getSystemId() {
    return systemId;
  }

  void setSystemId(@Nullable UUID systemId) {
    this.systemId = systemId;
  }

  String getTitle() {
    return title;
  }

  void setTitle(String title) {
    this.title = title;
  }
}
