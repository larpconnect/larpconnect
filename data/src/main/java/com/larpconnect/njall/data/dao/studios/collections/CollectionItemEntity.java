package com.larpconnect.njall.data.dao.studios.collections;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.collection_items} table. */
@Entity
@Table(name = "collection_items", schema = "njall_users")
class CollectionItemEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "collection_id", nullable = false, updatable = false)
  private UUID collectionId;

  @Column(name = "ordinal", nullable = false)
  private int ordinal;

  @Column(name = "added_on", nullable = false, updatable = false)
  private Instant addedOn;

  @Column(name = "refers_to_tenant_id", nullable = false, updatable = false)
  private UUID refersToTenantId;

  @Column(name = "refers_to_id", nullable = false, updatable = false)
  private UUID refersToId;

  CollectionItemEntity() {}

  CollectionItemEntity(
      UUID id,
      UUID tenantId,
      UUID collectionId,
      int ordinal,
      Instant addedOn,
      UUID refersToTenantId,
      UUID refersToId) {
    this.id = id;
    this.tenantId = tenantId;
    this.collectionId = collectionId;
    this.ordinal = ordinal;
    this.addedOn = addedOn;
    this.refersToTenantId = refersToTenantId;
    this.refersToId = refersToId;
  }

  UUID getId() {
    return id;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getCollectionId() {
    return collectionId;
  }

  int getOrdinal() {
    return ordinal;
  }

  void setOrdinal(int ordinal) {
    this.ordinal = ordinal;
  }

  Instant getAddedOn() {
    return addedOn;
  }

  UUID getRefersToTenantId() {
    return refersToTenantId;
  }

  UUID getRefersToId() {
    return refersToId;
  }
}
