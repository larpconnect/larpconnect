@AdminApi
Feature: User Space Studios REST API
  As a studio member or client
  I want to retrieve studio details via user space endpoints
  So that I can access tenant studio information without exposing internal tenant identifiers

  Scenario: Successfully retrieve studio by alias
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant",
        "name": "Valiant Games"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant/v1/studio"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "alias" with value "valiant"
    And the JSON response contains field "name" with value "Valiant Games"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully retrieve studio by public studio ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_uuid_test",
        "name": "Valiant UUID Games"
      }
      """
    Then the HTTP response status code is 201
    And the response field "studioId" is remembered as "valiant_studio_id"
    When an API admin sends a GET request to "/api/studios/{valiant_studio_id}/v1/studio"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "alias" with value "valiant_uuid_test"
    And the JSON response contains field "name" with value "Valiant UUID Games"
    And the JSON response does not contain field "tenantId"

  Scenario: Requesting nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a GET request to "/api/studios/nonexistent_studio/v1/studio"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message

  Scenario: Requesting soft-deleted studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "abandoned_studio",
        "name": "Abandoned Games"
      }
      """
    Then the HTTP response status code is 201
    And the soft-deleted studio timestamp is set for alias "abandoned_studio"
    When an API admin sends a GET request to "/api/studios/abandoned_studio/v1/studio"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
