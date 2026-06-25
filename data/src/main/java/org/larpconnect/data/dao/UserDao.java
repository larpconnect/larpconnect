package org.larpconnect.data.dao;

import org.larpconnect.data.entity.User;

/** DAO interface for managing User entities. */
public sealed interface UserDao extends BaseDao<User, UserDao> permits DefaultUserDao {}
