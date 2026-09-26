package com.larpconnect.njall.data.domain;

import com.google.errorprone.annotations.Immutable;
import java.util.UUID;

/**
 * Immutable domain representation of a tenanted studio.
 *
 * @param id The tenant studio UUID identifier.
 * @param name The human-readable studio name.
 */
@Immutable
public record Studio(UUID id, String name) implements DatabaseObject {}
