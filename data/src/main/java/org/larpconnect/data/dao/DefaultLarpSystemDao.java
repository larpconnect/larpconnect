package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.LarpSystem;

/** Default implementation of LarpSystemDao. */
final class DefaultLarpSystemDao extends AbstractDao<LarpSystem, LarpSystemDao>
    implements LarpSystemDao {
  @Inject
  DefaultLarpSystemDao(SessionFactory sessionFactory) {
    super(sessionFactory, LarpSystem.class);
  }
}
