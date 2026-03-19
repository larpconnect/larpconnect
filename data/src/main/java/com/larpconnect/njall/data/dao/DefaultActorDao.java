package com.larpconnect.njall.data.dao;

import com.larpconnect.njall.data.entity.Actor;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;

final class DefaultActorDao extends AbstractDao<Actor, UUID> implements ActorDao {

  @Inject
  DefaultActorDao(Provider<Mutiny.SessionFactory> sessionFactoryProvider) {
    super(sessionFactoryProvider, Actor.class);
  }
}
