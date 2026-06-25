package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.Collection;

/** Default implementation of CollectionDao. */
final class DefaultCollectionDao extends AbstractDao<Collection, CollectionDao>
    implements CollectionDao {
  @Inject
  DefaultCollectionDao(SessionFactory sessionFactory) {
    super(sessionFactory, Collection.class);
  }
}
