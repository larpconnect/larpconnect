package com.larpconnect.njall.data.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** JPA entity mapping the {@code njall_users.links} Common Table Inheritance subtype table. */
@Entity
@Table(name = "links", schema = "njall_users")
@IdClass(EntityId.class)
class LinkEntity {

  @Id
  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "link_type", nullable = false)
  private String linkType;

  @Column(name = "url", nullable = false)
  private String url;

  @Column(name = "media_type", nullable = false)
  private String mediaType;

  LinkEntity() {}

  LinkEntity(UUID tenantId, UUID id, String linkType, String url, String mediaType) {
    this.tenantId = tenantId;
    this.id = id;
    this.linkType = linkType;
    this.url = url;
    this.mediaType = mediaType;
  }

  UUID getTenantId() {
    return tenantId;
  }

  UUID getId() {
    return id;
  }

  String getLinkType() {
    return linkType;
  }

  void setLinkType(String linkType) {
    this.linkType = linkType;
  }

  String getUrl() {
    return url;
  }

  void setUrl(String url) {
    this.url = url;
  }

  String getMediaType() {
    return mediaType;
  }

  void setMediaType(String mediaType) {
    this.mediaType = mediaType;
  }
}
