package com.larpconnect.njall.api.studios.locations;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.studios.LocationDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultLocationActorFactory implements LocationActorFactory {

  private final LocationDAO locationDao;

  @Inject
  DefaultLocationActorFactory(LocationDAO locationDao) {
    this.locationDao = locationDao;
  }

  @Override
  public Behavior<LocationCommand> create() {
    return Behaviors.setup(context -> new LocationActor(context, locationDao));
  }
}
