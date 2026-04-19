package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.ContractTag;
import com.larpconnect.njall.common.annotations.DefaultImplementation;
import com.larpconnect.njall.data.entity.StudioRole;
import io.smallrye.mutiny.Uni;

/** DAO for StudioRole. */
@DefaultImplementation(DefaultStudioRoleDao.class)
public interface StudioRoleDao {
  @AiContract(
      require = {"$id \\neq \\bot$"},
      tags = {ContractTag.IDEMPOTENT},
      implementationHint = "Finds an entity by its identifier.")
  Uni<StudioRole> findById(StudioRole.StudioRoleId id);

  @AiContract(
      require = {"$entity \\neq \\bot$"},
      implementationHint = "Persists the given entity to the database.")
  Uni<Void> persist(StudioRole entity);
}
