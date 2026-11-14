package com.larpconnect.njall.api.studios.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Validator contract enforcing FeatherPub wire contract, 10 verbs, temporal bounds, and URI
 * authority matching.
 */
interface FeatherPubActivityValidator {

  /**
   * Validates a candidate FeatherPub activity JSON payload.
   *
   * @param activity The JSON node representing the activity.
   * @return Optional containing validation error message if invalid, or empty if valid.
   */
  Optional<String> validate(JsonNode activity);
}
