package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.CharacterInstance;
import java.util.UUID;

/** DAO for CharacterInstance. */
@DefaultImplementation(DefaultCharacterInstanceDao.class)
public interface CharacterInstanceDao extends Dao<CharacterInstance, UUID> {}
