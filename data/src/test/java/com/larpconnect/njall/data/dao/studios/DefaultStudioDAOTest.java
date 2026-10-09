package com.larpconnect.njall.data.dao.studios;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findById_success() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    when(session.find(StudioEntity.class, tenantId))
        .thenReturn(new StudioEntity(tenantId, "Valhalla"));

    var result = dao.findById(tenantId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(tenantId);
    assertThat(result.orElseThrow().name()).isEqualTo("Valhalla");
    verify(session).find(StudioEntity.class, tenantId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("getStudio delegates to findById")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void getStudio_delegatesToFindById() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    when(session.find(StudioEntity.class, tenantId))
        .thenReturn(new StudioEntity(tenantId, "Valhalla"));

    var result = dao.getStudio(tenantId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().id()).isEqualTo(tenantId);
    assertThat(result.orElseThrow().name()).isEqualTo("Valhalla");
    verify(session).find(StudioEntity.class, tenantId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById returns empty when entity not found")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findById_notFound() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenReturn(tenantId.toString());

    when(session.find(StudioEntity.class, tenantId)).thenReturn(null);

    var result = dao.findById(tenantId);

    assertThat(result).isEmpty();
    verify(session).find(StudioEntity.class, tenantId);
    verify(tx).commit();
  }

  @Test
  @DisplayName("findById rollbacks on exception")
  // Suppressed due to Hibernate generic query mocking type erasure.
  @SuppressWarnings("unchecked")
  void findById_rollbackOnException() {
    var tx = mock(Transaction.class);
    when(session.beginTransaction()).thenReturn(tx);

    NativeQuery<String> configQuery = mock(NativeQuery.class);
    when(session.createNativeQuery(contains("set_config"), eq(String.class)))
        .thenReturn(configQuery);
    when(configQuery.setParameter(eq("tenantId"), any())).thenReturn(configQuery);
    when(configQuery.getSingleResult()).thenThrow(new RuntimeException("DB error"));
    Mockito.doThrow(new RuntimeException("Rollback error")).when(tx).rollback();

    assertThatThrownBy(() -> dao.findById(tenantId))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("DB error")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback error");
            });
    verify(tx).rollback();
  }

  @Test
  @DisplayName("list throws UnsupportedOperationException in user space")
  void list_unsupported() {
    assertThatThrownBy(() -> dao.list()).isInstanceOf(UnsupportedOperationException.class);
  }
}
