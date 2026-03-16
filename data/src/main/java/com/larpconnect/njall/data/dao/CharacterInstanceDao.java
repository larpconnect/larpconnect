package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.CharacterInstance;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultCharacterInstanceDao.class)
public interface CharacterInstanceDao {
  Uni<CharacterInstance> findById(UUID id);

  Uni<CharacterInstance> persist(CharacterInstance entity);
}
