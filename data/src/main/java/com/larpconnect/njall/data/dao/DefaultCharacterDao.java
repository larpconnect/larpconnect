package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.BuildWith;
import com.larpconnect.njall.data.entity.Character;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

@BuildWith(DaoModule.class)
final class DefaultCharacterDao extends AbstractDao<Character, UUID> implements CharacterDao {

  @Inject
  DefaultCharacterDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Character.class);
  }
}
