package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.larpconnect.njall.data.domain.admin.StudioLookup;
import java.util.Optional;
import java.util.UUID;

/** Contract extracting candidate actor recipient identifiers addressed to the local studio. */
interface LocalActorRecipientExtractor {

  /**
   * Extracts candidate local actor recipient identifiers from an activity.
   *
   * @param activity The FeatherPub activity JSON payload.
   * @param studioIdParam Studio identifier parameter from request path.
   * @param studioId Resolved studio UUID.
   * @param lookup Optional studio lookup metadata.
   * @return Immutable set of recipient actor identifiers matching the target studio.
   */
  ImmutableSet<String> extract(
      JsonNode activity, String studioIdParam, UUID studioId, Optional<StudioLookup> lookup);
}
