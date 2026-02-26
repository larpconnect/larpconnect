Feature: gRPC Server

  Scenario: Call the gRPC service
    Given the server is configured
    And I start the server
    When I call the GetMessage RPC with an empty request
    Then I should receive a Message with "Greeting" type and "Hello from gRPC" content
