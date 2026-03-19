package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Represents a piece of contact information for an entity. */
@jakarta.persistence.Entity
@Table(name = "contact_info")
public final class ContactInfo implements DatabaseObject {

  public enum ContactType {
    email,
    phone,
    address
  }

  @Id
  @Column(name = "id")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private Entity owner;

  @Column(name = "ordering", nullable = false)
  private Integer ordering;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_type", nullable = false)
  private ContactType contactType;

  @Column(name = "contact", nullable = false)
  private String contact;

  @Column(name = "active", nullable = false)
  private Boolean active;

  ContactInfo() {}

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Entity getOwner() {
    return owner;
  }

  public void setOwner(Entity owner) {
    this.owner = owner;
  }

  public Integer getOrdering() {
    return ordering;
  }

  public void setOrdering(Integer ordering) {
    this.ordering = ordering;
  }

  public ContactType getContactType() {
    return contactType;
  }

  public void setContactType(ContactType contactType) {
    this.contactType = contactType;
  }

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
