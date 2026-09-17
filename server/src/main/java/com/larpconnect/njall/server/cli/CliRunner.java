package com.larpconnect.njall.server.cli;

import com.typesafe.config.Config;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/** Facade for configuring and executing Picocli CLI commands. */
public final class CliRunner {

  private final RootCommand rootCommand;
  private final IFactory factory;

  public CliRunner() {
    this(new RootCommand(), CommandLine.defaultFactory());
  }

  public CliRunner(Consumer<Config> serverLauncher, Function<Config, Integer> migrationExecutor) {
    this(createServerCommand(serverLauncher), createMigrateCommand(migrationExecutor));
  }

  public CliRunner(ServerCommand serverCommand, MigrateCommand migrateCommand) {
    this(new RootCommand(serverCommand), createCommandFactory(serverCommand, migrateCommand));
  }

  public CliRunner(RootCommand rootCommand, IFactory factory) {
    this.rootCommand = rootCommand;
    this.factory = factory;
  }

  private static ServerCommand createServerCommand(Consumer<Config> serverLauncher) {
    return new ServerCommand(serverLauncher);
  }

  private static MigrateCommand createMigrateCommand(Function<Config, Integer> migrationExecutor) {
    return new MigrateCommand(migrationExecutor);
  }

  private static IFactory createCommandFactory(
      ServerCommand serverCommand, MigrateCommand migrateCommand) {
    return new IFactory() {
      @Override
      // Safe unchecked cast mapping known command classes to explicit command instances
      @SuppressWarnings("unchecked")
      public <K> K create(Class<K> cls) throws Exception {
        if (cls == ServerCommand.class) {
          return (K) serverCommand;
        }
        if (cls == MigrateCommand.class) {
          return (K) migrateCommand;
        }
        return CommandLine.defaultFactory().create(cls);
      }
    };
  }

  /**
   * Executes the CLI with provided arguments.
   *
   * @param args Command-line arguments array.
   * @return Null if server runtime should keep executing; integer status code if process should
   *     terminate.
   */
  public @Nullable Integer execute(@Nullable String[] args) {
    var cmd = buildCommandLine();
    var exitCode = executeCommandLine(cmd, args);
    return determineExitStatus(cmd, exitCode);
  }

  private int executeCommandLine(CommandLine cmd, @Nullable String[] args) {
    return cmd.execute(args != null ? args : new String[0]);
  }

  CommandLine buildCommandLine() {
    return new CommandLine(rootCommand, factory);
  }

  private @Nullable Integer determineExitStatus(CommandLine cmd, int exitCode) {
    if (isHelpRequested(cmd)) {
      return exitCode;
    }
    if (isErrorExit(exitCode)) {
      return exitCode;
    }
    return resolveExecutionResult(cmd);
  }

  private boolean isErrorExit(int exitCode) {
    return exitCode != 0;
  }

  private @Nullable Integer resolveExecutionResult(CommandLine cmd) {
    var terminal = findTerminalCommandLine(cmd);
    return extractStatusCode(terminal);
  }

  private CommandLine findTerminalCommandLine(CommandLine cmd) {
    var current = cmd;
    var parseResult = cmd.getParseResult();
    while (parseResult != null && parseResult.hasSubcommand()) {
      parseResult = parseResult.subcommand();
      current = parseResult.commandSpec().commandLine();
    }
    return current;
  }

  private @Nullable Integer extractStatusCode(CommandLine terminal) {
    var executionResult = terminal.getExecutionResult();
    if (executionResult instanceof Integer code) {
      return code;
    }
    return null;
  }

  private boolean isHelpRequested(CommandLine cmd) {
    if (cmd.isUsageHelpRequested() || cmd.isVersionHelpRequested()) {
      return true;
    }
    for (CommandLine sub : cmd.getSubcommands().values()) {
      if (sub.isUsageHelpRequested() || sub.isVersionHelpRequested()) {
        return true;
      }
    }
    return false;
  }
}
