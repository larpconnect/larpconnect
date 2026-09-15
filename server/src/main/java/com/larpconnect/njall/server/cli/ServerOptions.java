package com.larpconnect.njall.server.cli;

/** Encapsulates CLI options for the server subcommand. */
public record ServerOptions(
    String host, Integer port, String name, String primaryDomain, String adminContact) {}
