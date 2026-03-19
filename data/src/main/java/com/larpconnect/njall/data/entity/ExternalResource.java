package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Represents external sources of data. */
@jakarta.persistence.Entity
@Table(name = "external_resources")
public final class ExternalResource implements DatabaseObject {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "external_uri", nullable = false, unique = true)
  private String externalUri;

  @Column(name = "data", columnDefinition = "jsonb")
  private String data;

  @Column(name = "last_refresh", nullable = false)
  private OffsetDateTime lastRefresh;

  @Column(name = "hostname", insertable = false, updatable = false)
  private String hostname;

  ExternalResource() {}

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getExternalUri() {
    return externalUri;
  }

  public void setExternalUri(String externalUri) {
    this.externalUri = externalUri;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public OffsetDateTime getLastRefresh() {
    return lastRefresh;
  }

  public void setLastRefresh(OffsetDateTime lastRefresh) {
    this.lastRefresh = lastRefresh;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }
}
