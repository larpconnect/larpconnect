package com.larpconnect.njall.data;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Studio role id. */
@Embeddable
public final class StudioRoleId implements Serializable {
  private static final long serialVersionUID = 1L;

  private UUID studioId;
  private UUID roleId;
  private UUID individualId;

  public StudioRoleId() {}

  public StudioRoleId(UUID studioId, UUID roleId, UUID individualId) {
    this.studioId = studioId;
    this.roleId = roleId;
    this.individualId = individualId;
  }

  public UUID getStudioId() {
    return studioId;
  }

  public void setStudioId(UUID studioId) {
    this.studioId = studioId;
  }

  public UUID getRoleId() {
    return roleId;
  }

  public void setRoleId(UUID roleId) {
    this.roleId = roleId;
  }

  public UUID getIndividualId() {
    return individualId;
  }

  public void setIndividualId(UUID individualId) {
    this.individualId = individualId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StudioRoleId that = (StudioRoleId) o;
    return Objects.equals(studioId, that.studioId)
        && Objects.equals(roleId, that.roleId)
        && Objects.equals(individualId, that.individualId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studioId, roleId, individualId);
  }
}
