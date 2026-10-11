package com.larpconnect.njall.api.studios.collections;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.collections.CollectionDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultCollectionActorFactory implements CollectionActorFactory {

  private final CollectionDAO collectionDao;

  @Inject
  DefaultCollectionActorFactory(CollectionDAO collectionDao) {
    this.collectionDao = collectionDao;
  }

  @Override
  public Behavior<CollectionCommand> create() {
    return Behaviors.setup(context -> new CollectionActor(context, collectionDao));
  }
}
