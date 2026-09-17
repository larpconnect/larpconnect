package com.larpconnect.njall.server.cli;

import org.jspecify.annotations.Nullable;

/** Encapsulates CLI options for the server subcommand. */
public record ServerOptions(
    @Nullable String host,
    @Nullable Integer port,
    @Nullable String name,
    @Nullable String primaryDomain,
    @Nullable String adminContact) {}
