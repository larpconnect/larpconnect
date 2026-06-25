package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Collection;

/** DAO interface for managing Collection entities. */
public sealed interface CollectionDao extends BaseDao<Collection, CollectionDao>
    permits DefaultCollectionDao {}
