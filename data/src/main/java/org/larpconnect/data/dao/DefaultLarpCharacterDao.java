package org.larpconnect.data.dao;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.larpconnect.data.entity.LarpCharacter;

/** Default implementation of LarpCharacterDao. */
final class DefaultLarpCharacterDao extends AbstractDao<LarpCharacter, LarpCharacterDao>
    implements LarpCharacterDao {
  @Inject
  DefaultLarpCharacterDao(SessionFactory sessionFactory) {
    super(sessionFactory, LarpCharacter.class);
  }
}
