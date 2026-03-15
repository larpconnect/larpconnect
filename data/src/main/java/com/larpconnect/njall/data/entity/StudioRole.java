package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "studio_roles")
@IdClass(StudioRole.StudioRoleId.class)
public class StudioRole {
  public StudioRole() {}

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

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public UUID getStudio() {
      return studio;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setStudio(UUID studio) {
      this.studio = studio;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public UUID getRole() {
      return role;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setRole(UUID role) {
      this.role = role;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public UUID getIndividual() {
      return individual;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setIndividual(UUID individual) {
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
  @JoinColumn(name = "studio_id")
  private Studio studio;

  @Id
  @ManyToOne
  @JoinColumn(name = "role_id")
  private Role role;

  @Id
  @ManyToOne
  @JoinColumn(name = "individual_id")
  private Individual individual;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Studio getStudio() {
    return studio;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setStudio(Studio studio) {
    this.studio = studio;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Role getRole() {
    return role;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setRole(Role role) {
    this.role = role;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Individual getIndividual() {
    return individual;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setIndividual(Individual individual) {
    this.individual = individual;
  }
}
