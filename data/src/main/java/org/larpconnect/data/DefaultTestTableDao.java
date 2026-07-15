package org.larpconnect.data;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/** Hibernate-backed implementation of {@link TestTableDao}. */
@ThreadSafe
final class DefaultTestTableDao implements TestTableDao {
  private final Provider<SessionFactory> sessionFactoryProvider;

  @Inject
  DefaultTestTableDao(Provider<SessionFactory> sessionFactoryProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public void save(TestTable entity) {
    Transaction transaction = null;
    try (Session session = sessionFactoryProvider.get().openSession()) {
      transaction = session.beginTransaction();
      session.merge(entity);
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        try {
          if (transaction.isActive()) {
            transaction.rollback();
          }
        } catch (Exception rollbackEx) {
          e.addSuppressed(rollbackEx);
        }
      }
      throw e;
    }
  }

  @Override
  public Optional<TestTable> findById(UUID id) {
    try (Session session = sessionFactoryProvider.get().openSession()) {
      return Optional.ofNullable(session.find(TestTable.class, id));
    }
  }
}
