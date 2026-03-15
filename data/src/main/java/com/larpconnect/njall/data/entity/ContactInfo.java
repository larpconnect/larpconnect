package com.larpconnect.njall.data.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "contact_info")
public class ContactInfo {
  public ContactInfo() {}

  @Id @Generated private UUID id;

  @ManyToOne
  @JoinColumn(name = "owner_id")
  private Entity owner;

  @Column(name = "ordering")
  private int ordering;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_type")
  private ContactType contactType;

  @Column(name = "contact")
  private String contact;

  @Column(name = "active")
  private boolean active;

  public enum ContactType {
    email,
    phone,
    address
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public UUID getId() {
    return id;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setId(UUID id) {
    this.id = id;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public Entity getOwner() {
    return owner;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  public int getOrdering() {
    return ordering;
  }

  public void setOrdering(int ordering) {
    this.ordering = ordering;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ContactType getContactType() {
    return contactType;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setContactType(ContactType contactType) {
    this.contactType = contactType;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String getContact() {
    return contact;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setContact(String contact) {
    this.contact = contact;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
