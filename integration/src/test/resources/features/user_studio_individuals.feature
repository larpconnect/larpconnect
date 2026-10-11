@AdminApi
Feature: User Space Studio Individuals REST API
  As a studio member or client
  I want to manage individuals for a studio via user space endpoints
  So that I can register people within the tenant boundary under Row-Level Security

  Scenario: Successfully create individual with all fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_all",
        "name": "Valiant Individuals All Fields"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_all/v1/individuals" with body:
      """
      {
        "name": "Jane Eyre",
        "summary": "Visiting scholar and resident"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "name" with value "Jane Eyre"
    And the JSON response contains field "summary" with value "Visiting scholar and resident"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_individual_id"

  Scenario: Successfully create individual with minimal fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_min",
        "name": "Valiant Individuals Minimal"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_min/v1/individuals" with body:
      """
      {
        "name": "Edward Rochester"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "name" with value "Edward Rochester"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating individual with blank name is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_blank",
        "name": "Valiant Individuals Blank Name"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_blank/v1/individuals" with body:
      """
      {
        "name": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating individual for nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/studios/nonexistent_ind_studio/v1/individuals" with body:
      """
      {
        "name": "Ghost Individual"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully retrieve active individual by ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_get",
        "name": "Valiant Individuals Get"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_get/v1/individuals" with body:
      """
      {
        "name": "St. John Rivers",
        "summary": "Parish rector"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "st_john_id"
    When an API admin sends a GET request to "/api/studios/valiant_ind_get/v1/individuals/{st_john_id}"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "name" with value "St. John Rivers"
    And the JSON response contains field "summary" with value "Parish rector"

  Scenario: Requesting nonexistent studio or invalid individual ID returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a GET request to "/api/studios/nonexistent_studio/v1/individuals/11111111-1111-1111-1111-111111111111"
    Then the HTTP response status code is 404
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_invalid_ind_id",
        "name": "Valiant Invalid Ind ID"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_invalid_ind_id/v1/individuals/not-a-valid-uuid"
    Then the HTTP response status code is 404

  Scenario: Successfully patch individual with update_mask
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_patch",
        "name": "Valiant Individuals Patch"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_patch/v1/individuals" with body:
      """
      {
        "name": "Initial Name",
        "summary": "Initial Summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_ind_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_ind_patch/v1/individuals/{patch_ind_id}?update_mask=name" with body:
      """
      {
        "name": "Patched Name",
        "summary": "Ignored Summary"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Patched Name"
    And the JSON response contains field "summary" with value "Initial Summary"

  Scenario: Patching individual with blank name is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_bad_patch",
        "name": "Valiant Bad Patch Ind"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_bad_patch/v1/individuals" with body:
      """
      {
        "name": "Valid Individual"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_patch_ind_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_ind_bad_patch/v1/individuals/{bad_patch_ind_id}" with body:
      """
      {
        "name": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Successfully soft delete individual and confirm database deleted_on
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_del",
        "name": "Valiant Delete Ind"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_ind_del/v1/individuals" with body:
      """
      {
        "name": "To Be Deleted"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "del_ind_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_ind_del/v1/individuals/{del_ind_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_ind_del/v1/individuals/{del_ind_id}"
    Then the HTTP response status code is 404
    And the entity for remembered individual "del_ind_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_ind_del/v1/individuals/{del_ind_id}"
    Then the HTTP response status code is 404
    When an API admin sends a PATCH request to "/api/studios/valiant_ind_del/v1/individuals/{del_ind_id}" with body:
      """
      {
        "name": "Resurrect Attempt"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Collection GET is rejected with 404 Not Found
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_ind_list",
        "name": "Valiant List Ind"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_ind_list/v1/individuals"
    Then the HTTP response status code is 404

  Scenario: Cross-tenant individual access returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_ind_alpha",
        "name": "Studio Ind Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_ind_beta",
        "name": "Studio Ind Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/studio_ind_alpha/v1/individuals" with body:
      """
      {
        "name": "Alpha Individual"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_ind_id"
    When an API admin sends a GET request to "/api/studios/studio_ind_beta/v1/individuals/{alpha_ind_id}"
    Then the HTTP response status code is 404
