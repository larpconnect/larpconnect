package com.larpconnect.njall.data.dao.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class EntityIdTest {

  @Test
  @DisplayName(
      "EntityId getters, setters, equals, and hashCode behave correctly across all branches")
  void entityId_contracts() {
    var tenant1 = UUID.randomUUID();
    var tenant2 = UUID.randomUUID();
    var id1 = UUID.randomUUID();
    var id2 = UUID.randomUUID();

    var entityId1 = new EntityId();
    entityId1.setTenantId(tenant1);
    entityId1.setId(id1);

    var entityIdSame = new EntityId(tenant1, id1);
    var entityIdDiffTenant = new EntityId(tenant2, id1);
    var entityIdDiffId = new EntityId(tenant1, id2);

    assertThat(entityId1.getTenantId()).isEqualTo(tenant1);
    assertThat(entityId1.getId()).isEqualTo(id1);

    // Reflexive
    assertThat(entityId1.equals(entityId1)).isTrue();
    // Equal values
    assertThat(entityId1.equals(entityIdSame)).isTrue();
    assertThat(entityId1.hashCode()).isEqualTo(entityIdSame.hashCode());

    // Null and other types
    Object nullObj = null;
    Object otherType = new Object();
    assertThat(entityId1.equals(nullObj)).isFalse();
    assertThat(entityId1.equals(otherType)).isFalse();

    // Different fields
    assertThat(entityId1.equals(entityIdDiffTenant)).isFalse();
    assertThat(entityId1.equals(entityIdDiffId)).isFalse();
  }
}
