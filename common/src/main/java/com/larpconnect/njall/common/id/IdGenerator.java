package com.larpconnect.njall.common.id;

import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.DefaultImplementation;
import java.util.UUID;

/**
 * An abstraction for generating unique identifiers across the system.
 *
 * <p>Centralizing ID generation provides two main architectural benefits. First, it allows the
 * system to standardise on a specific ID format (such as UUIDv7) that may have desirable properties
 * like lexicographical sorting, without coupling consumers to a specific library or implementation.
 * Second, it abstracts away randomness and temporal dependencies, enabling test doubles to inject
 * predictable or deterministic IDs during unit and integration testing.
 */
@DefaultImplementation(IdGeneratorService.class)
public interface IdGenerator {
  /**
   * Generates a new, system-wide unique identifier.
   *
   * <p>This method abstracts the underlying complexity of ID creation (e.g., mixing timestamp,
   * machine ID, and entropy) to provide a simple, uniform interface for assigning identities to new
   * entities or events.
   *
   * @return a new uniquely generated UUID
   */
  @AiContract(ensure = "$res \\neq \\bot", implementationHint = "Generates a unique identifier.")
  UUID generate();
}
