package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.DatabaseObject;
import io.smallrye.mutiny.Uni;

public interface Dao<T extends DatabaseObject, ID> {
  Uni<T> findById(ID id);

  Uni<T> persist(T entity);
}
