Feature: Web Server

  Scenario: Web server returns message
    Given the server is configured
    When I start the server
    Then I should receive a response from "http://localhost:8080/v1/message" containing:
      """
      {
        "proto": {"protobufName": "Greeting"}
      }
      """
