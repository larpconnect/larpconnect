package com.larpconnect.njall.common.id;

import com.larpconnect.njall.common.annotations.AiContract;
import java.util.UUID;

public interface IdGenerator {
  /**
   * Generates a new unique identifier.
   *
   * @return a new UUID
   */
  @AiContract(ensure = "$res \\neq \\bot", implementationHint = "Generates a unique identifier.")
  UUID generate();
}
