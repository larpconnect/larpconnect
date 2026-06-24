package org.larpconnect.data.dao;

import org.larpconnect.data.entity.LarpCharacter;

/** DAO interface for managing LarpCharacter entities. */
public sealed interface LarpCharacterDao extends BaseDao<LarpCharacter, LarpCharacterDao>
    permits DefaultLarpCharacterDao {}
