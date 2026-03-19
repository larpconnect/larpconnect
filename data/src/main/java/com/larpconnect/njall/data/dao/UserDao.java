package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.User;
import java.util.UUID;

/** DAO for User. */
@DefaultImplementation(DefaultUserDao.class)
public interface UserDao extends Dao<User, UUID> {}
