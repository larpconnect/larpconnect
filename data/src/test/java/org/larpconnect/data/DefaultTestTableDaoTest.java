package org.larpconnect.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultTestTableDao}. */
public final class DefaultTestTableDaoTest {
  private SessionFactory sessionFactory;
  private Session session;
  private Transaction transaction;
  private DefaultTestTableDao dao;

  @BeforeEach
  public void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);
    transaction = mock(Transaction.class);
    when(sessionFactory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
    dao = new DefaultTestTableDao(sessionFactory);
  }

  @Test
  public void save_success_commitsTransaction() {
    TestTableEntity entity = new TestTableEntity(UUID.randomUUID(), "Test Entity");
    dao.save(entity);

    verify(sessionFactory).openSession();
    verify(session).beginTransaction();
    verify(session).merge(entity);
    verify(transaction).commit();
  }

  @Test
  public void save_failure_rollsBackTransaction() {
    TestTableEntity entity = new TestTableEntity(UUID.randomUUID(), "Test Entity");
    doThrow(new RuntimeException("Database error")).when(session).merge(any());

    assertThatThrownBy(() -> dao.save(entity)).isInstanceOf(RuntimeException.class);

    verify(transaction).rollback();
  }

  @Test
  public void save_failureNoTransaction_doesNotRollback() {
    TestTableEntity entity = new TestTableEntity(UUID.randomUUID(), "Test Entity");
    when(session.beginTransaction()).thenThrow(new RuntimeException("Connection error"));

    assertThatThrownBy(() -> dao.save(entity)).isInstanceOf(RuntimeException.class);
  }

  @Test
  public void findById_returnsExpectedEntity() {
    UUID id = UUID.randomUUID();
    TestTableEntity entity = new TestTableEntity(id, "Found Entity");
    when(session.find(TestTableEntity.class, id)).thenReturn(entity);

    Optional<TestTableEntity> result = dao.findById(id);

    assertThat(result).hasValue(entity);
    verify(sessionFactory).openSession();
    verify(session).find(TestTableEntity.class, id);
  }
}
