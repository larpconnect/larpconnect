package com.larpconnect.njall.data.dao.studios.inbox;

import com.larpconnect.njall.data.domain.inbox.InboxActivityRecord;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.inbox_activities} deduplication table. */
@Entity
@Table(name = "inbox_activities", schema = "njall_users")
class InboxActivityEntity {

  @EmbeddedId private InboxActivityId id;

  @Column(name = "received_on", nullable = false, updatable = false)
  private Instant receivedOn;

  @Column(name = "expires_on", nullable = false)
  private Instant expiresOn;

  InboxActivityEntity() {
    this(createEmptyId(), Instant.EPOCH, Instant.EPOCH);
  }

  InboxActivityEntity(InboxActivityId id, Instant receivedOn, Instant expiresOn) {
    this.id = id;
    this.receivedOn = receivedOn;
    this.expiresOn = expiresOn;
  }

  InboxActivityId getId() {
    return id;
  }

  Instant getReceivedOn() {
    return receivedOn;
  }

  Instant getExpiresOn() {
    return expiresOn;
  }

  void setExpiresOn(Instant expiresOn) {
    this.expiresOn = expiresOn;
  }

  InboxActivityRecord toDomain() {
    return createDomainRecord(
        id.getTenantId(), id.getTargetActorId(), id.getActivityId(), receivedOn, expiresOn);
  }

  private static InboxActivityId createEmptyId() {
    return new InboxActivityId();
  }

  private static InboxActivityRecord createDomainRecord(
      UUID tenantId, UUID targetActorId, String activityId, Instant receivedOn, Instant expiresOn) {
    return new InboxActivityRecord(tenantId, targetActorId, activityId, receivedOn, expiresOn);
  }
}
