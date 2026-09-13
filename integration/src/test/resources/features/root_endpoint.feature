Feature: HTTP Server Runtime Root Endpoint
  As a client of LarpConnect
  I want to make a GET request to the server root /
  So that I can verify the service is running and healthy

  Scenario: Requesting the root URL returns 200 OK with empty body
    Given the HTTP server is running on an ephemeral port
    When the client sends a GET request to "/"
    Then the response status code should be 200
    And the response body should be empty
