package com.larpconnect.njall.data.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class DefaultStudioDAOTest {

  private SessionFactory sessionFactory;
  private Session session;
  private DefaultStudioDAO dao;

  private final UUID tenantId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultStudioDAO(() -> sessionFactory);
  }

  @Test
  @DisplayName("findById sets app.tenant_id in transaction and returns Studio")
  @SuppressWarnings("unchecked")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    Query<StudioEntity> hqlQuery = mock(Query.class);
    when(session.createQuery(contains("from StudioEntity"), eq(StudioEntity.class)))
        .thenReturn(hqlQuery);
    when(hqlQuery.setParameter("tenantId", tenantId)).thenReturn(hqlQuery);
    when(hqlQuery.uniqueResult()).thenReturn(new StudioEntity(tenantId, "Valhalla"));

    var result = dao.findById(tenantId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(tenantId);
    assertThat(result.get().name()).isEqualTo("Valhalla");
    verify(tx).commit();
  }

  @Test
  @DisplayName("getStudio delegates to findById")
  @SuppressWarnings("unchecked")
  void getStudio_delegatesToFindById() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    Query<StudioEntity> hqlQuery = mock(Query.class);
    when(session.createQuery(contains("from StudioEntity"), eq(StudioEntity.class)))
        .thenReturn(hqlQuery);
    when(hqlQuery.setParameter("tenantId", tenantId)).thenReturn(hqlQuery);
    when(hqlQuery.uniqueResult()).thenReturn(new StudioEntity(tenantId, "Valhalla"));

    var result = dao.getStudio(tenantId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(tenantId);
    assertThat(result.get().name()).isEqualTo("Valhalla");
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity not found")
  @SuppressWarnings("unchecked")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    Query<StudioEntity> hqlQuery = mock(Query.class);
    when(session.createQuery(contains("from StudioEntity"), eq(StudioEntity.class)))
        .thenReturn(hqlQuery);
    when(hqlQuery.setParameter("tenantId", tenantId)).thenReturn(hqlQuery);
    when(hqlQuery.uniqueResult()).thenReturn(null);

    var result = dao.findById(tenantId);

    assertThat(result).isEmpty();
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById rollbacks on exception")
  @SuppressWarnings("unchecked")
  void findById_rollbackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));

    assertThatThrownBy(() -> dao.findById(tenantId)).isInstanceOf(RuntimeException.class);
    verify(tx).rollback();
  }

  @Test
  @DisplayName("list throws UnsupportedOperationException in user space")
  void list_unsupported() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
  }
}
