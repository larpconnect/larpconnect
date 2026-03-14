package com.larpconnect.njall.data;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Studio role entity. */
@Entity
@Table(name = "studio_roles")
public final class StudioRole {

  @EmbeddedId private StudioRoleId id;

  public StudioRole() {}

  public StudioRoleId getId() {
    return new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());
  }

  public void setId(StudioRoleId id) {
    this.id = new StudioRoleId(id.getStudioId(), id.getRoleId(), id.getIndividualId());
  }
}
