package com.larpconnect.njall.api.studios.campaigns;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.larpconnect.njall.api.http.RouteProvider;
import com.larpconnect.njall.api.studios.common.StudioSubRoute;
import com.larpconnect.njall.common.annotation.Blocking;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Props;

/** Guice module configuring user-space studio campaign actors and routes. */
public final class CampaignsModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CampaignsRoute.class).in(Scopes.SINGLETON);
    bind(CampaignActorFactory.class).to(DefaultCampaignActorFactory.class);
    Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class)
        .addBinding()
        .to(CampaignsRoute.class);
  }

  @Provides
  @Singleton
  ActorRef<CampaignCommand> provideCampaignActor(
      ActorSystem<Void> system, CampaignActorFactory factory, @Blocking Props dispatcher) {
    return system.systemActorOf(factory.create(), "campaignActor", dispatcher);
  }
}
