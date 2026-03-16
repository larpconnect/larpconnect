package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.LarpCharacter;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultLarpCharacterDao.class)
public interface LarpCharacterDao {
  Uni<LarpCharacter> findById(UUID id);

  Uni<LarpCharacter> persist(LarpCharacter entity);
}
