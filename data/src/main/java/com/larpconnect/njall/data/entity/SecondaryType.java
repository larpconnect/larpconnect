package com.larpconnect.njall.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Secondary types for entities. */
@jakarta.persistence.Entity
@Table(name = "secondary_types")
public final class SecondaryType implements DatabaseObject {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "types_list", nullable = false)
  @JdbcTypeCode(SqlTypes.ARRAY)
  private String[] typesList;

  SecondaryType() {}

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String[] getTypesList() {
    return typesList;
  }

  public void setTypesList(String[] typesList) {
    this.typesList = typesList;
  }
}
