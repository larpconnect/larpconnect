Feature: Administrative Health Check Endpoint
  As an administrator or infrastructure probe
  I want to make an HTTP GET request to /api/admin/v1/health
  So that I can verify the Apache Pekko runtime and subsystem health

  Scenario: Health check passes when Pekko is healthy
    Given the HTTP server is running and the Pekko framework is healthy
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    Then the health probe response status code should be 200
    And the health probe response body should be empty

  Scenario: Cached health probe result served on subsequent requests
    Given the HTTP server is running and the Pekko framework is healthy
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    And a client sends another HTTP GET request to "/api/admin/v1/health" within the cache window
    Then the health probe response status code should be 200
    And the health probe response body should be empty
    And no new database ping query is executed

  Scenario: Unauthenticated request is processed
    Given the HTTP server is running and the Pekko framework is healthy
    When a client sends an HTTP GET request to "/api/admin/v1/health" without authorization headers
    Then the health probe response status code should be 200
    And the health probe response body should be empty

  Scenario: Health check fails when Pekko is terminating or an unhealthy state is detected
    Given the Pekko framework is terminating or an unhealthy state is detected
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    Then the health probe response status code should be 500
    And the health probe response body should be empty

  Scenario: Health check fails when database is unreachable or times out
    Given the njall_admin database connection fails, is terminated, or times out during probe evaluation
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    Then the health probe response status code should be 500
    And the health probe response body should be empty

  Scenario: Health check actor ask times out
    Given the health check actor does not respond within the configured ask timeout
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    Then the health probe response status code should be 500
    And the health probe response body should be empty
