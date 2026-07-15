package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Provider;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link DefaultTestTableDao}. */
@ExtendWith(MockitoExtension.class)
public final class DefaultTestTableDaoTest {
  private static final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private Provider<SessionFactory> sessionFactoryProvider;
  @Mock private SessionFactory sessionFactory;
  @Mock private Session session;
  @Mock private Transaction transaction;

  private DefaultTestTableDao dao;

  @BeforeEach
  public void setUp() {
    when(sessionFactoryProvider.get()).thenReturn(sessionFactory);
    when(sessionFactory.openSession()).thenReturn(session);
    dao = new DefaultTestTableDao(sessionFactoryProvider);
  }

  @Test
  public void save_success_commitsTransaction() {
    TestTable entity = new TestTable(TEST_UUID, "Test Entity");
    when(session.beginTransaction()).thenReturn(transaction);
    dao.save(entity);

    // TODO: This test is too brittle for how little value it provides. We can
    // either remove it or add ordering to make it more valuable.
    verify(sessionFactoryProvider).get();
    verify(sessionFactory).openSession();
    verify(session).beginTransaction();
    verify(session).merge(entity);
    verify(transaction).commit();
  }

  @Test
  public void save_failure_rollsBackTransaction() {
    TestTable entity = new TestTable(TEST_UUID, "Test Entity");
    when(session.beginTransaction()).thenReturn(transaction);
    doThrow(new RuntimeException("Database error")).when(session).merge(any());
    when(transaction.isActive()).thenReturn(true);

    assertThatThrownBy(() -> dao.save(entity)).isInstanceOf(RuntimeException.class);

    verify(transaction).rollback();
  }

  @Test
  public void save_transactionInactive_doesNotRollback() {
    TestTable entity = new TestTable(TEST_UUID, "Test Entity");
    when(session.beginTransaction()).thenReturn(transaction);
    doThrow(new RuntimeException("Database error")).when(session).merge(any());
    when(transaction.isActive()).thenReturn(false);

    assertThatThrownBy(() -> dao.save(entity)).isInstanceOf(RuntimeException.class);

    verify(transaction, never()).rollback();
  }

  @Test
  public void save_failureNoTransaction_doesNotRollback() {
    TestTable entity = new TestTable(TEST_UUID, "Test Entity");
    when(session.beginTransaction()).thenThrow(new RuntimeException("Connection error"));

    assertThatThrownBy(() -> dao.save(entity)).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void save_rollbackFails_suppressesException() {
    TestTable entity = new TestTable(TEST_UUID, "Test Entity");
    when(session.beginTransaction()).thenReturn(transaction);
    doThrow(new RuntimeException("Database error")).when(session).merge(any());
    when(transaction.isActive()).thenReturn(true);
    doThrow(new RuntimeException("Rollback error")).when(transaction).rollback();

    assertThatThrownBy(() -> dao.save(entity))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database error")
        .satisfies(
            e -> {
              assertThat(e.getSuppressed()).hasSize(1);
              assertThat(e.getSuppressed()[0]).hasMessage("Rollback error");
            });
  }

  @Test
  public void findById_returnsExpectedEntity() {
    TestTable entity = new TestTable(TEST_UUID, "Found Entity");
    when(session.find(TestTable.class, TEST_UUID)).thenReturn(entity);

    Optional<TestTable> result = dao.findById(TEST_UUID);

    assertThat(result).hasValue(entity);
    verify(sessionFactoryProvider).get();
    verify(sessionFactory).openSession();
    verify(session).find(TestTable.class, TEST_UUID);
  }
}
