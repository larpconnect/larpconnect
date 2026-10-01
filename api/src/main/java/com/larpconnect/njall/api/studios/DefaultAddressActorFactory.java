package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.data.dao.AddressDAO;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

final class DefaultAddressActorFactory implements AddressActorFactory {

  private final AddressDAO addressDao;

  @Inject
  DefaultAddressActorFactory(AddressDAO addressDao) {
    this.addressDao = addressDao;
  }

  @Override
  public Behavior<AddressCommand> create() {
    return Behaviors.setup(context -> new AddressActor(context, addressDao));
  }
}
