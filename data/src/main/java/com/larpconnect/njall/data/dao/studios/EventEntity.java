package com.larpconnect.njall.data.dao.studios;

import com.larpconnect.njall.data.dao.common.EntityId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** JPA entity mapping the {@code njall_users.events} Common Table Inheritance subtype table. */
@Entity
@Table(name = "events", schema = "njall_users")
@IdClass(EntityId.class)
class EventEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "location_id")
  private @Nullable UUID locationId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "start_time")
  private @Nullable Instant startTime;

  @Column(name = "end_time")
  private @Nullable Instant endTime;

  EventEntity() {}

  EventEntity(
      UUID tenantId,
      UUID id,
      @Nullable UUID locationId,
      String title,
      @Nullable Instant startTime,
      @Nullable Instant endTime) {
    this.tenantId = tenantId;
    this.id = id;
    this.locationId = locationId;
    this.title = title;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  @Nullable UUID getLocationId() {
    return locationId;
  }

  void setLocationId(@Nullable UUID locationId) {
    this.locationId = locationId;
  }

  String getTitle() {
    return title;
  }

  void setTitle(String title) {
    this.title = title;
  }

  @Nullable Instant getStartTime() {
    return startTime;
  }

  void setStartTime(@Nullable Instant startTime) {
    this.startTime = startTime;
  }

  @Nullable Instant getEndTime() {
    return endTime;
  }

  void setEndTime(@Nullable Instant endTime) {
    this.endTime = endTime;
  }
}
