package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.LarpSystem;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

@DefaultImplementation(DefaultLarpSystemDao.class)
public interface LarpSystemDao {
  Uni<LarpSystem> findById(UUID id);

  Uni<LarpSystem> persist(LarpSystem entity);
}
