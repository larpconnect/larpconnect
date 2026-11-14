package com.larpconnect.njall.data.dao.studios.campaigns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultCampaignDAOCreateTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultCampaignDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
  private final UUID systemId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultCampaignDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("create persists entity and campaign records with active system and summary")
  void create_withSystemAndSummary_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    var activeSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Ruleset", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(activeSystem);

    var created =
        dao.create(
            tenantId,
            owner,
            "Seattle By Night",
            Optional.of("Camarilla chronicle"),
            Optional.of(systemId));

    assertThat(created.title()).isEqualTo("Seattle By Night");
    assertThat(created.generator()).isEqualTo(owner);
    assertThat(created.summary()).contains("Camarilla chronicle");
    assertThat(created.systemId()).contains(systemId);
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(CampaignEntity.class));
    verify(session).flush();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create persists systemless campaign when optional summary and systemId are empty")
  void create_withEmptyOptionals_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);

    var created =
        dao.create(tenantId, owner, "Freeform Anthology", Optional.empty(), Optional.empty());

    assertThat(created.title()).isEqualTo("Freeform Anthology");
    assertThat(created.generator()).isEqualTo(owner);
    assertThat(created.summary()).isEmpty();
    assertThat(created.systemId()).isEmpty();
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(CampaignEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rejects missing systemId")
  void create_missingSystem_throws() {
    assertInvalidSystemRejected(null);
  }

  @Test
  @DisplayName("create rejects soft-deleted systemId")
  void create_deletedSystem_throws() {
    var deletedSystem =
        new EntityBaseEntity(tenantId, systemId, owner, "System", "Ruleset", now, now, now);
    assertInvalidSystemRejected(deletedSystem);
  }

  @Test
  @DisplayName("create rejects non-System entity for systemId")
  void create_nonSystemEntity_throws() {
    var wrongType =
        new EntityBaseEntity(tenantId, systemId, owner, "Location", "Venue", now, now, null);
    assertInvalidSystemRejected(wrongType);
  }

  private void assertInvalidSystemRejected(@Nullable EntityBaseEntity systemEntity) {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var actorOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(actorOwner);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, systemId)))
        .thenReturn(systemEntity);

    assertThatThrownBy(
            () ->
                dao.create(tenantId, owner, "Invalid Ref", Optional.empty(), Optional.of(systemId)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator does not exist")
  void create_generatorNotFound_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner))).thenReturn(null);

    assertThatThrownBy(
            () -> dao.create(tenantId, owner, "Orphan", Optional.empty(), Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator is soft-deleted")
  void create_generatorDeleted_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var deletedOwner =
        new EntityBaseEntity(tenantId, owner, owner, "Actor", "Owner Bio", now, now, now);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner)))
        .thenReturn(deletedOwner);

    assertThatThrownBy(
            () -> dao.create(tenantId, owner, "Orphan", Optional.empty(), Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create throws when generator is not an Actor")
  void create_generatorNotActor_throws() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    var nonActor =
        new EntityBaseEntity(tenantId, owner, owner, "Location", "Owner Bio", now, now, null);
    when(session.find(EntityBaseEntity.class, new EntityId(tenantId, owner))).thenReturn(nonActor);

    assertThatThrownBy(
            () -> dao.create(tenantId, owner, "Orphan", Optional.empty(), Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class);
    verify(tx).rollback();
  }

  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock();
    when(session.createNativeQuery(contains("SELECT set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
