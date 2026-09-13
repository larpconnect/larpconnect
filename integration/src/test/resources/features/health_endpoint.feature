Feature: Administrative Health Check Endpoint
  As an administrator or infrastructure probe
  I want to make an HTTP GET request to /api/admin/v1/health
  So that I can verify the Apache Pekko runtime and subsystem health

  Scenario: Health check passes when Pekko is healthy
    Given the HTTP server is running and the Pekko framework is healthy
    When a client sends an HTTP GET request to "/api/admin/v1/health"
    Then the health probe response status code should be 200
    And the health probe response body should be empty

  Scenario: Unauthenticated request is processed
    Given the HTTP server is running and the Pekko framework is healthy
    When a client sends an HTTP GET request to "/api/admin/v1/health" without authorization headers
    Then the health probe response status code should be 200
    And the health probe response body should be empty
