package com.larpconnect.njall.api.studios.inbox;

import com.larpconnect.njall.data.domain.admin.StudioLookup;
import org.apache.pekko.http.javadsl.server.Route;

/**
 * Pekko HTTP subroute contract handling FeatherPub Activity inbox delivery and AIP-233 batching.
 */
public interface InboxRoute {

  /**
   * Returns subroute for inbox endpoints under /api/studios/{studio-id}/v1/actors/{actor-id}.
   *
   * @param lookup The tenanted studio metadata.
   * @param studioIdParam Studio parameter from request path.
   * @param actorIdentifier Canonical actor UUID string or case-insensitive preferredUsername.
   * @return Pekko HTTP route for inbox and custom message endpoints.
   */
  Route inboxSubroutes(StudioLookup lookup, String studioIdParam, String actorIdentifier);
}
