package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.larpconnect.njall.data.dao.common.EntityBaseEntity;
import com.larpconnect.njall.data.dao.common.EntityId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultHashtagDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private Transaction tx;
  private DefaultHashtagDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID hashtagId = UUID.randomUUID();
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    tx = mock(Transaction.class);
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(tx);
    mockTenantConfigQuery();
    dao = new DefaultHashtagDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById sets app.tenant_id and returns active Hashtag")
  void findById_success() {
    mockFindEntities(defaultEntity(), defaultLink(), defaultHashtag());

    var result = dao.findById(tenantId, hashtagId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(hashtagId);
    assertThat(result.orElseThrow().tag()).isEqualTo("SolarPunk");
    assertThat(result.orElseThrow().summary()).contains("Aesthetic");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity not found")
  void findById_notFound_returnsEmpty() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.findById(tenantId, hashtagId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity is soft-deleted")
  void findById_softDeleted_returnsEmpty() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedEntity());

    var result = dao.findById(tenantId, hashtagId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when link or hashtagEntity is null")
  void findById_nullLinkOrHashtagEntity_returnsEmpty() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(defaultEntity());
    when(session.find(LinkEntity.class, entityId)).thenReturn(null);

    assertThat(dao.findById(tenantId, hashtagId)).isEmpty();

    when(session.find(LinkEntity.class, entityId)).thenReturn(defaultLink());
    when(session.find(HashtagEntity.class, entityId)).thenReturn(null);

    assertThat(dao.findById(tenantId, hashtagId)).isEmpty();
  }

  @Test
  @DisplayName("findById rolls back transaction on exception")
  void findById_exception_rollsBack() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> dao.findById(tenantId, hashtagId))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("findByTag performs case-insensitive lookup and returns Hashtag")
  void findByTag_success() {
    mockFindTagQuery(List.of(hashtagId));
    mockFindEntities(defaultEntity(), defaultLink(), defaultHashtag());

    var result = dao.findByTag(tenantId, "solarpunk");

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().tag()).isEqualTo("SolarPunk");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findByTag returns empty when tag not found")
  void findByTag_notFound_returnsEmpty() {
    mockFindTagQuery(List.of());

    var result = dao.findByTag(tenantId, "unknown");

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findByTag returns empty when tag found but entity is soft-deleted")
  void findByTag_softDeleted_returnsEmpty() {
    mockFindTagQuery(List.of(hashtagId));
    mockFindEntities(deletedEntity(), defaultLink(), defaultHashtag());

    var result = dao.findByTag(tenantId, "solarpunk");

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findByTag rolls back transaction on exception")
  void findByTag_exception_rollsBack() {
    when(session.createQuery(contains("SELECT h.id FROM HashtagEntity h"), eq(UUID.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> dao.findByTag(tenantId, "solarpunk"))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("listAll returns active hashtags ordered by tag")
  void listAll_success() {
    @SuppressWarnings("unchecked")
    Query<Object[]> query = mock(Query.class);
    when(session.createQuery(
            contains("FROM EntityBaseEntity e, LinkEntity l, HashtagEntity h"), eq(Object[].class)))
        .thenReturn(query);
    when(query.setParameter("tenantId", tenantId)).thenReturn(query);
    when(query.getResultList())
        .thenReturn(
            List.<Object[]>of(new Object[] {defaultEntity(), defaultLink(), defaultHashtag()}));

    var list = dao.listAll(tenantId);

    assertThat(list).hasSize(1);
    assertThat(list.getFirst().tag()).isEqualTo("SolarPunk");
    verify(tx).commit();
  }

  @Test
  @DisplayName("listAll rolls back transaction on exception")
  void listAll_exception_rollsBack() {
    when(session.createQuery(
            contains("FROM EntityBaseEntity e, LinkEntity l, HashtagEntity h"), eq(Object[].class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> dao.listAll(tenantId)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("create persists new Hashtag when not present")
  void create_persistsNew() {
    mockFindTagQuery(List.of());

    var result =
        dao.create(
            tenantId,
            "SolarPunk",
            "/api/studios/valiant/v1/tags/SolarPunk",
            Optional.of("Aesthetic"));

    assertThat(result.tag()).isEqualTo("SolarPunk");
    verify(session).persist(any(EntityBaseEntity.class));
    verify(session).persist(any(LinkEntity.class));
    verify(session).persist(any(HashtagEntity.class));
    verify(tx).commit();
  }

  @Test
  @DisplayName("create reactivates soft-deleted hashtag if already exists")
  void create_reactivatesSoftDeleted() {
    mockFindTagQuery(List.of(hashtagId));
    var entity = new EntityBaseEntity(tenantId, hashtagId, "Hashtag", "Old", now, now, now);
    mockFindEntities(entity, defaultLink(), defaultHashtag());

    var result =
        dao.create(
            tenantId,
            "SolarPunk",
            "/api/studios/valiant/v1/tags/SolarPunk",
            Optional.of("Reactivated"));

    assertThat(result.tag()).isEqualTo("SolarPunk");
    assertThat(entity.getDeletedOn()).isNull();
    assertThat(entity.getSummary()).isEqualTo("Reactivated");
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("create returns existing active tag without modifying deletedOn")
  void create_existingActiveTag_returnsWithoutModification() {
    mockFindTagQuery(List.of(hashtagId));
    var entity = defaultEntity();
    mockFindEntities(entity, defaultLink(), defaultHashtag());

    var result =
        dao.create(
            tenantId, "SolarPunk", "/api/studios/valiant/v1/tags/SolarPunk", Optional.empty());

    assertThat(result.tag()).isEqualTo("SolarPunk");
    assertThat(entity.getDeletedOn()).isNull();
    verify(tx).commit();
  }

  @Test
  @DisplayName("create rolls back transaction on exception")
  void create_exception_rollsBack() {
    when(session.createQuery(contains("SELECT h.id FROM HashtagEntity h"), eq(UUID.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(
            () ->
                dao.create(
                    tenantId,
                    "SolarPunk",
                    "/api/studios/valiant/v1/tags/SolarPunk",
                    Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("batchCreate creates multiple hashtags")
  void batchCreate_success() {
    mockFindTagQuery(List.of());

    var result =
        dao.batchCreate(
            tenantId,
            List.of(
                new HashtagDAO.TagCreationItem(
                    "SolarPunk", "/api/studios/valiant/v1/tags/SolarPunk", Optional.empty())));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tag()).isEqualTo("SolarPunk");
    verify(tx).commit();
  }

  @Test
  @DisplayName("batchCreate rolls back transaction on exception")
  void batchCreate_exception_rollsBack() {
    when(session.createQuery(contains("SELECT h.id FROM HashtagEntity h"), eq(UUID.class)))
        .thenThrow(new RuntimeException("Database error"));

    var items =
        List.of(
            new HashtagDAO.TagCreationItem(
                "SolarPunk", "/api/studios/valiant/v1/tags/SolarPunk", Optional.empty()));
    assertThatThrownBy(() -> dao.batchCreate(tenantId, items)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("softDelete sets deleted_on and returns true")
  void softDelete_success() {
    var entityId = new EntityId(tenantId, hashtagId);
    var entity = defaultEntity();
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);

    var result = dao.softDelete(tenantId, hashtagId);

    assertThat(result).isTrue();
    assertThat(entity.getDeletedOn()).isNotNull();
    verify(session).merge(entity);
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity not found")
  void softDelete_notFound_returnsFalse() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    var result = dao.softDelete(tenantId, hashtagId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete returns false when entity already deleted")
  void softDelete_alreadyDeleted_returnsFalse() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(deletedEntity());

    var result = dao.softDelete(tenantId, hashtagId);

    assertThat(result).isFalse();
    verify(tx).commit();
  }

  @Test
  @DisplayName("softDelete rolls back transaction on exception")
  void softDelete_exception_rollsBack() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> dao.softDelete(tenantId, hashtagId))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("patch updates tag and summary successfully")
  void patch_success() {
    mockFindEntities(defaultEntity(), defaultLink(), defaultHashtag());

    var result =
        dao.patch(tenantId, hashtagId, Optional.of("CyberPunk"), Optional.of("New Description"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().tag()).isEqualTo("CyberPunk");
    assertThat(result.orElseThrow().summary()).contains("New Description");
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only tag when summary is empty")
  void patch_onlyTag_success() {
    var hashtag = defaultHashtag();
    mockFindEntities(defaultEntity(), defaultLink(), hashtag);

    var result = dao.patch(tenantId, hashtagId, Optional.of("CyberPunk"), Optional.empty());

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().tag()).isEqualTo("CyberPunk");
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch updates only summary when tag is empty")
  void patch_onlySummary_success() {
    var entity = defaultEntity();
    mockFindEntities(entity, defaultLink(), defaultHashtag());

    var result = dao.patch(tenantId, hashtagId, Optional.empty(), Optional.of("New Description"));

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().summary()).contains("New Description");
    verify(tx).commit();
  }

  @Test
  @DisplayName("patch returns empty when entity not found or deleted")
  void patch_notFoundOrDeleted_returnsEmpty() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(null);

    assertThat(dao.patch(tenantId, hashtagId, Optional.of("CyberPunk"), Optional.empty()))
        .isEmpty();

    mockFindEntities(deletedEntity(), defaultLink(), defaultHashtag());

    assertThat(dao.patch(tenantId, hashtagId, Optional.empty(), Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("patch returns empty when link or hashtagEntity is null")
  void patch_nullLinkOrHashtagEntity_returnsEmpty() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(defaultEntity());
    when(session.find(LinkEntity.class, entityId)).thenReturn(null);

    assertThat(dao.patch(tenantId, hashtagId, Optional.empty(), Optional.empty())).isEmpty();

    when(session.find(LinkEntity.class, entityId)).thenReturn(defaultLink());
    when(session.find(HashtagEntity.class, entityId)).thenReturn(null);

    assertThat(dao.patch(tenantId, hashtagId, Optional.empty(), Optional.empty())).isEmpty();
  }

  @Test
  @DisplayName("patch rollback transaction on exception")
  void patch_exception_rollsBack() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> dao.patch(tenantId, hashtagId, Optional.of("Tag"), Optional.empty()))
        .isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("rollbackQuietly suppresses secondary rollback exception")
  void patch_rollbackQuietly_suppressesRollbackException() {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId))
        .thenThrow(new RuntimeException("Primary DB error"));
    doThrow(new RuntimeException("Secondary rollback error")).when(tx).rollback();

    assertThatThrownBy(() -> dao.patch(tenantId, hashtagId, Optional.of("Tag"), Optional.empty()))
        .isInstanceOf(RuntimeException.class)
        .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
  }

  @Test
  @DisplayName("list throws UnsupportedOperationException")
  void list_throwsException() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
  }

  private EntityBaseEntity defaultEntity() {
    return new EntityBaseEntity(tenantId, hashtagId, "Hashtag", "Aesthetic", now, now, null);
  }

  private EntityBaseEntity deletedEntity() {
    return new EntityBaseEntity(tenantId, hashtagId, "Hashtag", "Aesthetic", now, now, now);
  }

  private LinkEntity defaultLink() {
    return new LinkEntity(
        tenantId,
        hashtagId,
        "hashtag",
        "/api/studios/valiant/v1/tags/SolarPunk",
        "application/json");
  }

  private HashtagEntity defaultHashtag() {
    return new HashtagEntity(tenantId, hashtagId, "SolarPunk");
  }

  private void mockFindEntities(EntityBaseEntity entity, LinkEntity link, HashtagEntity hashtag) {
    var entityId = new EntityId(tenantId, hashtagId);
    when(session.find(EntityBaseEntity.class, entityId)).thenReturn(entity);
    when(session.find(LinkEntity.class, entityId)).thenReturn(link);
    when(session.find(HashtagEntity.class, entityId)).thenReturn(hashtag);
  }

  @SuppressWarnings("unchecked")
  private void mockFindTagQuery(List<UUID> result) {
    Query<UUID> query = mock(Query.class);
    when(session.createQuery(contains("SELECT h.id FROM HashtagEntity h"), eq(UUID.class)))
        .thenReturn(query);
    when(query.setParameter(eq("tenantId"), any(UUID.class))).thenReturn(query);
    when(query.setParameter(eq("lowerTag"), any(String.class))).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

  @SuppressWarnings("unchecked")
  private void mockTenantConfigQuery() {
    NativeQuery<String> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(nativeQuery);
    when(nativeQuery.setParameter("tenantId", tenantId.toString())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
