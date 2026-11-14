package com.larpconnect.njall.data.dao.studios.systems;

import com.google.errorprone.annotations.Immutable;
import java.util.Optional;

/**
 * Immutable parameter bundle for applying partial updates to a tenanted game system.
 *
 * @param name Optional updated system name.
 * @param summary Optional updated description summary.
 * @param clearSummary Whether to clear {@code summary} to null.
 */
@Immutable
public record PatchGameSystemParams(
    Optional<String> name, Optional<String> summary, boolean clearSummary) {

  /**
   * Constructs partial update parameters without explicit null clearing flags.
   *
   * @param name Optional updated system name.
   * @param summary Optional updated description summary.
   */
  public PatchGameSystemParams(Optional<String> name, Optional<String> summary) {
    this(name, summary, false);
  }
}
