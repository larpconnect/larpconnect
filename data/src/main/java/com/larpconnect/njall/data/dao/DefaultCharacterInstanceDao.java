package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.CharacterInstance;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultCharacterInstanceDao extends AbstractDao<CharacterInstance, UUID>
    implements CharacterInstanceDao {

  @Inject
  DefaultCharacterInstanceDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, CharacterInstance.class);
  }
}
