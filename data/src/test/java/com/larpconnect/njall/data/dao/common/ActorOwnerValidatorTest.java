package com.larpconnect.njall.data.dao.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class ActorOwnerValidatorTest {

  @Test
  @DisplayName("validateActorOwner succeeds when owner is an active Actor entity")
  void validateActorOwner_success() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var entityId = new EntityId(tenantId, ownerId);
    var actorEntity =
        new EntityBaseEntity(
            tenantId,
            ownerId,
            ownerId,
            "Actor",
            "Actor Summary",
            Instant.now(),
            Instant.now(),
            null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(actorEntity);

    assertThatCode(() -> ActorOwnerValidator.validateActorOwner(session, tenantId, ownerId))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("validateActorOwner throws when owner entity does not exist")
  void validateActorOwner_notFound() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var entityId = new EntityId(tenantId, ownerId);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    assertThatThrownBy(() -> ActorOwnerValidator.validateActorOwner(session, tenantId, ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  @DisplayName("validateActorOwner throws when owner entity is soft-deleted")
  void validateActorOwner_deleted() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var entityId = new EntityId(tenantId, ownerId);
    var deletedActor =
        new EntityBaseEntity(
            tenantId,
            ownerId,
            ownerId,
            "Actor",
            "Actor Summary",
            Instant.now(),
            Instant.now(),
            Instant.now());

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedActor);

    assertThatThrownBy(() -> ActorOwnerValidator.validateActorOwner(session, tenantId, ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("deleted");
  }

  @Test
  @DisplayName("validateActorOwner throws when owner entity is not of type Actor")
  void validateActorOwner_notActor() {
    var session = mock(Session.class);
    var tenantId = UUID.randomUUID();
    var ownerId = UUID.randomUUID();
    var entityId = new EntityId(tenantId, ownerId);
    var individualEntity =
        new EntityBaseEntity(
            tenantId,
            ownerId,
            UUID.randomUUID(),
            "Individual",
            "Individual Summary",
            Instant.now(),
            Instant.now(),
            null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(individualEntity);

    assertThatThrownBy(() -> ActorOwnerValidator.validateActorOwner(session, tenantId, ownerId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be 'Actor'");
  }
}
