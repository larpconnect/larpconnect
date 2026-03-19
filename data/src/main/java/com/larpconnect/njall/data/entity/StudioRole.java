package com.larpconnect.njall.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** A role that an individual has in a studio. */
@Entity
@Table(name = "studio_roles")
@IdClass(StudioRole.StudioRoleId.class)
public final class StudioRole {

  public static class StudioRoleId implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID studio;
    private UUID role;
    private UUID individual;

    public StudioRoleId() {}

    public StudioRoleId(UUID studio, UUID role, UUID individual) {
      this.studio = studio;
      this.role = role;
      this.individual = individual;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof StudioRoleId that)) return false;
      return Objects.equals(studio, that.studio)
          && Objects.equals(role, that.role)
          && Objects.equals(individual, that.individual);
    }

    @Override
    public int hashCode() {
      return Objects.hash(studio, role, individual);
    }
  }

  @Id
  @ManyToOne
  @JoinColumn(name = "studio_id", nullable = false)
  private Studio studio;

  @Id
  @ManyToOne
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Id
  @ManyToOne
  @JoinColumn(name = "individual_id", nullable = false)
  private Individual individual;

  StudioRole() {}

  public Studio getStudio() {
    return studio;
  }

  public void setStudio(Studio studio) {
    this.studio = studio;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Individual getIndividual() {
    return individual;
  }

  public void setIndividual(Individual individual) {
    this.individual = individual;
  }
}
