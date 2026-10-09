package com.larpconnect.njall.api.studios.links;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.LinkDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultLinkActorFactory implements LinkActorFactory {

  private final LinkDAO linkDao;

  @Inject
  DefaultLinkActorFactory(LinkDAO linkDao) {
    this.linkDao = linkDao;
  }

  @Override
  public Behavior<LinkCommand> create() {
    return Behaviors.setup(context -> new LinkActor(context, linkDao));
  }
}
