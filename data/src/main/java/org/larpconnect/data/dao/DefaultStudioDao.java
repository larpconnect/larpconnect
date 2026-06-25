package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Studio;

/** Default implementation of StudioDao. */
final class DefaultStudioDao extends AbstractDao<Studio, StudioDao> implements StudioDao {
  @Inject
  DefaultStudioDao(SessionFactory sessionFactory) {
    super(sessionFactory, Studio.class);
  }
}
