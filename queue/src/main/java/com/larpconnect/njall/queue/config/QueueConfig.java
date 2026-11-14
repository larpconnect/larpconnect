package com.larpconnect.njall.queue.config;

import com.google.errorprone.annotations.Immutable;
import com.typesafe.config.Config;

/**
 * Configuration for the AMQP 1.0 queue messaging infrastructure.
 *
 * @param enabled Whether AMQP queue integration is enabled.
 * @param host AMQP 1.0 broker hostname or IP address.
 * @param port AMQP 1.0 broker port.
 * @param username Authentication username.
 * @param password Authentication password.
 * @param inboxQueueName Destination queue name for inbox activity messages.
 */
@Immutable
public record QueueConfig(
    boolean enabled,
    String host,
    int port,
    String username,
    String password,
    String inboxQueueName) {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 5672;
  public static final String DEFAULT_USERNAME = "njall_amqp";
  public static final String DEFAULT_INBOX_QUEUE_NAME = "njall.inbox.activities";

  public QueueConfig {
    if (enabled && password.isBlank()) {
      throw createMissingPasswordException();
    }
  }

  /**
   * Convenience constructor parsing {@link QueueConfig} directly from a Typesafe {@link Config}.
   *
   * @param config Typesafe config instance, either root or scoped to {@code njall.queue}.
   */
  public QueueConfig(Config config) {
    var scoped = resolveScopedConfig(config);
    this(
        scoped.hasPath("enabled") && scoped.getBoolean("enabled"),
        scoped.hasPath("host") ? scoped.getString("host") : DEFAULT_HOST,
        scoped.hasPath("port") ? scoped.getInt("port") : DEFAULT_PORT,
        scoped.hasPath("username") ? scoped.getString("username") : DEFAULT_USERNAME,
        scoped.hasPath("password") ? scoped.getString("password") : "",
        scoped.hasPath("inbox-queue-name")
            ? scoped.getString("inbox-queue-name")
            : DEFAULT_INBOX_QUEUE_NAME);
  }

  private static Config resolveScopedConfig(Config config) {
    return config.hasPath("njall.queue") ? config.getConfig("njall.queue") : config;
  }

  private static IllegalStateException createMissingPasswordException() {
    return new IllegalStateException("Missing required AMQP queue password when queue is enabled");
  }
}
