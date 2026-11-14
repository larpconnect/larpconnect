package com.larpconnect.njall.queue.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class QueueConfigTest {

  @Test
  @DisplayName("Config constructor parses full root config block")
  void constructor_fullRootConfig_parsesAllFields() {
    var config =
        ConfigFactory.parseString(
            """
            njall.queue {
              enabled = true
              host = "amqp.broker.internal"
              port = 5673
              username = "custom_user"
              password = "secret_password"
              inbox-queue-name = "custom.inbox.activities"
            }
            """);

    var queueConfig = new QueueConfig(config);

    assertThat(queueConfig.enabled()).isTrue();
    assertThat(queueConfig.host()).isEqualTo("amqp.broker.internal");
    assertThat(queueConfig.port()).isEqualTo(5673);
    assertThat(queueConfig.username()).isEqualTo("custom_user");
    assertThat(queueConfig.password()).isEqualTo("secret_password");
    assertThat(queueConfig.inboxQueueName()).isEqualTo("custom.inbox.activities");
  }

  @Test
  @DisplayName("Config constructor parses scoped config directly")
  void constructor_scopedConfig_parsesAllFields() {
    var config =
        ConfigFactory.parseString(
            """
            enabled = true
            host = "broker.test"
            port = 5674
            username = "test_user"
            password = "test_password"
            inbox-queue-name = "test.queue"
            """);

    var queueConfig = new QueueConfig(config);

    assertThat(queueConfig.enabled()).isTrue();
    assertThat(queueConfig.host()).isEqualTo("broker.test");
    assertThat(queueConfig.port()).isEqualTo(5674);
    assertThat(queueConfig.username()).isEqualTo("test_user");
    assertThat(queueConfig.password()).isEqualTo("test_password");
    assertThat(queueConfig.inboxQueueName()).isEqualTo("test.queue");
  }

  @Test
  @DisplayName("Config constructor applies default values when config is empty")
  void constructor_emptyConfig_appliesDefaults() {
    var config = ConfigFactory.empty();

    var queueConfig = new QueueConfig(config);

    assertThat(queueConfig.enabled()).isFalse();
    assertThat(queueConfig.host()).isEqualTo(QueueConfig.DEFAULT_HOST);
    assertThat(queueConfig.port()).isEqualTo(QueueConfig.DEFAULT_PORT);
    assertThat(queueConfig.username()).isEqualTo(QueueConfig.DEFAULT_USERNAME);
    assertThat(queueConfig.password()).isEmpty();
    assertThat(queueConfig.inboxQueueName()).isEqualTo(QueueConfig.DEFAULT_INBOX_QUEUE_NAME);
  }

  @Test
  @DisplayName(
      "Config constructor throws IllegalStateException when enabled is true and password is empty")
  void constructor_enabledWithEmptyPassword_throwsIllegalStateException() {
    var config =
        ConfigFactory.parseString(
            """
            njall.queue {
              enabled = true
              password = ""
            }
            """);

    assertThatThrownBy(() -> new QueueConfig(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required AMQP queue password when queue is enabled");
  }

  @Test
  @DisplayName(
      "Config constructor throws IllegalStateException when enabled is true and password is blank")
  void constructor_enabledWithBlankPassword_throwsIllegalStateException() {
    var config =
        ConfigFactory.parseString(
            """
            njall.queue {
              enabled = true
              password = "    "
            }
            """);

    assertThatThrownBy(() -> new QueueConfig(config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing required AMQP queue password when queue is enabled");
  }

  @Test
  @DisplayName("constructor succeeds when enabled is false and password is blank")
  void constructor_disabledWithBlankPassword_succeeds() {
    var queueConfig =
        new QueueConfig(false, "localhost", 5672, "njall_amqp", "", "njall.inbox.activities");

    assertThat(queueConfig.enabled()).isFalse();
    assertThat(queueConfig.password()).isEmpty();
  }

  @Test
  @DisplayName("convenience constructor creates valid QueueConfig from Config")
  void convenienceConstructor_createsInstance() {
    var config =
        ConfigFactory.parseString(
            """
            njall.queue {
              enabled = true
              password = "valid_password"
            }
            """);

    var queueConfig = new QueueConfig(config);

    assertThat(queueConfig.enabled()).isTrue();
    assertThat(queueConfig.password()).isEqualTo("valid_password");
  }

  @Test
  @DisplayName("QueueConfigModule provides QueueConfig cleanly from Guice")
  void queueConfigModule_providesQueueConfig() {
    var config =
        ConfigFactory.parseString(
            """
            njall.queue {
              enabled = false
              host = "localhost"
            }
            """);

    var module = new QueueConfigModule();
    var queueConfig = module.provideQueueConfig(config);

    assertThat(queueConfig.host()).isEqualTo("localhost");
    assertThat(queueConfig.enabled()).isFalse();
  }
}
