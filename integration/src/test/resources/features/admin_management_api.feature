@AdminApi
Feature: Administrative Management REST API
  As a system administrator
  I want to manage studios, roles, and administrative users via REST endpoints
  So that administrative workflows, access control, and tenant lookups are fully operational

  Scenario: Studio lifecycle management
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valkyrie_studios"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "alias" with value "valkyrie_studios"
    And the response field "studioId" is remembered as "valkyrie_id"
    When an API admin sends a GET request to "/api/admin/v1/studios/valkyrie_studios"
    Then the HTTP response status code is 200
    And the JSON response contains field "alias" with value "valkyrie_studios"
    When an API admin sends a GET request to "/api/admin/v1/studios/{valkyrie_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "alias" with value "valkyrie_studios"
    When an API admin sends a GET request to "/api/admin/v1/studios"
    Then the HTTP response status code is 200
    And the JSON response array contains an item with "alias" equal to "valkyrie_studios"
    When an API admin sends a GET request to "/api/admin/v1/studios?include_deleted=true"
    Then the HTTP response status code is 200
    And the JSON response array contains an item with "alias" equal to "valkyrie_studios"

  Scenario: Role lifecycle management
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "system_auditor"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "roleName" with value "system_auditor"
    And the response field "id" is remembered as "auditor_role_id"
    When an API admin sends a GET request to "/api/admin/v1/roles/system_auditor"
    Then the HTTP response status code is 200
    And the JSON response contains field "roleName" with value "system_auditor"
    When an API admin sends a GET request to "/api/admin/v1/roles/{auditor_role_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "roleName" with value "system_auditor"
    When an API admin sends a GET request to "/api/admin/v1/roles"
    Then the HTTP response status code is 200
    And the JSON response array contains an item with "roleName" equal to "system_auditor"

  Scenario: User lifecycle and role assignment custom methods
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "moderator"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "event_lead"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/users" with body:
      """
      {
        "username": "bjorn_ironside",
        "status": "ACTIVE",
        "roles": ["moderator"]
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "username" with value "bjorn_ironside"
    And the JSON response array "roles" contains "moderator"
    And the response field "id" is remembered as "bjorn_user_id"
    When an API admin sends a GET request to "/api/admin/v1/users/bjorn_ironside"
    Then the HTTP response status code is 200
    And the JSON response contains field "username" with value "bjorn_ironside"
    When an API admin sends a GET request to "/api/admin/v1/users/{bjorn_user_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "username" with value "bjorn_ironside"
    When an API admin sends a GET request to "/api/admin/v1/users"
    Then the HTTP response status code is 200
    And the JSON response array contains an item with "username" equal to "bjorn_ironside"
    When an API admin sends a POST request to "/api/admin/v1/users/{bjorn_user_id}:addRole" with body:
      """
      {
        "roleName": "moderator"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response array "roles" contains "moderator"
    When an API admin sends a POST request to "/api/admin/v1/users/{bjorn_user_id}:addRole" with body:
      """
      {
        "roleName": "event_lead"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response array "roles" contains "moderator"
    And the JSON response array "roles" contains "event_lead"
    When an API admin sends a POST request to "/api/admin/v1/users/{bjorn_user_id}:removeRole" with body:
      """
      {
        "roleName": "moderator"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response array "roles" does not contain "moderator"
    And the JSON response array "roles" contains "event_lead"
    When an API admin sends a POST request to "/api/admin/v1/users/{bjorn_user_id}:removeRole" with body:
      """
      {
        "roleName": "moderator"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response array "roles" does not contain "moderator"
    And the JSON response array "roles" contains "event_lead"

  Scenario: AIP-193 structured error responses
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "123-Invalid!"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "123-BadRole!"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/users" with body:
      """
      {
        "username": "ragnar_lothbrok",
        "status": "ACTIVE"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/users/ragnar_lothbrok:addRole" with body:
      """
      {
        "roleName": "non_existent_role"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a GET request to "/api/admin/v1/studios/non_existent_studio"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a GET request to "/api/admin/v1/users/non_existent_user"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a GET request to "/api/admin/v1/roles/non_existent_role"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/users/00000000-0000-0000-0000-000000000000:addRole" with body:
      """
      {
        "roleName": "moderator"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "duplicate_studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "duplicate_studio"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "duplicate_role"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/roles" with body:
      """
      {
        "roleName": "duplicate_role"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message
    When an API admin sends a POST request to "/api/admin/v1/users" with body:
      """
      {
        "username": "duplicate_user",
        "status": "ACTIVE"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/users" with body:
      """
      {
        "username": "duplicate_user",
        "status": "ACTIVE"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message
