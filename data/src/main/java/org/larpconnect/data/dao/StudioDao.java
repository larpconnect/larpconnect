package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Studio;

/** DAO interface for managing Studio entities. */
public sealed interface StudioDao extends BaseDao<Studio, StudioDao> permits DefaultStudioDao {}
