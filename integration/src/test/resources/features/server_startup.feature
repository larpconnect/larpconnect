Feature: Server Startup
  Scenario: Server starts up successfully
    Given the server is configured correctly
    When I start the server
    Then the server should be running
    And the gRPC server should be reachable
    And the OpenAPI endpoint should be reachable
