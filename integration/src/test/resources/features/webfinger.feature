Feature: WebFinger

  Scenario: WebFinger endpoint returns resources
    Given the server is configured
    When I start the server
    Then I should receive a response from "http://localhost:8080/.well-known/webfinger?resource=acct:user@host" containing:
      """
      {
        "resources": []
      }
      """
