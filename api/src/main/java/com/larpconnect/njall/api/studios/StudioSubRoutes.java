package com.larpconnect.njall.api.studios;

import com.google.inject.Inject;
import com.larpconnect.njall.api.studios.events.EventsRoute;
import com.larpconnect.njall.api.studios.individuals.IndividualsRoute;
import com.larpconnect.njall.api.studios.links.LinksRoute;
import com.larpconnect.njall.api.studios.locations.LocationsRoute;
import com.larpconnect.njall.api.studios.tags.TagsRoute;
import org.apache.pekko.http.javadsl.server.Directives;
import org.apache.pekko.http.javadsl.server.Route;

/** Subordinate route aggregator nested under user-space studio endpoints. */
final class StudioSubRoutes {

  private final LinksRoute linksRoute;
  private final LocationsRoute locationsRoute;
  private final TagsRoute tagsRoute;
  private final EventsRoute eventsRoute;
  private final IndividualsRoute individualsRoute;

  @Inject
  StudioSubRoutes(
      LinksRoute linksRoute,
      LocationsRoute locationsRoute,
      TagsRoute tagsRoute,
      EventsRoute eventsRoute,
      IndividualsRoute individualsRoute) {
    this.linksRoute = linksRoute;
    this.locationsRoute = locationsRoute;
    this.tagsRoute = tagsRoute;
    this.eventsRoute = eventsRoute;
    this.individualsRoute = individualsRoute;
  }

  /**
   * Concatenates all subordinate studio routes.
   *
   * @return Composite Pekko HTTP route.
   */
  Route route() {
    return Directives.concat(
        locationsRoute.route(),
        linksRoute.route(),
        tagsRoute.route(),
        eventsRoute.route(),
        individualsRoute.route());
  }
}
