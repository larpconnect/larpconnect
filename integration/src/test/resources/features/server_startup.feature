Feature: Server Startup

  Scenario: Server starts up successfully
    Given the server is configured
    When I start the server
    Then the server should be running
    And the MainVerticle should be deployed
    And I should be able to send a Message on the event bus
