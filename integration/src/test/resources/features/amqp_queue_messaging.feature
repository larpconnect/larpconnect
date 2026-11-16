Feature: AMQP 1.0 Queue Messaging Service and Health Probes
  As a server application and background messaging infrastructure
  I want to publish accepted FeatherPub Activity envelopes over AMQP 1.0 to RabbitMQ
  So that activities are reliably and durably dispatched with trace correlation and disposition guarantees

  Scenario: Queue service connects over AMQP 1.0 and declares durable inbox queue
    Given a configured AMQP 1.0 broker connection
    When the AMQP queue service starts up
    Then the service transitions to RUNNING state
    And the durable queue "njall.inbox.activities" is declared on the broker
    And an active publisher link is established

  Scenario: Missing broker password fails fast during configuration ingestion
    Given queue configuration with enabled set to true and an empty password
    When the queue configuration is parsed
    Then configuration loading fails fast throwing an IllegalStateException

  Scenario: Accepted activity is published as one durable AMQP 1.0 message with traceparent
    Given an active AMQP 1.0 queue service connected to RabbitMQ
    When an inbox queue envelope is published for activity "https://remote.example/activities/msg-001" with traceparent "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    Then the broker confirms ACCEPTED disposition
    And one durable message is delivered to queue "njall.inbox.activities"
    And the delivered message has messageId "https://remote.example/activities/msg-001"
    And the delivered message has contentType "application/json"
    And the delivered message has application property "traceparent" equal to "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"

  Scenario: Broker rejection or connection failure throws QueuePublishException
    Given an AMQP 1.0 publisher whose broker connection is unavailable or link is rejected
    When an inbox queue envelope is published
    Then the publication future completes exceptionally with QueuePublishException

  Scenario: AMQP health check returns cached result within 10-second window
    Given an AMQP health check evaluated a healthy broker connection
    When the AMQP health check is evaluated again within 10 seconds
    Then the cached healthy result is returned
    And no additional broker ping query is executed

  Scenario: AMQP health check reports unhealthy when broker connection is down
    Given the AMQP broker connection is closed, lost, or timed out
    And the 10-second health check cache has expired
    When the AMQP health check is evaluated
    Then an unhealthy health check result is returned describing the broker failure
    And the unhealthy result is cached for 10 seconds

  Scenario: AMQP health check times out cleanly without deadlocking when paused broker stalls lock
    Given an AMQP health check evaluated a healthy broker connection
    And the 10-second health check cache has expired
    When the RabbitMQ container is paused while a concurrent publisher probe holds the connection lock
    Then the AMQP health check times out cleanly within 3 seconds and reports unhealthy
