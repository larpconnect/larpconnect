package com.larpconnect.njall.server.cli;

import java.io.File;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

/**
 * Root CLI command for Project Njall / LarpConnect.
 *
 * <p>Dispatches to {@link ServerCommand} or {@link MigrateCommand}, defaulting to {@link
 * ServerCommand} when no subcommands are supplied.
 */
@Command(
    name = "larpconnect",
    description = "Project Njall / LarpConnect server runtime and management CLI.",
    mixinStandardHelpOptions = true,
    version = "0.1.0",
    subcommands = {ServerCommand.class, MigrateCommand.class})
public final class RootCommand implements Callable<Integer> {

  @Option(
      names = {"-c", "--config"},
      description = "Path to optional external HOCON configuration file.",
      scope = ScopeType.INHERIT)
  private @Nullable File configFile;

  @Option(
      names = {"-v", "--verbose"},
      description = "Enable verbose debug logging output.",
      scope = ScopeType.INHERIT)
  private boolean verbose;

  private final ServerCommand defaultServerCommand;

  public RootCommand() {
    this(new ServerCommand());
  }

  public RootCommand(ServerCommand defaultServerCommand) {
    this.defaultServerCommand = defaultServerCommand;
  }

  public @Nullable File configFile() {
    return configFile;
  }

  public boolean verbose() {
    return verbose;
  }

  @Override
  public @Nullable Integer call() {
    return defaultServerCommand.callWithRoot(this);
  }
}
