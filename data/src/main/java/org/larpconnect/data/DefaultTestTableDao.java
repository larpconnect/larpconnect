package org.larpconnect.data;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.concurrent.ThreadSafe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/** Hibernate-backed implementation of {@link TestTableDao}. */
@ThreadSafe
final class DefaultTestTableDao implements TestTableDao {
  private final SessionFactory sessionFactory;

  @Inject
  DefaultTestTableDao(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void save(TestTableEntity entity) {
    Transaction transaction = null;
    try (Session session = sessionFactory.openSession()) {
      transaction = session.beginTransaction();
      session.merge(entity);
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }
  }

  @Override
  public Optional<TestTableEntity> findById(UUID id) {
    try (Session session = sessionFactory.openSession()) {
      return Optional.ofNullable(session.find(TestTableEntity.class, id));
    }
  }
}
