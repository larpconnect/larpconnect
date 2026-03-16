package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.LarpCharacter;
import java.util.UUID;

@DefaultImplementation(DefaultLarpCharacterDao.class)
public interface LarpCharacterDao extends Dao<LarpCharacter, UUID> {}
