package com.larpconnect.njall.data.dao.studios.events;

import com.larpconnect.njall.data.dao.common.EntityId;
import com.larpconnect.njall.data.domain.studios.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.jspecify.annotations.Nullable;

/** JPA entity mapping the {@code njall_users.events} Common Table Inheritance subtype table. */
@Entity
@Table(name = "events", schema = "njall_users")
@IdClass(EntityId.class)
public class EventEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "event_type", nullable = false, columnDefinition = "njall_users.tevent_type")
  private EventType eventType = EventType.EVENT_TYPE_UNSPECIFIED;

  @Column(name = "location_id")
  private @Nullable UUID locationId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "start_time")
  private @Nullable Instant startTime;

  @Column(name = "end_time")
  private @Nullable Instant endTime;

  /** Required no-arg constructor for Hibernate. */
  public EventEntity() {}

  /**
   * Constructs a populated {@link EventEntity}.
   *
   * @param tenantId The internal tenant UUID.
   * @param id The event UUID identifier.
   * @param eventType The event classification type.
   * @param locationId Optional physical location UUID.
   * @param title The event title.
   * @param startTime Optional start timestamp.
   * @param endTime Optional end timestamp.
   */
  public EventEntity(
      UUID tenantId,
      UUID id,
      EventType eventType,
      @Nullable UUID locationId,
      String title,
      @Nullable Instant startTime,
      @Nullable Instant endTime) {
    this.tenantId = tenantId;
    this.id = id;
    this.eventType = eventType;
    this.locationId = locationId;
    this.title = title;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getId() {
    return id;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public @Nullable UUID getLocationId() {
    return locationId;
  }

  public void setLocationId(@Nullable UUID locationId) {
    this.locationId = locationId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public @Nullable Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(@Nullable Instant startTime) {
    this.startTime = startTime;
  }

  public @Nullable Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(@Nullable Instant endTime) {
    this.endTime = endTime;
  }
}
