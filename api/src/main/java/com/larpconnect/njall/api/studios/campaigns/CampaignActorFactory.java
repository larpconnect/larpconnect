package com.larpconnect.njall.api.studios.campaigns;

import org.apache.pekko.actor.typed.Behavior;

/** Factory for creating {@link CampaignActor} behaviors. */
public interface CampaignActorFactory {

  /**
   * Creates the {@link Behavior} for {@link CampaignActor}.
   *
   * @return A typed behavior handling {@link CampaignCommand}.
   */
  Behavior<CampaignCommand> create();
}
