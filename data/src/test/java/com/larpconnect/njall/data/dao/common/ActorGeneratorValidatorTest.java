package com.larpconnect.njall.data.dao.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

final class ActorGeneratorValidatorTest {

  private final UUID tenantId = UUID.randomUUID();
  private final UUID generatorId = UUID.randomUUID();

  @Test
  void isActiveActor_returnsTrue_whenEntityIsActiveActor() {
    Session session = mock(Session.class);
    EntityBaseEntity actorEntity = activeEntity("Actor");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(actorEntity);

    assertThat(ActorGeneratorValidator.isActiveActor(session, tenantId, generatorId)).isTrue();
  }

  @Test
  void isActiveActor_returnsFalse_whenEntityMissing() {
    Session session = mock(Session.class);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(null);

    assertThat(ActorGeneratorValidator.isActiveActor(session, tenantId, generatorId)).isFalse();
  }

  @Test
  void isActiveActor_returnsFalse_whenEntityDeleted() {
    Session session = mock(Session.class);
    EntityBaseEntity deletedActor = deletedEntity("Actor");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(deletedActor);

    assertThat(ActorGeneratorValidator.isActiveActor(session, tenantId, generatorId)).isFalse();
  }

  @Test
  void isActiveActor_returnsFalse_whenEntityIsNotActor() {
    Session session = mock(Session.class);
    EntityBaseEntity individualEntity = activeEntity("Individual");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(individualEntity);

    assertThat(ActorGeneratorValidator.isActiveActor(session, tenantId, generatorId)).isFalse();
  }

  @Test
  void validateActorGenerator_succeeds_whenEntityIsActiveActor() {
    Session session = mock(Session.class);
    EntityBaseEntity actorEntity = activeEntity("Actor");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(actorEntity);

    assertThatCode(
            () -> ActorGeneratorValidator.validateActorGenerator(session, tenantId, generatorId))
        .doesNotThrowAnyException();
  }

  @Test
  void validateActorGenerator_throws_whenEntityMissing() {
    Session session = mock(Session.class);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(null);

    assertThatThrownBy(
            () -> ActorGeneratorValidator.validateActorGenerator(session, tenantId, generatorId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Generator entity does not exist or has been deleted");
  }

  @Test
  void validateActorGenerator_throws_whenEntityDeleted() {
    Session session = mock(Session.class);
    EntityBaseEntity deletedActor = deletedEntity("Actor");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(deletedActor);

    assertThatThrownBy(
            () -> ActorGeneratorValidator.validateActorGenerator(session, tenantId, generatorId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Generator entity does not exist or has been deleted");
  }

  @Test
  void validateActorGenerator_throws_whenEntityIsNotActor() {
    Session session = mock(Session.class);
    EntityBaseEntity individualEntity = activeEntity("Individual");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(individualEntity);

    assertThatThrownBy(
            () -> ActorGeneratorValidator.validateActorGenerator(session, tenantId, generatorId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be 'Actor'");
  }

  @Test
  void validateActiveEntity_succeeds_whenNonActorEntityIsActive() {
    Session session = mock(Session.class);
    EntityBaseEntity individualEntity = activeEntity("Individual");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(individualEntity);

    assertThatCode(
            () -> ActorGeneratorValidator.validateActiveEntity(session, tenantId, generatorId))
        .doesNotThrowAnyException();
  }

  @Test
  void validateActiveEntity_throws_whenEntityMissing() {
    Session session = mock(Session.class);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(null);

    assertThatThrownBy(
            () -> ActorGeneratorValidator.validateActiveEntity(session, tenantId, generatorId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Referenced entity does not exist or has been deleted");
  }

  @Test
  void validateActiveEntity_throws_whenEntityDeleted() {
    Session session = mock(Session.class);
    EntityBaseEntity deletedEntity = deletedEntity("Individual");
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, generatorId)))
        .thenReturn(deletedEntity);

    assertThatThrownBy(
            () -> ActorGeneratorValidator.validateActiveEntity(session, tenantId, generatorId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Referenced entity does not exist or has been deleted");
  }

  private EntityBaseEntity activeEntity(String entityType) {
    Instant now = Instant.now();
    return new EntityBaseEntity(
        tenantId, generatorId, generatorId, entityType, null, now, now, null);
  }

  private EntityBaseEntity deletedEntity(String entityType) {
    EntityBaseEntity entity = activeEntity(entityType);
    entity.setDeletedOn(Instant.now());
    return entity;
  }
}
