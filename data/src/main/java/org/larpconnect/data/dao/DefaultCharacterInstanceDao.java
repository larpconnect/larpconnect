package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.CharacterInstance;

/** Default implementation of CharacterInstanceDao. */
final class DefaultCharacterInstanceDao extends AbstractDao<CharacterInstance, CharacterInstanceDao>
    implements CharacterInstanceDao {
  @Inject
  DefaultCharacterInstanceDao(SessionFactory sessionFactory) {
    super(sessionFactory, CharacterInstance.class);
  }
}
