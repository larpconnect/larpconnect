@AdminApi
Feature: User Space Studio Links REST API
  As a studio member or client
  I want to manage external links for a studio via user space endpoints
  So that I can configure studio links within the tenant boundary under Row-Level Security

  Scenario: Successfully create link for studio by alias
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant",
        "name": "Valiant Games"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant/v1/actors" with body:
      """
      {
        "preferredUsername": "link_creator"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "link_creator_id"
    When an API admin sends a POST request to "/api/studios/valiant/v1/links" with body:
      """
      {
        "generator": "/api/studios/valiant/v1/actors/{link_creator_id}",
        "linkType": "website",
        "url": "https://valiant.example.com",
        "mediaType": "text/html",
        "summary": "Official Studio Homepage"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "linkType" with value "website"
    And the JSON response contains field "url" with value "https://valiant.example.com"
    And the JSON response contains field "mediaType" with value "text/html"
    And the JSON response contains field "summary" with value "Official Studio Homepage"
    And the JSON response contains field "generator" with value "/api/studios/valiant/v1/actors/{link_creator_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_link_id"

  Scenario: Successfully create link for studio by public studio ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_uuid_links",
        "name": "Valiant UUID Links"
      }
      """
    Then the HTTP response status code is 201
    And the response field "studioId" is remembered as "valiant_uuid_studio_id"
    When an API admin sends a POST request to "/api/studios/valiant_uuid_links/v1/actors" with body:
      """
      {
        "preferredUsername": "uuid_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "uuid_actor_id"
    When an API admin sends a POST request to "/api/studios/{valiant_uuid_studio_id}/v1/links" with body:
      """
      {
        "generator": "{uuid_actor_id}",
        "linkType": "discord",
        "url": "https://discord.gg/valiant"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "linkType" with value "discord"
    And the JSON response contains field "url" with value "https://discord.gg/valiant"
    And the JSON response contains field "generator" with value "/api/studios/{valiant_uuid_studio_id}/v1/actors/{uuid_actor_id}"

  Scenario: Successfully retrieve active link by ID
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_retrieve",
        "name": "Valiant Retrieve"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_retrieve/v1/actors" with body:
      """
      {
        "preferredUsername": "docs_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "docs_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_retrieve/v1/links" with body:
      """
      {
        "generator": "{docs_actor_id}",
        "linkType": "docs",
        "url": "https://docs.valiant.example.com"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "retrieve_link_id"
    When an API admin sends a GET request to "/api/studios/valiant_retrieve/v1/links/{retrieve_link_id}"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "linkType" with value "docs"
    And the JSON response contains field "url" with value "https://docs.valiant.example.com"
    And the JSON response contains field "generator" with value "/api/studios/valiant_retrieve/v1/actors/{docs_actor_id}"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully update link URL and summary adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_patch",
        "name": "Valiant Patch"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "wiki_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "wiki_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_patch/v1/links" with body:
      """
      {
        "generator": "{wiki_actor_id}",
        "linkType": "wiki",
        "url": "https://old.wiki.example.com",
        "summary": "Old Wiki"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_link_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_patch/v1/links/{patch_link_id}?update_mask=url,summary" with body:
      """
      {
        "url": "https://new.wiki.example.com",
        "summary": "Updated Wiki"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "linkType" with value "wiki"
    And the JSON response contains field "url" with value "https://new.wiki.example.com"
    And the JSON response contains field "summary" with value "Updated Wiki"

  Scenario: Successfully soft-delete link
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_delete",
        "name": "Valiant Delete"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_delete/v1/actors" with body:
      """
      {
        "preferredUsername": "forum_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "forum_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_delete/v1/links" with body:
      """
      {
        "generator": "{forum_actor_id}",
        "linkType": "forum",
        "url": "https://forum.valiant.example.com"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "delete_link_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_delete/v1/links/{delete_link_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_delete/v1/links/{delete_link_id}"
    Then the HTTP response status code is 404
    And the entity for remembered link "delete_link_id" has deleted_on populated in database

  Scenario: Cross-tenant link access returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_alpha",
        "name": "Studio Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_beta",
        "name": "Studio Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/studio_alpha/v1/actors" with body:
      """
      {
        "preferredUsername": "alpha_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_actor_id"
    When an API admin sends a POST request to "/api/studios/studio_alpha/v1/links" with body:
      """
      {
        "generator": "{alpha_actor_id}",
        "linkType": "secret",
        "url": "https://alpha.internal.example.com"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_link_id"
    When an API admin sends a GET request to "/api/studios/studio_beta/v1/links/{alpha_link_id}"
    Then the HTTP response status code is 404

  Scenario: Requesting nonexistent studio or invalid link ID returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a GET request to "/api/studios/nonexistent_studio/v1/links/11111111-1111-1111-1111-111111111111"
    Then the HTTP response status code is 404
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_invalid_id",
        "name": "Valiant Invalid ID"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_invalid_id/v1/links/not-a-valid-uuid"
    Then the HTTP response status code is 404

  Scenario: Unfiltered link listing is rejected with 405 Method Not Allowed
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_listing",
        "name": "Valiant Listing"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_listing/v1/links"
    Then the HTTP response status code is 405
