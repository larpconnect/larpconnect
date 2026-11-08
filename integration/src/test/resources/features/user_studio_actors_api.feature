@AdminApi
Feature: User Space Studio Actors REST API
  As a studio member or federated client
  I want to manage actors, federated endpoints, and collections for a studio via user space endpoints
  So that I can establish domain identities and communicate within the tenant boundary under Row-Level Security

  Scenario: Successfully create actor with all fields and generator forms
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_all",
        "name": "Valiant Actors All Fields"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_all/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead",
        "summary": "Lead singer of Valkyrie"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "type" with value "Actor"
    And the JSON response contains field "preferredUsername" with value "valkyrie_lead"
    And the JSON response contains field "summary" with value "Lead singer of Valkyrie"
    And the JSON response contains non-null field "inbox"
    And the JSON response contains non-null field "outbox"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the JSON response does not contain field "ownerId"
    And the response field "id" is remembered as "created_actor_id"
    And the JSON response contains field "generator" with value "/api/studios/valiant_act_all/v1/actors/{created_actor_id}"
    When an API admin sends a POST request to "/api/studios/valiant_act_all/v1/actors" with body:
      """
      {
        "generator": "{created_actor_id}",
        "preferredUsername": "valkyrie_synth"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "generator" with value "/api/studios/valiant_act_all/v1/actors/{created_actor_id}"
    When an API admin sends a POST request to "/api/studios/valiant_act_all/v1/actors" with body:
      """
      {
        "generator": "/api/studios/valiant_act_all/v1/actors/{created_actor_id}",
        "preferredUsername": "valkyrie_bass"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "generator" with value "/api/studios/valiant_act_all/v1/actors/{created_actor_id}"

  Scenario: Successfully create actor with minimal fields defaulting to self-ownership
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_min",
        "name": "Valiant Actors Minimal"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_min/v1/actors" with body:
      """
      {
        "preferredUsername": "rochester_lord"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "min_actor_id"
    And the JSON response contains field "preferredUsername" with value "rochester_lord"
    And the JSON response contains field "generator" with value "/api/studios/valiant_act_min/v1/actors/{min_actor_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating actor with duplicate preferred username in same tenant returns 409
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_dup",
        "name": "Valiant Actors Duplicate"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_dup/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_dup/v1/actors" with body:
      """
      {
        "preferredUsername": "VALKYRIE_LEAD"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Creating actor with missing generator entity is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_no_owner",
        "name": "Valiant Actors Missing Owner"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_no_owner/v1/actors" with body:
      """
      {
        "generator": "018f2345-6789-7abc-def0-000000000000",
        "preferredUsername": "orphan_actor"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating actor with non-actor generator entity is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_non_actor_gen",
        "name": "Valiant Non-Actor Generator Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_non_actor_gen/v1/actors" with body:
      """
      {
        "preferredUsername": "valid_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "valid_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_act_non_actor_gen/v1/individuals" with body:
      """
      {
        "generator": "{valid_actor_id}",
        "name": "Jane Eyre"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ind_gen_id"
    When an API admin sends a POST request to "/api/studios/valiant_act_non_actor_gen/v1/actors" with body:
      """
      {
        "generator": "{ind_gen_id}",
        "preferredUsername": "rejected_actor"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating actor with soft-deleted generator entity is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_deleted_gen",
        "name": "Valiant Deleted Generator Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_deleted_gen/v1/actors" with body:
      """
      {
        "preferredUsername": "temp_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "temp_actor_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_act_deleted_gen/v1/actors/{temp_actor_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_act_deleted_gen/v1/actors" with body:
      """
      {
        "generator": "{temp_actor_id}",
        "preferredUsername": "new_actor_deleted_gen"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating actor with blank preferred username is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_blank",
        "name": "Valiant Actors Blank Username"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_blank/v1/actors" with body:
      """
      {
        "preferredUsername": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Listing actors is not supported and returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_list",
        "name": "Valiant Actors List"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_act_list/v1/actors"
    Then the HTTP response status code is 404

  Scenario: Successfully retrieve active actor by ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_get",
        "name": "Valiant Actors Get"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_get/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead",
        "summary": "Lead singer"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "retrieved_actor_id"
    When an API admin sends a GET request to "/api/studios/valiant_act_get/v1/actors/{retrieved_actor_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "id" with value "{retrieved_actor_id}"
    And the JSON response contains field "preferredUsername" with value "valkyrie_lead"
    And the JSON response contains field "summary" with value "Lead singer"
    And the JSON response contains field "generator" with value "/api/studios/valiant_act_get/v1/actors/{retrieved_actor_id}"
    And the JSON response contains non-null field "inbox"
    And the JSON response contains non-null field "outbox"

  Scenario: Successfully update actor summary and username via update mask
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_patch",
        "name": "Valiant Actors Patch"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead",
        "summary": "Original summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_actor_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_act_patch/v1/actors/{patch_actor_id}?update_mask=summary,preferredUsername" with body:
      """
      {
        "summary": "Updated bio text",
        "preferredUsername": "valkyrie_commander"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "summary" with value "Updated bio text"
    And the JSON response contains field "preferredUsername" with value "valkyrie_commander"

  Scenario: Reject update with conflicting preferred username in same tenant
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_patch_conflict",
        "name": "Valiant Actors Patch Conflict"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_patch_conflict/v1/actors" with body:
      """
      {
        "preferredUsername": "actor_one"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_patch_conflict/v1/actors" with body:
      """
      {
        "preferredUsername": "actor_two"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "actor_two_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_act_patch_conflict/v1/actors/{actor_two_id}?update_mask=preferredUsername" with body:
      """
      {
        "preferredUsername": "ACTOR_ONE"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Successfully add, overwrite, and remove federated endpoints
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_ep",
        "name": "Valiant Actors Endpoints"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_ep/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ep_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}:addEndpoint" with body:
      """
      {
        "name": "sharedInbox",
        "endpoint": "https://old.example.com/inbox"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}"
    Then the HTTP response status code is 200
    And the JSON response path "endpoints.sharedInbox" has value "https://old.example.com/inbox"
    When an API admin sends a POST request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}:addEndpoint" with body:
      """
      {
        "name": "sharedInbox",
        "endpoint": "https://new.example.com/inbox"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}"
    Then the HTTP response status code is 200
    And the JSON response path "endpoints.sharedInbox" has value "https://new.example.com/inbox"
    When an API admin sends a POST request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}:removeEndpoint" with body:
      """
      {
        "name": "sharedInbox"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_act_ep/v1/actors/{ep_actor_id}"
    Then the HTTP response status code is 200
    And the JSON response does not contain field "endpoints.sharedInbox"

  Scenario: Reject endpoint with invalid name format or excessive length
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_bad_ep",
        "name": "Valiant Actors Bad Endpoint"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_bad_ep/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bep_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_act_bad_ep/v1/actors/{bep_actor_id}:addEndpoint" with body:
      """
      {
        "name": "invalid-hyphen-name",
        "endpoint": "https://fed.example.com/inbox"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Successfully retrieve and post to actor inbox and outbox
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_box",
        "name": "Valiant Actors Box"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_box/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "box_actor_id"
    When an API admin sends a GET request to "/api/studios/valiant_act_box/v1/actors/{box_actor_id}/inbox"
    Then the HTTP response status code is 200
    And the JSON response contains field "type" with value "OrderedCollection"
    And the JSON response contains field "name" with value "inbox"
    And the JSON response array "orderedItems" has length 0
    When an API admin sends a GET request to "/api/studios/valiant_act_box/v1/actors/{box_actor_id}/outbox"
    Then the HTTP response status code is 200
    And the JSON response contains field "type" with value "OrderedCollection"
    And the JSON response contains field "name" with value "outbox"
    And the JSON response array "orderedItems" has length 0
    When an API admin sends a POST request to "/api/studios/valiant_act_box/v1/actors/{box_actor_id}/inbox" with body:
      """
      {
        "type": "Create",
        "summary": "Notification of incoming interaction"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_box/v1/actors/{box_actor_id}/outbox" with body:
      """
      {
        "type": "Note",
        "content": "Published message"
      }
      """
    Then the HTTP response status code is 201

  Scenario: Successfully soft-delete actor and cascade to paired collections
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_act_del",
        "name": "Valiant Actors Delete"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_act_del/v1/actors" with body:
      """
      {
        "preferredUsername": "valkyrie_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "delete_actor_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_act_del/v1/actors/{delete_actor_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_act_del/v1/actors/{delete_actor_id}"
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/valiant_act_del/v1/actors/{delete_actor_id}/inbox"
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/valiant_act_del/v1/actors/{delete_actor_id}/outbox"
    Then the HTTP response status code is 404
    And the entity for remembered actor "delete_actor_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_act_del/v1/actors/{delete_actor_id}"
    Then the HTTP response status code is 404
