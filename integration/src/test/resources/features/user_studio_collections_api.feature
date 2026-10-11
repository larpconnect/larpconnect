@AdminApi
Feature: User Space Studio Collections REST API
  As a studio member or client
  I want to manage collections and collection items for a studio via user space endpoints
  So that I can curate ordered entities within the tenant boundary under Row-Level Security

  Scenario: Successfully create collection with all fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_all",
        "name": "Valiant Collections All Fields"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_all/v1/individuals" with body:
      """
      {
        "name": "Jane Eyre"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_ind_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_all/v1/collections" with body:
      """
      {
        "ownerId": "{owner_ind_id}",
        "name": "Featured Cast",
        "summary": "Primary cast members for season 1"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "name" with value "Featured Cast"
    And the JSON response contains field "summary" with value "Primary cast members for season 1"
    And the JSON response contains field "ownerId" with value "{owner_ind_id}"
    And the JSON response contains field "type" with value "OrderedCollection"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the JSON response array "orderedItems" has length 0
    And the response field "id" is remembered as "created_collection_id"

  Scenario: Successfully create collection with minimal fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_min",
        "name": "Valiant Collections Minimal"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_min/v1/individuals" with body:
      """
      {
        "name": "Edward Rochester"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_min_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_min/v1/collections" with body:
      """
      {
        "ownerId": "{owner_min_id}",
        "name": "Crew Members"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "name" with value "Crew Members"
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
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/individuals" with body:
      """
      {
        "name": "St. John Rivers"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_dup_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/collections" with body:
      """
      {
        "ownerId": "{owner_dup_id}",
        "name": "Parish Cast"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_dup/v1/collections" with body:
      """
      {
        "ownerId": "{owner_dup_id}",
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
    When an API admin sends a POST request to "/api/studios/valiant_col_blank/v1/individuals" with body:
      """
      {
        "name": "Adele Varens"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_blank_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_blank/v1/collections" with body:
      """
      {
        "ownerId": "{owner_blank_id}",
        "name": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating collection with nonexistent owner returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_fk",
        "name": "Valiant Collections FK"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_col_fk/v1/collections" with body:
      """
      {
        "ownerId": "019273c5-9271-7000-8000-000000000999",
        "name": "Ghost Collection"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Deliberate omission of collection listing endpoint returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_col_list",
        "name": "Valiant Collections List Omission"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_col_list/v1/collections"
    Then the HTTP response status code is 404
    And the JSON response error has code 404 and non-empty message

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
    When an API admin sends a POST request to "/api/studios/valiant_col_crud/v1/individuals" with body:
      """
      {
        "name": "Mrs. Fairfax"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "owner_crud_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_crud/v1/collections" with body:
      """
      {
        "ownerId": "{owner_crud_id}",
        "name": "House Staff",
        "summary": "Original summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "crud_collection_id"
    When an API admin sends a GET request to "/api/studios/valiant_col_crud/v1/collections/{crud_collection_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "House Staff"
    And the JSON response contains field "summary" with value "Original summary"
    When an API admin sends a PATCH request to "/api/studios/valiant_col_crud/v1/collections/{crud_collection_id}" with body:
      """
      {
        "name": "Estate Staff",
        "summary": "Updated summary"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Estate Staff"
    And the JSON response contains field "summary" with value "Updated summary"
    When an API admin sends a DELETE request to "/api/studios/valiant_col_crud/v1/collections/{crud_collection_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_crud/v1/collections/{crud_collection_id}"
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
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "name": "Owner Entity"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "items_owner_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "name": "Item Alpha"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_alpha_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "name": "Item Beta"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_beta_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/individuals" with body:
      """
      {
        "name": "Item Gamma"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "item_gamma_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/collections" with body:
      """
      {
        "ownerId": "{items_owner_id}",
        "name": "Ordered Cast List"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ordered_col_id"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_alpha_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_beta_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 2
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_alpha_id}"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}:addItem" with body:
      """
      {
        "refersToId": "{item_gamma_id}",
        "before": "{item_alpha_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 3
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_gamma_id}"
    And the JSON response path "orderedItems.2" has value "{item_alpha_id}"
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}?form=expanded"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 3
    And the JSON response path "orderedItems.0.name" has value "Item Beta"
    And the JSON response path "orderedItems.1.name" has value "Item Gamma"
    And the JSON response path "orderedItems.2.name" has value "Item Alpha"
    When an API admin sends a POST request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}:removeItem" with body:
      """
      {
        "refersToId": "{item_gamma_id}"
      }
      """
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_col_items/v1/collections/{ordered_col_id}"
    Then the HTTP response status code is 200
    And the JSON response array "orderedItems" has length 2
    And the JSON response path "orderedItems.0" has value "{item_beta_id}"
    And the JSON response path "orderedItems.1" has value "{item_alpha_id}"
