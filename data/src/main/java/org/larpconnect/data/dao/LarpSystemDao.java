package org.larpconnect.data.dao;

import org.larpconnect.data.entity.LarpSystem;

/** DAO interface for managing LarpSystem entities. */
public sealed interface LarpSystemDao extends BaseDao<LarpSystem, LarpSystemDao>
    permits DefaultLarpSystemDao {}
