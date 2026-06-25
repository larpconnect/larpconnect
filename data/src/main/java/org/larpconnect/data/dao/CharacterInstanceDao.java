package org.larpconnect.data.dao;

import org.larpconnect.data.entity.CharacterInstance;

/** DAO interface for managing CharacterInstance entities. */
public sealed interface CharacterInstanceDao
    extends BaseDao<CharacterInstance, CharacterInstanceDao> permits DefaultCharacterInstanceDao {}
