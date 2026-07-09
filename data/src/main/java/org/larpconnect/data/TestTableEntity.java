package org.larpconnect.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Entity representing the TESTTABLE for integration verification. */
@Entity
@Table(name = "TESTTABLE")
public final class TestTableEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  /** Required by Hibernate. */
  TestTableEntity() {}

  public TestTableEntity(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TestTableEntity that)) {
      return false;
    }
    return Objects.equals(id, that.id) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
