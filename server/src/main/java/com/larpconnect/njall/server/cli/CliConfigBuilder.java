package com.larpconnect.njall.server.cli;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Builds composite Typesafe {@link Config} instances by layering CLI options and external
 * configuration files atop default configurations.
 */
public final class CliConfigBuilder {

  private final Map<String, Object> overrides = new HashMap<>();
  private final Config baseConfig;
  private Optional<File> configFile = Optional.empty();

  public CliConfigBuilder() {
    this(ConfigFactory.load());
  }

  public CliConfigBuilder(Config baseConfig) {
    this.baseConfig = baseConfig;
  }

  public CliConfigBuilder withConfigFile(Optional<File> configFile) {
    this.configFile = configFile;
    return this;
  }

  public CliConfigBuilder withConfigFile(File configFile) {
    return withConfigFile(Optional.of(configFile));
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
    options.host().ifPresent(v -> withOverride("larpconnect.server.host", v));
    options.port().ifPresent(v -> withOverride("larpconnect.server.port", v));
    options.name().ifPresent(v -> withOverride("larpconnect.server.name", v));
    options.primaryDomain().ifPresent(v -> withOverride("larpconnect.server.primary-domain", v));
    options.adminContact().ifPresent(v -> withOverride("larpconnect.server.admin-contact", v));
    return this;
  }

  public CliConfigBuilder withMigrationOptions(@Nullable MigrationOptions options) {
    if (options == null) {
      return this;
    }
    return applyMigrationOptions(options);
  }

  private CliConfigBuilder applyMigrationOptions(MigrationOptions options) {
    options
        .jdbcUrl()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.jdbc-url", v));
    options
        .username()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.username", v));
    options
        .password()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.password", v));
    options
        .trustAuth()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.trust-auth", v));
    options
        .schemas()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.schemas", v));
    options
        .defaultSchema()
        .ifPresent(v -> withOverride("larpconnect.data.database.migration.default-schema", v));
    options.serverName().ifPresent(v -> withOverride("larpconnect.server.name", v));
    options.primaryDomain().ifPresent(v -> withOverride("larpconnect.server.primary-domain", v));
    options.adminContact().ifPresent(v -> withOverride("larpconnect.server.admin-contact", v));
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
    return configFile
        .map(
            file -> {
              if (!file.exists()) {
                throw new IllegalArgumentException(
                    "Specified configuration file does not exist: " + file.getPath());
              }
              return ConfigFactory.parseFile(file);
            })
        .orElseGet(ConfigFactory::empty);
  }
}
