package org.larpconnect.data.dao;

import org.larpconnect.data.entity.Actor;

/** DAO interface for managing Actor entities. */
public sealed interface ActorDao extends BaseDao<Actor, ActorDao> permits DefaultActorDao {}
