package com.larpconnect.njall.data.dao.studios.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link InboxActivityEntity}. */
@Embeddable
class InboxActivityId implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final UUID EMPTY_UUID = new UUID(0L, 0L);

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "target_actor_id", nullable = false, updatable = false)
  private UUID targetActorId;

  @Column(name = "activity_id", nullable = false, updatable = false)
  private String activityId;

  InboxActivityId() {
    this(EMPTY_UUID, EMPTY_UUID, "");
  }

  InboxActivityId(UUID tenantId, UUID targetActorId, String activityId) {
    this.tenantId = tenantId;
    this.targetActorId = targetActorId;
    this.activityId = activityId;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getTargetActorId() {
    return targetActorId;
  }

  String getActivityId() {
    return activityId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof InboxActivityId that)) {
      return false;
    }
    return Objects.equals(tenantId, that.tenantId)
        && Objects.equals(targetActorId, that.targetActorId)
        && Objects.equals(activityId, that.activityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, targetActorId, activityId);
  }
}
