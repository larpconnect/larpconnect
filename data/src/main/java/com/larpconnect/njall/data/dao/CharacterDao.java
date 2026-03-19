package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.Character;
import java.util.UUID;

/** DAO for Character. */
@DefaultImplementation(DefaultCharacterDao.class)
public interface CharacterDao extends Dao<Character, UUID> {}
