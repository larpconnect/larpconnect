package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.User;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultUserDao.class)
public interface UserDao {
  Uni<User> findById(UUID id);

  Uni<User> persist(User user);
}
