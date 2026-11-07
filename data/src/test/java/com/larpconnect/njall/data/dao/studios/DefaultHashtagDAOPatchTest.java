package com.larpconnect.njall.data.dao.studios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

final class DefaultHashtagDAOPatchTest {

  private SessionFactory sessionFactory;
  private Session session;
  private Transaction tx;
  private DefaultHashtagDAO dao;

  private final UUID tenantId = UUID.randomUUID();
  private final UUID hashtagId = UUID.randomUUID();
  private final UUID owner = UUID.randomUUID();
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

  private EntityBaseEntity defaultEntity() {
    return new EntityBaseEntity(tenantId, hashtagId, owner, "Hashtag", "Aesthetic", now, now, null);
  }

  private EntityBaseEntity deletedEntity() {
    return new EntityBaseEntity(tenantId, hashtagId, owner, "Hashtag", "Aesthetic", now, now, now);
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
  private void mockTenantConfigQuery() {
    NativeQuery<String> nativeQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(
            ArgumentMatchers.contains("set_config"), ArgumentMatchers.eq(String.class)))
        .thenReturn(nativeQuery);
    when(nativeQuery.setParameter("tenantId", tenantId.toString())).thenReturn(nativeQuery);
    when(nativeQuery.getSingleResult()).thenReturn(tenantId.toString());
  }
}
