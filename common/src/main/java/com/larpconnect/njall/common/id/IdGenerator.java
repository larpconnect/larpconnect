package com.larpconnect.njall.common.id;

import com.larpconnect.njall.common.annotations.AiContract;
import com.larpconnect.njall.common.annotations.DefaultImplementation;
import java.util.UUID;

@DefaultImplementation(UuidV7Generator.class)
public interface IdGenerator {
  /**
   * Generates a new unique identifier.
   *
   * @return a new UUID
   */
  @AiContract(ensure = "$res \\neq \\bot", implementationHint = "Generates a unique identifier.")
  UUID generate();
}
