Feature: Administrative Server Management API
  As a system administrator
  I want to query registered servers and contact points via the admin API
  So that I can monitor server configuration and administrative contacts

  Scenario: Successful retrieval of servers and contacts
    Given the HTTP server is running with database migrations applied and seed records present
    When an admin client sends a GET request to "/api/admin/v1/servers"
    Then the admin response status code should be 200
    And the admin response content-type should be "application/json"
    And the response body contains a JSON array of servers with fields "id", "name", "primaryDomain", "createdOn", and an array of "contacts"
    And each contact contains "id", "roleType", "contactType", "contact", and "ordering"
    And the server list contains a server named "alpha-node" with primary domain "larpconnect.test"
    And the server contacts contain an "ADMIN" "EMAIL" contact with value "ops@larpconnect.test"
