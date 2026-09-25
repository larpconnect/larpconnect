package com.larpconnect.njall.data.config;

import com.google.inject.Inject;
import com.typesafe.config.Config;

final class DefaultSessionConfigFactory implements SessionConfigFactory {

  private final Config config;

  @Inject
  DefaultSessionConfigFactory(Config config) {
    this.config = config;
  }

  @Override
  public SessionConfig create(String path) {
    var sessionSection = config.getConfig(path);
    var jdbcUrl = sessionSection.getString("jdbc-url");
    var username = sessionSection.getString("username");
    var password = sessionSection.hasPath("password") ? sessionSection.getString("password") : null;

    var globalTrustKey = "larpconnect.data.database.trust-auth";
    var globalTrustAuth = config.hasPath(globalTrustKey) && config.getBoolean(globalTrustKey);
    var trustAuth =
        sessionSection.hasPath("trust-auth")
            ? sessionSection.getBoolean("trust-auth")
            : globalTrustAuth;

    var poolSection = sessionSection.getConfig("pool");
    var minPoolSize = poolSection.getInt("min-size");
    var maxPoolSize = poolSection.getInt("max-size");
    var timeoutSeconds = poolSection.getInt("timeout-seconds");

    return new SessionConfig(
        jdbcUrl, username, password, trustAuth, minPoolSize, maxPoolSize, timeoutSeconds);
  }
}
