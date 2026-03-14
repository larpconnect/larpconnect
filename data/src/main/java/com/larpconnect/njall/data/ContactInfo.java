package com.larpconnect.njall.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Contact info entity. */
@Entity
@Table(name = "contact_info")
public final class ContactInfo {

  @Id
  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "ordering", nullable = false)
  private Integer ordering;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_type", nullable = false)
  private ContactType contactType;

  @Column(name = "contact", nullable = false)
  private String contact;

  @Column(name = "active", nullable = false)
  private Boolean active;

  public ContactInfo() {}

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
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
