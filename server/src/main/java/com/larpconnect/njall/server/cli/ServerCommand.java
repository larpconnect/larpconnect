package com.larpconnect.njall.server.cli;

import com.typesafe.config.Config;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/** Subcommand for starting the Project Njall HTTP server runtime. */
@Command(
    name = "server",
    description = "Starts the HTTP server runtime and binds network listeners.",
    mixinStandardHelpOptions = true)
public final class ServerCommand implements Callable<Integer> {

  private final Logger logger = LoggerFactory.getLogger(ServerCommand.class);

  @ParentCommand private @Nullable RootCommand rootCommand;

  @Option(
      names = {"--host", "-b"},
      description = "Network host or IP interface address to bind to.")
  private @Nullable String host;

  @Option(
      names = {"-p", "--port"},
      description = "HTTP port number to bind to (0-65535, 0 for dynamic ephemeral port).")
  private @Nullable Integer port;

  @Option(
      names = {"--name"},
      description = "Server node identification name.")
  private @Nullable String name;

  @Option(
      names = {"--primary-domain"},
      description = "Server primary domain.")
  private @Nullable String primaryDomain;

  @Option(
      names = {"--admin-contact"},
      description = "Administrative contact email address.")
  private @Nullable String adminContact;

  private final Consumer<Config> serverLauncher;

  public ServerCommand() {
    this(config -> {});
  }

  public ServerCommand(Consumer<Config> serverLauncher) {
    this.serverLauncher = serverLauncher;
  }

  public @Nullable String host() {
    return host;
  }

  public @Nullable Integer port() {
    return port;
  }

  public @Nullable String name() {
    return name;
  }

  public @Nullable String primaryDomain() {
    return primaryDomain;
  }

  public @Nullable String adminContact() {
    return adminContact;
  }

  @Nullable Integer callWithRoot(RootCommand parent) {
    this.rootCommand = parent;
    return call();
  }

  @Override
  public @Nullable Integer call() {
    logServerStart();
    var configFile = resolveConfigFile();
    var config = buildConfig(configFile);
    launchServer(config);
    return null;
  }

  private void logServerStart() {
    logger.info("Preparing server configuration from CLI options...");
  }

  private void launchServer(Config config) {
    serverLauncher.accept(config);
  }

  private @Nullable File resolveConfigFile() {
    return rootCommand != null ? rootCommand.configFile() : null;
  }

  private Config buildConfig(@Nullable File configFile) {
    var builder = createConfigBuilder();
    var options = createServerOptions();
    return builder.withConfigFile(configFile).withServerOptions(options).build();
  }

  private CliConfigBuilder createConfigBuilder() {
    return new CliConfigBuilder();
  }

  private ServerOptions createServerOptions() {
    return new ServerOptions(host, port, name, primaryDomain, adminContact);
  }
}
