@AdminApi
Feature: User Space Studio Game Systems REST API
  As a studio member or client
  I want to manage game systems for a studio via user space endpoints
  So that tabletop and LARP rulesets can be cataloged within the tenant boundary under Row-Level Security

  Scenario: Successfully create game system with bare generator UUID and summary
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_full",
        "name": "Valiant Systems Full Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_full/v1/actors" with body:
      """
      {
        "preferredUsername": "sys_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_full/v1/systems" with body:
      """
      {
        "generator": "{sys_actor_id}",
        "name": "Vampire: The Masquerade 5e",
        "summary": "Personal and political horror ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains non-null field "id"
    And the JSON response contains field "name" with value "Vampire: The Masquerade 5e"
    And the JSON response contains field "summary" with value "Personal and political horror ruleset"
    And the JSON response contains field "generator" with value "/api/studios/valiant_sys_full/v1/actors/{sys_actor_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully create game system with canonical generator URI and minimal fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_min",
        "name": "Valiant Systems Minimal Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_min/v1/actors" with body:
      """
      {
        "preferredUsername": "sys_min_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "sys_min_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_min/v1/systems" with body:
      """
      {
        "generator": "/api/studios/valiant_sys_min/v1/actors/{sys_min_actor_id}",
        "name": "Dungeons & Dragons 5e"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "name" with value "Dungeons & Dragons 5e"
    And the JSON response contains field "generator" with value "/api/studios/valiant_sys_min/v1/actors/{sys_min_actor_id}"
    And the JSON response field "summary" is null
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating game system with duplicate case-insensitive name returns 409
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_dup",
        "name": "Valiant Duplicate System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_dup/v1/actors" with body:
      """
      {
        "preferredUsername": "dup_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "dup_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_dup/v1/systems" with body:
      """
      {
        "generator": "{dup_sys_actor_id}",
        "name": "Call of Cthulhu"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_dup/v1/systems" with body:
      """
      {
        "generator": "{dup_sys_actor_id}",
        "name": "call of cthulhu"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Creating game system with same name succeeds in another studio and rejects soft-deleted name reuse in same studio with 409
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_reuse_a",
        "name": "Valiant System Reuse Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_reuse_b",
        "name": "Valiant System Reuse Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_reuse_a/v1/actors" with body:
      """
      {
        "preferredUsername": "reuse_a_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "reuse_a_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_reuse_b/v1/actors" with body:
      """
      {
        "preferredUsername": "reuse_b_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "reuse_b_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_reuse_a/v1/systems" with body:
      """
      {
        "generator": "{reuse_a_actor_id}",
        "name": "Blades in the Dark"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "reuse_old_sys_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_reuse_b/v1/systems" with body:
      """
      {
        "generator": "{reuse_b_actor_id}",
        "name": "Blades in the Dark"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a DELETE request to "/api/studios/valiant_sys_reuse_a/v1/systems/{reuse_old_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/valiant_sys_reuse_a/v1/systems" with body:
      """
      {
        "generator": "{reuse_a_actor_id}",
        "name": "Blades in the Dark"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Creating game system with blank name returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_blank",
        "name": "Valiant Blank System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_blank/v1/actors" with body:
      """
      {
        "preferredUsername": "blank_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "blank_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_blank/v1/systems" with body:
      """
      {
        "generator": "{blank_sys_actor_id}",
        "name": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating game system with missing or non-Actor generator returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_bad_gen",
        "name": "Valiant Bad System Generator Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_bad_gen/v1/actors" with body:
      """
      {
        "preferredUsername": "bad_gen_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_gen_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_bad_gen/v1/locations" with body:
      """
      {
        "generator": "{bad_gen_actor_id}",
        "name": "Non Actor Entity"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_gen_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_bad_gen/v1/systems" with body:
      """
      {
        "generator": "{bad_gen_loc_id}",
        "name": "Invalid Generator Type System"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_sys_bad_gen/v1/systems" with body:
      """
      {
        "generator": "01925b6a-93be-7000-8000-000000000999",
        "name": "Nonexistent Generator System"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating game system for nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/studios/unknown/v1/systems" with body:
      """
      {
        "generator": "018f2345-6789-7abc-def0-000000000000",
        "name": "Ghost System"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully list active game systems ordered alphabetically excluding soft-deleted
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_list",
        "name": "Valiant List Systems Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_list/v1/actors" with body:
      """
      {
        "preferredUsername": "list_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "list_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_list/v1/systems" with body:
      """
      {
        "generator": "{list_sys_actor_id}",
        "name": "Zweihander"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_list/v1/systems" with body:
      """
      {
        "generator": "{list_sys_actor_id}",
        "name": "Beta System"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_list/v1/systems" with body:
      """
      {
        "generator": "{list_sys_actor_id}",
        "name": "alpha system"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_list/v1/systems" with body:
      """
      {
        "generator": "{list_sys_actor_id}",
        "name": "Deleted System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "list_del_sys_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_sys_list/v1/systems/{list_del_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_sys_list/v1/systems"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array has length 3
    And the JSON response path "0.name" has value "alpha system"
    And the JSON response path "1.name" has value "Beta System"
    And the JSON response path "2.name" has value "Zweihander"
    And the JSON response array does not contain an item with "name" equal to "Deleted System"

  Scenario: Listing game systems for studio with none returns empty array
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_empty",
        "name": "Valiant Empty Systems Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_sys_empty/v1/systems"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array has length 0

  Scenario: Successfully retrieve active game system
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_get",
        "name": "Valiant Get System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_get/v1/actors" with body:
      """
      {
        "preferredUsername": "get_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "get_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_get/v1/systems" with body:
      """
      {
        "generator": "{get_sys_actor_id}",
        "name": "Fate Core",
        "summary": "Narrative-first generic system"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "get_sys_id"
    When an API admin sends a GET request to "/api/studios/valiant_sys_get/v1/systems/{get_sys_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "id" with value "{get_sys_id}"
    And the JSON response contains field "name" with value "Fate Core"
    And the JSON response contains field "summary" with value "Narrative-first generic system"
    And the JSON response contains field "generator" with value "/api/studios/valiant_sys_get/v1/actors/{get_sys_actor_id}"
    And the JSON response does not contain field "tenantId"

  Scenario: Cross-tenant and soft-deleted game system access returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "sys_ct_alpha",
        "name": "System CT Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "sys_ct_beta",
        "name": "System CT Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/sys_ct_alpha/v1/actors" with body:
      """
      {
        "preferredUsername": "sys_ct_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "sys_ct_actor_id"
    When an API admin sends a POST request to "/api/studios/sys_ct_alpha/v1/systems" with body:
      """
      {
        "generator": "{sys_ct_actor_id}",
        "name": "Alpha Exclusive System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_sys_id"
    When an API admin sends a GET request to "/api/studios/sys_ct_beta/v1/systems/{alpha_sys_id}"
    Then the HTTP response status code is 404
    When an API admin sends a DELETE request to "/api/studios/sys_ct_alpha/v1/systems/{alpha_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/sys_ct_alpha/v1/systems/{alpha_sys_id}"
    Then the HTTP response status code is 404

  Scenario: Successfully update game system name and summary adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_patch",
        "name": "Valiant Patch System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "patch_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_patch/v1/systems" with body:
      """
      {
        "generator": "{patch_sys_actor_id}",
        "name": "Fiasco",
        "summary": "Initial summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_sys_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_sys_patch/v1/systems/{patch_sys_id}?update_mask=name" with body:
      """
      {
        "name": "Fiasco Classic",
        "summary": "Unmasked summary should remain unchanged"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Fiasco Classic"
    And the JSON response contains field "summary" with value "Initial summary"
    When an API admin sends a PATCH request to "/api/studios/valiant_sys_patch/v1/systems/{patch_sys_id}?update_mask=summary" with body:
      """
      {
        "summary": "Storytelling heist tabletop RPG"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Fiasco Classic"
    And the JSON response contains field "summary" with value "Storytelling heist tabletop RPG"
    When an API admin sends a PATCH request to "/api/studios/valiant_sys_patch/v1/systems/{patch_sys_id}?update_mask=summary" with body:
      """
      {
        "summary": null
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Fiasco Classic"
    And the JSON response field "summary" is null

  Scenario: Patching game system to duplicate case-insensitive name returns 409
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_patch_dup",
        "name": "Valiant Patch Duplicate System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_patch_dup/v1/actors" with body:
      """
      {
        "preferredUsername": "pdup_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pdup_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_patch_dup/v1/systems" with body:
      """
      {
        "generator": "{pdup_sys_actor_id}",
        "name": "Shadowrun"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_patch_dup/v1/systems" with body:
      """
      {
        "generator": "{pdup_sys_actor_id}",
        "name": "Cyberpunk RED"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cp_sys_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_sys_patch_dup/v1/systems/{cp_sys_id}?update_mask=name" with body:
      """
      {
        "name": "SHADOWRUN"
      }
      """
    Then the HTTP response status code is 409
    And the JSON response error has code 409 and non-empty message

  Scenario: Successfully soft delete game system and confirm database deleted_on
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_sys_soft_del",
        "name": "Valiant Soft Delete System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_sys_soft_del/v1/actors" with body:
      """
      {
        "preferredUsername": "soft_del_sys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_sys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_sys_soft_del/v1/systems" with body:
      """
      {
        "generator": "{soft_del_sys_actor_id}",
        "name": "System To Soft Delete"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_sys_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_sys_soft_del/v1/systems/{soft_del_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_sys_soft_del/v1/systems/{soft_del_sys_id}"
    Then the HTTP response status code is 404
    And the entity for remembered system "soft_del_sys_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_sys_soft_del/v1/systems/{soft_del_sys_id}"
    Then the HTTP response status code is 404
