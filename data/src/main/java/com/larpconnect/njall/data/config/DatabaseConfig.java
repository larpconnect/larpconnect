package com.larpconnect.njall.data.config;

import com.google.errorprone.annotations.Immutable;

/**
 * Root database configuration composite for Project Njall persistence.
 *
 * @param migration Migration-specific configuration profile.
 * @param admin Administrator database session configuration profile.
 * @param users Tenanted users database session configuration profile.
 */
@Immutable
public record DatabaseConfig(MigrationConfig migration, SessionConfig admin, SessionConfig users) {}
