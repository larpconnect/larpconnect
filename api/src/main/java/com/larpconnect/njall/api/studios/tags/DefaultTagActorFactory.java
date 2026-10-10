package com.larpconnect.njall.api.studios.tags;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.HashtagDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultTagActorFactory implements TagActorFactory {

  private final HashtagDAO hashtagDao;

  @Inject
  DefaultTagActorFactory(HashtagDAO hashtagDao) {
    this.hashtagDao = hashtagDao;
  }

  @Override
  public Behavior<TagCommand> create() {
    return Behaviors.setup(context -> new TagActor(context, hashtagDao));
  }
}
