package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.api.studios.actors.AgentRoute;
import com.larpconnect.njall.api.studios.collections.CollectionsRoute;
import com.larpconnect.njall.api.studios.events.EventsRoute;
import com.larpconnect.njall.api.studios.individuals.IndividualsRoute;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.Route;

/** Subordinate route aggregator nested under user-space studio endpoints. */
final class StudioSubRoutes {

  private final Route compositeRoute;

  @Inject
  StudioSubRoutes(
      LinksRoute linksRoute,
      LocationsRoute locationsRoute,
      TagsRoute tagsRoute,
      EventsRoute eventsRoute,
      IndividualsRoute individualsRoute,
      CollectionsRoute collectionsRoute,
      AgentRoute agentRoute) {
    this(
        Directives.concat(
            locationsRoute.route(),
            linksRoute.route(),
            tagsRoute.route(),
            eventsRoute.route(),
            individualsRoute.route(),
            collectionsRoute.route(),
            agentRoute.route()));
  }

  StudioSubRoutes(Route compositeRoute) {
    this.compositeRoute = compositeRoute;
  }

  /**
   * Concatenates all subordinate studio routes.
   *
   * @return Composite Pekko HTTP route.
   */
  Route route() {
    return compositeRoute;
  }
}
