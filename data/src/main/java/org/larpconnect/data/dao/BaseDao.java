package org.larpconnect.data.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.larpconnect.data.entity.NjallEntity;

/** Common sealed interface for all DAOs using the Curiously Recurring Template Pattern. */
public sealed interface BaseDao<E extends NjallEntity, D extends BaseDao<E, D>>
    permits StudioDao,
        CampaignDao,
        LarpSystemDao,
        GameDao,
        LarpCharacterDao,
        CharacterInstanceDao,
        IndividualDao,
        UserDao,
        ActorDao,
        CollectionDao {
  /** Finds an entity by its UUID in the specified schema. */
  Optional<E> findById(String schema, UUID id);

  /** Saves (persists or merges) the entity in the specified schema. */
  void save(String schema, E entity);

  /** Deletes (or soft-deletes) the entity in the specified schema. */
  void delete(String schema, E entity);

  /** Returns all active (non-deleted) entities in the specified schema. */
  List<E> findAll(String schema);
}
