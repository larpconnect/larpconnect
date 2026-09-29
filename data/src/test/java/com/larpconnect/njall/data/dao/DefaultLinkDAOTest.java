package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultLinkDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultLinkDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID linkId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultLinkDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById sets app.tenant_id and returns active Link")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Homepage", now, now, null);
    var link = new LinkEntity(tenantId, linkId, "website", "https://example.com", "text/html");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);

    var result = dao.findById(tenantId, linkId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(linkId);
    assertThat(result.orElseThrow().linkType()).isEqualTo("website");
    assertThat(result.orElseThrow().url()).isEqualTo("https://example.com");
    assertThat(result.orElseThrow().mediaType()).isEqualTo("text/html");
    assertThat(result.orElseThrow().summary()).contains("Homepage");
    assertThat(result.orElseThrow().isDeleted()).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity is soft-deleted")
  void findById_softDeleted_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Homepage", now, now, now);
    var link = new LinkEntity(tenantId, linkId, "website", "https://example.com", "text/html");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);

    var result = dao.findById(tenantId, linkId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity is missing")
  void findById_missingEntity_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, linkId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when link is missing but entity exists")
  void findById_missingLink_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Homepage", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, linkId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById rollbacks transaction on error")
  void findById_error_rollbacks() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("Connection failed"));

    assertThatThrownBy(() -> dao.findById(tenantId, linkId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Connection failed");
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create inserts entity and link and returns Link record")
  @SuppressWarnings("unchecked")
  void create_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    mockTenantConfigQuery();

    NativeQuery<UUID> insertEntityQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("INSERT INTO njall_users.entities"), eq(UUID.class)))
        .thenReturn(insertEntityQuery);
    when(insertEntityQuery.setParameter(any(String.class), any())).thenReturn(insertEntityQuery);
    when(insertEntityQuery.getSingleResult()).thenReturn(linkId);

    NativeQuery<Void> insertLinkQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("INSERT INTO njall_users.links"), eq(Void.class)))
        .thenReturn(insertLinkQuery);
    when(insertLinkQuery.setParameter(any(String.class), any())).thenReturn(insertLinkQuery);

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "My Discord", now, now, null);
    var link = new LinkEntity(tenantId, linkId, "discord", "https://discord.gg/test", "text/html");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);

    var result =
        dao.create(
            tenantId, "discord", "https://discord.gg/test", "text/html", Optional.of("My Discord"));

    assertThat(result.id()).isEqualTo(linkId);
    assertThat(result.linkType()).isEqualTo("discord");
    assertThat(result.summary()).contains("My Discord");
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rollbacks on exception")
  void create_error_rollbacks() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    when(session.createNativeQuery(contains("INSERT INTO njall_users.entities"), eq(UUID.class)))
        .thenThrow(new RuntimeException("Insert error"));

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId, "discord", "https://discord.gg/test", "text/html", Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch updates fields and returns updated Link")
  void patch_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Old Summary", now, now, null);
    var link = new LinkEntity(tenantId, linkId, "website", "https://old.example.com", "text/html");

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);

    var result =
        dao.patch(
            tenantId,
            linkId,
            Optional.of("new_type"),
            Optional.of("https://new.example.com"),
            Optional.of("application/json"),
            Optional.of("New Summary"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().linkType()).isEqualTo("new_type");
    assertThat(result.orElseThrow().url()).isEqualTo("https://new.example.com");
    assertThat(result.orElseThrow().mediaType()).isEqualTo("application/json");
    assertThat(result.orElseThrow().summary()).contains("New Summary");
    verify(session).merge(entity);
    verify(session).merge(link);
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when entity not found")
  void patch_entityNotFound_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(
            tenantId,
            linkId,
            Optional.empty(),
            Optional.of("https://new.example.com"),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when link is missing but entity exists")
  void patch_linkMissing_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Old Summary", now, now, null);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(null);

    var result =
        dao.patch(
            tenantId,
            linkId,
            Optional.empty(),
            Optional.of("https://new.example.com"),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when entity is soft-deleted")
  void patch_softDeleted_returnsEmpty() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Old Summary", now, now, now);
    var link = new LinkEntity(tenantId, linkId, "website", "https://old.example.com", "text/html");
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);

    var result =
        dao.patch(
            tenantId,
            linkId,
            Optional.empty(),
            Optional.of("https://new.example.com"),
            Optional.empty(),
            Optional.empty());

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch rollbacks on exception")
  void patch_error_rollbacks() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(
            () ->
                dao.patch(
                    tenantId,
                    linkId,
                    Optional.empty(),
                    Optional.of("https://new.example.com"),
                    Optional.empty(),
                    Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete sets deletedOn and returns true")
  void softDelete_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Summary", now, now, null);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var success = dao.softDelete(tenantId, linkId);

    assertThat(success).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity not found")
  void softDelete_notFound_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var success = dao.softDelete(tenantId, linkId);

    assertThat(success).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity is already deleted")
  void softDelete_alreadyDeleted_returnsFalse() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();

    var entityId = new EntityId(tenantId, linkId);
    var entity = new EntityBaseEntity(tenantId, linkId, "Link", "Summary", now, now, now);

    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var success = dao.softDelete(tenantId, linkId);

    assertThat(success).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete rollbacks on error")
  void softDelete_error_rollbacks() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, linkId)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("list throws UnsupportedOperationException in user space")
  void list_unsupported() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("findById without tenant context throws UnsupportedOperationException")
  void findByIdWithoutTenant_throws() {
    assertThatThrownBy(() -> dao.findById(linkId))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @SuppressWarnings("unchecked")
  private void mockTenantConfigQuery() {
    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
