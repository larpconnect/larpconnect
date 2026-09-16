package com.larpconnect.njall.server.cli;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
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

  @ParentCommand private RootCommand rootCommand;

  @Option(
      names = {"--host", "-b"},
      description = "Network host or IP interface address to bind to.")
  private String host;

  @Option(
      names = {"-p", "--port"},
      description = "HTTP port number to bind to (0-65535, 0 for dynamic ephemeral port).")
  private Integer port;

  @Option(
      names = {"--name"},
      description = "Server node identification name.")
  private String name;

  @Option(
      names = {"--primary-domain"},
      description = "Server primary domain.")
  private String primaryDomain;

  @Option(
      names = {"--admin-contact"},
      description = "Administrative contact email address.")
  private String adminContact;

  private final Consumer<Config> serverLauncher;

  public ServerCommand() {
    this(config -> {});
  }

  public ServerCommand(Consumer<Config> serverLauncher) {
    this.serverLauncher = requireNonNull(serverLauncher, "serverLauncher cannot be null");
  }

  public String host() {
    return host;
  }

  public Integer port() {
    return port;
  }

  public String name() {
    return name;
  }

  public String primaryDomain() {
    return primaryDomain;
  }

  public String adminContact() {
    return adminContact;
  }

  Integer callWithRoot(RootCommand parent) {
    this.rootCommand = parent;
    return call();
  }

  @Override
  public Integer call() {
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

  private File resolveConfigFile() {
    return rootCommand != null ? rootCommand.configFile() : null;
  }

  private Config buildConfig(File configFile) {
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
