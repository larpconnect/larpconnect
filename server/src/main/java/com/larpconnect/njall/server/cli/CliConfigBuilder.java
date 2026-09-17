package com.larpconnect.njall.server.cli;

import static java.util.Objects.requireNonNull;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Builds composite Typesafe {@link Config} instances by layering CLI options and external
 * configuration files atop default configurations.
 */
public final class CliConfigBuilder {

  private final Map<String, Object> overrides = new HashMap<>();
  private final Config baseConfig;
  private @Nullable File configFile;

  public CliConfigBuilder() {
    this(ConfigFactory.load());
  }

  public CliConfigBuilder(Config baseConfig) {
    this.baseConfig = requireNonNull(baseConfig, "baseConfig cannot be null");
  }

  public CliConfigBuilder withConfigFile(@Nullable File configFile) {
    this.configFile = configFile;
    return this;
  }

  public CliConfigBuilder withOverride(String path, @Nullable Object value) {
    if (value != null) {
      if (value instanceof List<?> list && list.isEmpty()) {
        return this;
      }
      overrides.put(path, value);
    }
    return this;
  }

  public CliConfigBuilder withServerOptions(@Nullable ServerOptions options) {
    if (options == null) {
      return this;
    }
    return applyServerOptions(options);
  }

  private CliConfigBuilder applyServerOptions(ServerOptions options) {
    withOverride("larpconnect.server.host", options.host());
    withOverride("larpconnect.server.port", options.port());
    withOverride("larpconnect.server.name", options.name());
    withOverride("larpconnect.server.primary-domain", options.primaryDomain());
    withOverride("larpconnect.server.admin-contact", options.adminContact());
    return this;
  }

  public CliConfigBuilder withMigrationOptions(@Nullable MigrationOptions options) {
    if (options == null) {
      return this;
    }
    return applyMigrationOptions(options);
  }

  private CliConfigBuilder applyMigrationOptions(MigrationOptions options) {
    withOverride("larpconnect.data.database.migration.jdbc-url", options.jdbcUrl());
    withOverride("larpconnect.data.database.migration.username", options.username());
    withOverride("larpconnect.data.database.migration.password", options.password());
    withOverride("larpconnect.data.database.migration.schemas", options.schemas());
    withOverride("larpconnect.data.database.migration.default-schema", options.defaultSchema());
    withOverride("larpconnect.server.name", options.serverName());
    withOverride("larpconnect.server.primary-domain", options.primaryDomain());
    withOverride("larpconnect.server.admin-contact", options.adminContact());
    return this;
  }

  public Config build() {
    var cliConfig = parseCliConfig();
    var fileConfig = loadFileConfig();
    return mergeConfigs(cliConfig, fileConfig);
  }

  private Config parseCliConfig() {
    return ConfigFactory.parseMap(overrides);
  }

  private Config mergeConfigs(Config cliConfig, Config fileConfig) {
    return cliConfig.withFallback(fileConfig).withFallback(baseConfig);
  }

  private Config loadFileConfig() {
    if (configFile == null) {
      return ConfigFactory.empty();
    }
    if (!configFile.exists()) {
      throw new IllegalArgumentException(
          "Specified configuration file does not exist: " + configFile.getPath());
    }
    return ConfigFactory.parseFile(configFile);
  }
}
