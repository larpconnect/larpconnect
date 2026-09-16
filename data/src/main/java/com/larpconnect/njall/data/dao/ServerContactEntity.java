package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.domain.ContactType;
import com.larpconnect.njall.data.domain.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/** Package-private Hibernate entity mapping the {@code njall.server_contacts} table. */
@Entity
@Immutable
@Table(name = "server_contacts", schema = "njall")
class ServerContactEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "role_type", nullable = false, columnDefinition = "njall.trole")
  private RoleType roleType;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "contact_type", nullable = false, columnDefinition = "njall.tcontact")
  private ContactType contactType;

  @Column(name = "contact", nullable = false, length = 255)
  private String contact;

  @Column(name = "ordering", nullable = false)
  private int ordering;

  ServerContactEntity() {}

  ServerContactEntity(
      UUID id, RoleType roleType, ContactType contactType, String contact, int ordering) {
    this.id = id;
    this.roleType = roleType;
    this.contactType = contactType;
    this.contact = contact;
    this.ordering = ordering;
  }

  UUID getId() {
    return id;
  }

  RoleType getRoleType() {
    return roleType;
  }

  ContactType getContactType() {
    return contactType;
  }

  String getContact() {
    return contact;
  }

  int getOrdering() {
    return ordering;
  }
}
