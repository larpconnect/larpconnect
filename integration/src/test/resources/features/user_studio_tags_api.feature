@AdminApi
Feature: User Space Studio Hashtags REST API
  As a studio member or client
  I want to manage hashtags for a studio via user space endpoints
  So that I can categorize and link studio content within the tenant boundary under Row-Level Security

  Scenario: Successfully create hashtag for studio by alias
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_tags",
        "name": "Valiant Tags Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_tags/v1/tags" with body:
      """
      {
        "tag": "SolarPunk",
        "summary": "Speculative eco-futuristic aesthetic"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "tag" with value "SolarPunk"
    And the JSON response contains field "linkType" with value "hashtag"
    And the JSON response contains field "url" with value "/api/studios/valiant_tags/v1/tags/SolarPunk"
    And the JSON response contains field "mediaType" with value "application/json"
    And the JSON response contains field "summary" with value "Speculative eco-futuristic aesthetic"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_tag_id"

  Scenario: Idempotent hashtag creation returns existing tag without duplication
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_idempotent",
        "name": "Valiant Idempotent Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_idempotent/v1/tags" with body:
      """
      {
        "tag": "SolarPunk",
        "summary": "First creation"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "first_tag_id"
    When an API admin sends a POST request to "/api/studios/valiant_idempotent/v1/tags" with body:
      """
      {
        "tag": "#solarpunk",
        "summary": "Duplicate attempt with lowercase and hash"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "id" with value "{first_tag_id}"
    And the JSON response contains field "tag" with value "SolarPunk"

  Scenario: Successfully create hashtag for studio by public studio ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_uuid_tags",
        "name": "Valiant UUID Tags"
      }
      """
    Then the HTTP response status code is 201
    And the response field "studioId" is remembered as "uuid_studio_id"
    When an API admin sends a POST request to "/api/studios/{uuid_studio_id}/v1/tags" with body:
      """
      {
        "tag": "CyberPunk"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "tag" with value "CyberPunk"

  Scenario: Bulk provision studio hashtags via batchCreate adhering to AIP-233
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_batch",
        "name": "Valiant Batch Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_batch/v1/tags" with body:
      """
      {
        "tag": "ExistingTag"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_batch/v1/tags:batchCreate" with body:
      """
      {
        "requests": [
          {
            "tag": "NewBatchTag",
            "summary": "Bulk created"
          },
          {
            "tag": "existingtag"
          }
        ]
      }
      """
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array "tags" contains "NewBatchTag"
    And the JSON response array "tags" contains "ExistingTag"

  Scenario: Successfully retrieve active hashtag by ID and by tag name
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_dual_get",
        "name": "Valiant Dual Get"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_dual_get/v1/tags" with body:
      """
      {
        "tag": "SolarPunk"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "dual_tag_id"
    When an API admin sends a GET request to "/api/studios/valiant_dual_get/v1/tags/{dual_tag_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "tag" with value "SolarPunk"
    When an API admin sends a GET request to "/api/studios/valiant_dual_get/v1/tags/solarpunk"
    Then the HTTP response status code is 200
    And the JSON response contains field "tag" with value "SolarPunk"
    And the JSON response contains field "id" with value "{dual_tag_id}"

  Scenario: Successfully list all active studio hashtags
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_list_tags",
        "name": "Valiant List Tags"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_list_tags/v1/tags" with body:
      """
      {
        "tag": "TagAlpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_list_tags/v1/tags" with body:
      """
      {
        "tag": "TagBeta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_list_tags/v1/tags"
    Then the HTTP response status code is 200
    And the JSON response array contains an item with "tag" equal to "TagAlpha"
    And the JSON response array contains an item with "tag" equal to "TagBeta"

  Scenario: Successfully update hashtag summary adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_patch_tags",
        "name": "Valiant Patch Tags"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_patch_tags/v1/tags" with body:
      """
      {
        "tag": "OriginalTag",
        "summary": "Original summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_tag_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_patch_tags/v1/tags/{patch_tag_id}?update_mask=summary" with body:
      """
      {
        "summary": "Updated aesthetic description"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "summary" with value "Updated aesthetic description"
    And the JSON response contains field "tag" with value "OriginalTag"

  Scenario: Successfully soft delete hashtag
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_del_tags",
        "name": "Valiant Delete Tags"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_del_tags/v1/tags" with body:
      """
      {
        "tag": "TagToDelete"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "del_tag_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_del_tags/v1/tags/{del_tag_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_del_tags/v1/tags/{del_tag_id}"
    Then the HTTP response status code is 404
    And the entity for remembered tag "del_tag_id" has deleted_on populated in database

  Scenario: Multi-tenant isolation prevents cross-tenant access to hashtags
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_tenant_a",
        "name": "Studio Tenant A"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_tenant_b",
        "name": "Studio Tenant B"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/studio_tenant_a/v1/tags" with body:
      """
      {
        "tag": "SecretTag"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "tenant_a_tag_id"
    When an API admin sends a GET request to "/api/studios/studio_tenant_b/v1/tags/{tenant_a_tag_id}"
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/studio_tenant_b/v1/tags/SecretTag"
    Then the HTTP response status code is 404
