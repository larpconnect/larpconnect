@telemetry
Feature: OpenTelemetry Distributed Tracing and Log Correlation
  As an operator or client of LarpConnect
  I want HTTP requests to establish OpenTelemetry trace contexts and correlate with server logs
  So that I can observe and debug requests across asynchronous execution boundaries

  Scenario: Requesting the root URL returns valid W3C traceparent header
    Given the HTTP server is running with telemetry enabled
    When a telemetry client sends a GET request to "/"
    Then the telemetry response status code should be 200
    And the telemetry response should include a valid W3C traceparent header

  Scenario: Requesting the health check URL returns valid W3C traceparent header
    Given the HTTP server is running with telemetry enabled
    When a telemetry client sends a GET request to "/api/admin/v1/health"
    Then the telemetry response status code should be 200
    And the telemetry response should include a valid W3C traceparent header

  Scenario: Inbound client-supplied traceparent header is ignored and replaced
    Given the HTTP server is running with telemetry enabled
    When a telemetry client sends a GET request to "/" with traceparent header "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    Then the telemetry response status code should be 200
    And the telemetry response should include a valid W3C traceparent header
    And the telemetry response trace ID should not be "4bf92f3577b34da6a3ce929d0e0e4736"

  Scenario: Request execution logs contain matching trace and span identifiers
    Given the HTTP server is running with telemetry and log capture enabled
    When a telemetry client sends a GET request to "/api/admin/v1/health"
    Then the telemetry response status code should be 200
    And the telemetry response should include a valid W3C traceparent header
    And the captured server logs should contain the matching trace and span identifiers
