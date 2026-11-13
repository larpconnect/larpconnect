@AdminApi
Feature: User Space Studio Collections REST API
  As a studio member or client
  I want to manage actor-parented collections and collection items for a studio via user space endpoints
  So that I can curate ordered entities within the tenant boundary under Row-Level Security

  Scenario: Successfully create collection with omitted ownerId defaulting to path actor
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_all",
        "name": "Valiant Collections All Fields"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_all/v1/actors" with body:
      """
      {
        "preferredUsername": "director_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_act_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_all/v1/actors/{owner_act_id}/collections" with body:
      """
      {
        "name": "Featured Cast",
        "summary": "Primary cast members for season 1"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "name" with value "Featured Cast"
    And the JSON response contains field "summary" with value "Primary cast members for season 1"
    And the JSON response contains field "ownerId" with value "{owner_act_id}"
    And the JSON response contains field "generator" with value "/api/studios/valiant_col_all/v1/actors/{owner_act_id}"
    And the JSON response contains field "type" with value "OrderedCollection"
    And the JSON response contains field "totalItems" with value "0"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the JSON response array "orderedItems" has length 0
    And the response field "id" is remembered as "created_collection_id"

  Scenario: Successfully create collection with explicit non-Actor entity ownerId
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_non_actor",
        "name": "Valiant Non-Actor Owner Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_non_actor/v1/actors" with body:
      """
      {
        "preferredUsername": "curator_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "curator_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_non_actor/v1/individuals" with body:
      """
      {
        "generator": "{curator_actor_id}",
        "name": "Character Hero"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "character_ind_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_non_actor/v1/actors/{curator_actor_id}/collections" with body:
      """
      {
        "ownerId": "{character_ind_id}",
        "name": "Character Gallery"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "name" with value "Character Gallery"
    And the JSON response contains field "ownerId" with value "{character_ind_id}"
    And the JSON response contains field "generator" with value "/api/studios/valiant_col_non_actor/v1/actors/{curator_actor_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Successfully create collection with minimal fields and explicit actor ownerId
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_min",
        "name": "Valiant Collections Minimal"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_min/v1/actors" with body:
      """
      {
        "preferredUsername": "curator_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_min_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_min/v1/actors/{owner_min_id}/collections" with body:
      """
      {
        "ownerId": "{owner_min_id}",
        "name": "Crew Members"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "name" with value "Crew Members"
    And the JSON response contains field "ownerId" with value "{owner_min_id}"
    And the JSON response contains field "generator" with value "/api/studios/valiant_col_min/v1/actors/{owner_min_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating collection with duplicate name for same owner returns 409
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_dup",
        "name": "Valiant Collections Duplicate"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/actors" with body:
      """
      {
        "preferredUsername": "parish_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_dup_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/actors/{owner_dup_id}/collections" with body:
      """
      {
        "name": "Parish Cast"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/actors/{owner_dup_id}/collections" with body:
      """
      {
        "name": "Parish Cast"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Creating collection with blank name is rejected with 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_blank",
        "name": "Valiant Collections Blank Name"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_blank/v1/actors" with body:
      """
      {
        "preferredUsername": "blank_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_blank_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_blank/v1/actors/{owner_blank_id}/collections" with body:
      """
      {
        "name": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating collection with nonexistent or soft-deleted ownerId returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_fk",
        "name": "Valiant Collections FK"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_fk/v1/actors" with body:
      """
      {
        "preferredUsername": "fk_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "fk_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_fk/v1/actors/{fk_actor_id}/collections" with body:
      """
      {
        "ownerId": "019273c5-9271-7000-8000-000000000999",
        "name": "Ghost Collection"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_fk/v1/individuals" with body:
      """
      {
        "generator": "{fk_actor_id}",
        "name": "Deleted Owner"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "deleted_owner_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_col_fk/v1/individuals/{deleted_owner_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_col_fk/v1/actors/{fk_actor_id}/collections" with body:
      """
      {
        "ownerId": "{deleted_owner_id}",
        "name": "Deleted Owner Collection"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Deliberate omission of collection listing endpoint and legacy unparented paths return 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_list",
        "name": "Valiant Collections List Omission"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_list/v1/actors" with body:
      """
      {
        "preferredUsername": "list_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "list_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_list/v1/actors/{list_actor_id}/collections" with body:
      """
      {
        "name": "Active Collection"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "list_col_id"
    When an API admin sends a GET request to "/api/studios/valiant_col_list/v1/actors/{list_actor_id}/collections"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    And the JSON response contains field "message" with value "Listing collections is not supported"
    When an API admin sends a GET request to "/api/studios/valiant_col_list/v1/collections"
    Then the HTTP response status code is 404
    When an API admin sends a POST request to "/api/studios/valiant_col_list/v1/collections" with body:
      """
      {
        "name": "Legacy Collection"
      }
      """
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/valiant_col_list/v1/collections/{list_col_id}"
    Then the HTTP response status code is 404
    When an API admin sends a PATCH request to "/api/studios/valiant_col_list/v1/collections/{list_col_id}" with body:
      """
      {
        "name": "Legacy Patch"
      }
      """
    Then the HTTP response status code is 404
    When an API admin sends a DELETE request to "/api/studios/valiant_col_list/v1/collections/{list_col_id}"
    Then the HTTP response status code is 404

  Scenario: Missing, soft-deleted, or non-Actor path actor-id returns 404 across all collection endpoints
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_invalid_actor",
        "name": "Valiant Invalid Actor Collections"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors" with body:
      """
      {
        "preferredUsername": "valid_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "valid_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors" with body:
      """
      {
        "preferredUsername": "temp_deleted_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "deleted_actor_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/individuals" with body:
      """
      {
        "generator": "{valid_actor_id}",
        "name": "Non Actor Entity"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "non_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{valid_actor_id}/collections" with body:
      """
      {
        "name": "Valid Actor Collection"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "target_col_id"
    # Missing actor-id
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections" with body:
      """
      {
        "name": "Missing Actor Collection"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a GET request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections/{target_col_id}" with body:
      """
      {
        "name": "Attempted Update"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections/{target_col_id}:addItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections/{target_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a DELETE request to "/api/studios/valiant_col_invalid_actor/v1/actors/019273c5-9271-7000-8000-000000000999/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    # Soft-deleted actor-id
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections" with body:
      """
      {
        "name": "Deleted Actor Collection"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a GET request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections/{target_col_id}" with body:
      """
      {
        "name": "Attempted Update"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections/{target_col_id}:addItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections/{target_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a DELETE request to "/api/studios/valiant_col_invalid_actor/v1/actors/{deleted_actor_id}/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    # Non-Actor active entity id
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections" with body:
      """
      {
        "name": "Non Actor Collection"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a GET request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections/{target_col_id}" with body:
      """
      {
        "name": "Attempted Update"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections/{target_col_id}:addItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections/{target_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{non_actor_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a DELETE request to "/api/studios/valiant_col_invalid_actor/v1/actors/{non_actor_id}/collections/{target_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message

  Scenario: Mismatched actor-id returns 404 and leaves collection state unchanged
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_mismatch",
        "name": "Valiant Mismatched Actor Collections"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors" with body:
      """
      {
        "preferredUsername": "actor_a"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "actor_a_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors" with body:
      """
      {
        "preferredUsername": "actor_b"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "actor_b_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/individuals" with body:
      """
      {
        "generator": "{actor_a_id}",
        "name": "Item One"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_one_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/individuals" with body:
      """
      {
        "generator": "{actor_a_id}",
        "name": "Item Two"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_two_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_a_id}/collections" with body:
      """
      {
        "name": "Actor A Staff",
        "summary": "Original summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "actor_a_col_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_a_id}/collections/{actor_a_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_one_id}"
      }
      """
    Then the HTTP response status code is 204
    # Mismatched actor_b attempts across GET, PATCH, :addItem, :removeItem, DELETE
    When an API admin sends a GET request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_b_id}/collections/{actor_a_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a PATCH request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_b_id}/collections/{actor_a_col_id}" with body:
      """
      {
        "name": "Hijacked Name",
        "summary": "Tampered summary"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_b_id}/collections/{actor_a_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_two_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_b_id}/collections/{actor_a_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{item_one_id}"
      }
      """
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    When an API admin sends a DELETE request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_b_id}/collections/{actor_a_col_id}"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message
    # Verify original collection state under actor_a is completely unchanged
    When an API admin sends a GET request to "/api/studios/valiant_col_mismatch/v1/actors/{actor_a_id}/collections/{actor_a_col_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Actor A Staff"
    And the JSON response contains field "summary" with value "Original summary"
    And the JSON response array "orderedItems" has length 1
    And the JSON response path "orderedItems.0" has value "{item_one_id}"

  Scenario: Successfully retrieve, patch, and soft-delete collection
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_crud",
        "name": "Valiant Collections CRUD"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_crud/v1/actors" with body:
      """
      {
        "preferredUsername": "house_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_crud_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}/collections" with body:
      """
      {
        "name": "House Staff",
        "summary": "Original summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "crud_collection_id"
    When an API admin sends a GET request to "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}/collections/{crud_collection_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "House Staff"
    And the JSON response contains field "summary" with value "Original summary"
    And the JSON response contains field "ownerId" with value "{owner_crud_id}"
    And the JSON response contains field "generator" with value "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}"
    When an API admin sends a PATCH request to "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}/collections/{crud_collection_id}" with body:
      """
      {
        "name": "Estate Staff",
        "summary": "Updated summary"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Estate Staff"
    And the JSON response contains field "summary" with value "Updated summary"
    When an API admin sends a DELETE request to "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}/collections/{crud_collection_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_crud/v1/actors/{owner_crud_id}/collections/{crud_collection_id}"
    Then the HTTP response status code is 404
    And the entity for remembered collection "crud_collection_id" has deleted_on populated in database

  Scenario: Add items with default prepend, before, index, and removeItem custom methods
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_items",
        "name": "Valiant Collections Items"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors" with body:
      """
      {
        "preferredUsername": "item_creator_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "items_owner_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "generator": "{items_owner_id}",
        "name": "Item Alpha"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_alpha_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "generator": "{items_owner_id}",
        "name": "Item Beta"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_beta_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "generator": "{items_owner_id}",
        "name": "Item Gamma"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_gamma_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "generator": "{items_owner_id}",
        "name": "Item Delta"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_delta_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections" with body:
      """
      {
        "name": "Ordered Cast List"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ordered_col_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_alpha_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_beta_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 2
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_alpha_id}"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_gamma_id}",
        "before": "{item_alpha_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 3
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_gamma_id}"
    And the JSON response path "orderedItems.2" has value "{item_alpha_id}"
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}?form=expanded"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 3
    And the JSON response path "orderedItems.0.name" has value "Item Beta"
    And the JSON response path "orderedItems.0.generator" has value "/api/studios/valiant_col_items/v1/actors/{items_owner_id}"
    And the JSON response path "orderedItems.1.name" has value "Item Gamma"
    And the JSON response path "orderedItems.1.generator" has value "/api/studios/valiant_col_items/v1/actors/{items_owner_id}"
    And the JSON response path "orderedItems.2.name" has value "Item Alpha"
    And the JSON response path "orderedItems.2.generator" has value "/api/studios/valiant_col_items/v1/actors/{items_owner_id}"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{item_gamma_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_delta_id}",
        "index": 1
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/actors/{items_owner_id}/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 3
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_delta_id}"
    And the JSON response path "orderedItems.2" has value "{item_alpha_id}"
