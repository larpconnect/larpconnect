@AdminApi
Feature: User Space Studio Campaigns REST API
  As a studio member or client
  I want to manage campaigns for a studio via user space endpoints
  So that ongoing chronicles can be organized and optionally linked to a game system under Row-Level Security

  Scenario: Successfully create campaign linked to active system via canonical URI
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_full",
        "name": "Valiant Campaigns Full Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_full/v1/actors" with body:
      """
      {
        "preferredUsername": "camp_lead"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "camp_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_full/v1/systems" with body:
      """
      {
        "generator": "{camp_actor_id}",
        "name": "Vampire: The Masquerade"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "camp_sys_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_full/v1/campaigns" with body:
      """
      {
        "generator": "{camp_actor_id}",
        "title": "Seattle By Night",
        "summary": "Chronicle across the Pacific Northwest",
        "system": "/api/studios/valiant_camp_full/v1/systems/{camp_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains non-null field "id"
    And the JSON response contains field "title" with value "Seattle By Night"
    And the JSON response contains field "summary" with value "Chronicle across the Pacific Northwest"
    And the JSON response contains field "generator" with value "/api/studios/valiant_camp_full/v1/actors/{camp_actor_id}"
    And the JSON response contains field "system" with value "/api/studios/valiant_camp_full/v1/systems/{camp_sys_id}"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully create systemless campaign
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_nosys",
        "name": "Valiant Systemless Campaign Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_nosys/v1/actors" with body:
      """
      {
        "preferredUsername": "nosys_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "nosys_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_nosys/v1/campaigns" with body:
      """
      {
        "generator": "{nosys_actor_id}",
        "title": "Freeform Anthology"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "title" with value "Freeform Anthology"
    And the JSON response contains field "generator" with value "/api/studios/valiant_camp_nosys/v1/actors/{nosys_actor_id}"
    And the JSON response field "system" is null
    And the JSON response field "summary" is null
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating campaign with nonexistent, cross-studio, non-System, or soft-deleted system returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_bad_sys",
        "name": "Campaign Bad System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_other_studio",
        "name": "Campaign Other Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/actors" with body:
      """
      {
        "preferredUsername": "cb_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cb_actor_id"
    When an API admin sends a POST request to "/api/studios/camp_other_studio/v1/actors" with body:
      """
      {
        "preferredUsername": "other_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "other_actor_id"
    When an API admin sends a POST request to "/api/studios/camp_other_studio/v1/systems" with body:
      """
      {
        "generator": "{other_actor_id}",
        "name": "Cross Studio Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "other_sys_id"
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/locations" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "name": "Location Not A System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cb_loc_id"
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/systems" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "name": "Retired Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "retired_sys_id"
    When an API admin sends a DELETE request to "/api/studios/camp_bad_sys/v1/systems/{retired_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/campaigns" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "title": "Nonexistent System Campaign",
        "system": "01925b6a-93be-7000-8000-000000000999"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/campaigns" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "title": "Cross Studio System Campaign",
        "system": "{other_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/campaigns" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "title": "Non System Entity Campaign",
        "system": "{cb_loc_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/camp_bad_sys/v1/campaigns" with body:
      """
      {
        "generator": "{cb_actor_id}",
        "title": "Soft Deleted System Campaign",
        "system": "{retired_sys_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating campaign with blank title or invalid generator returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_blank",
        "name": "Valiant Blank Campaign Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_blank/v1/actors" with body:
      """
      {
        "preferredUsername": "blank_camp_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "blank_camp_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_blank/v1/campaigns" with body:
      """
      {
        "generator": "{blank_camp_actor_id}",
        "title": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a POST request to "/api/studios/valiant_camp_blank/v1/campaigns" with body:
      """
      {
        "generator": "01925b6a-93be-7000-8000-000000000999",
        "title": "Orphaned Campaign"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating campaign for nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/studios/unknown/v1/campaigns" with body:
      """
      {
        "generator": "018f2345-6789-7abc-def0-000000000000",
        "title": "Ghost Campaign"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully list active campaigns with optional systemId filter excluding soft-deleted
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_filter",
        "name": "Valiant Filter Campaigns Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/actors" with body:
      """
      {
        "preferredUsername": "cf_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cf_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/systems" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "name": "System Alpha"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cf_sys_a_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/systems" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "name": "System Beta"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cf_sys_b_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/campaigns" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "title": "Chronicle Alpha",
        "system": "{cf_sys_a_id}"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/campaigns" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "title": "Chronicle Beta",
        "system": "{cf_sys_b_id}"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/campaigns" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "title": "Chronicle Unbound"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_filter/v1/campaigns" with body:
      """
      {
        "generator": "{cf_actor_id}",
        "title": "Chronicle Soft Deleted",
        "system": "{cf_sys_a_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cf_del_camp_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_camp_filter/v1/campaigns/{cf_del_camp_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_camp_filter/v1/campaigns"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array has length 3
    And the JSON response array contains an item with "title" equal to "Chronicle Alpha"
    And the JSON response array contains an item with "title" equal to "Chronicle Beta"
    And the JSON response array contains an item with "title" equal to "Chronicle Unbound"
    And the JSON response array does not contain an item with "title" equal to "Chronicle Soft Deleted"
    When an API admin sends a GET request to "/api/studios/valiant_camp_filter/v1/campaigns?systemId={cf_sys_a_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response path "0.title" has value "Chronicle Alpha"
    And the JSON response path "0.system" has value "/api/studios/valiant_camp_filter/v1/systems/{cf_sys_a_id}"
    When an API admin sends a GET request to "/api/studios/valiant_camp_filter/v1/campaigns?systemId=/api/studios/valiant_camp_filter/v1/systems/{cf_sys_b_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response path "0.title" has value "Chronicle Beta"
    And the JSON response path "0.system" has value "/api/studios/valiant_camp_filter/v1/systems/{cf_sys_b_id}"

  Scenario: Listing campaigns with malformed or cross-studio systemId filter returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_filter_bad_a",
        "name": "Campaign Filter Bad A"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_filter_bad_b",
        "name": "Campaign Filter Bad B"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/camp_filter_bad_b/v1/actors" with body:
      """
      {
        "preferredUsername": "cfb_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cfb_actor_id"
    When an API admin sends a POST request to "/api/studios/camp_filter_bad_b/v1/systems" with body:
      """
      {
        "generator": "{cfb_actor_id}",
        "name": "Foreign System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "cfb_sys_id"
    When an API admin sends a GET request to "/api/studios/camp_filter_bad_a/v1/campaigns?systemId=not-a-uuid"
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message
    When an API admin sends a GET request to "/api/studios/camp_filter_bad_a/v1/campaigns?systemId=/api/studios/camp_filter_bad_b/v1/systems/{cfb_sys_id}"
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Campaign referencing a soft-deleted system masks system as null on get and list
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_mask_sys",
        "name": "Campaign Mask Soft Deleted System Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/camp_mask_sys/v1/actors" with body:
      """
      {
        "preferredUsername": "ms_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ms_actor_id"
    When an API admin sends a POST request to "/api/studios/camp_mask_sys/v1/systems" with body:
      """
      {
        "generator": "{ms_actor_id}",
        "name": "Temporary System"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ms_sys_id"
    When an API admin sends a POST request to "/api/studios/camp_mask_sys/v1/campaigns" with body:
      """
      {
        "generator": "{ms_actor_id}",
        "title": "Surviving Chronicle",
        "system": "{ms_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ms_camp_id"
    When an API admin sends a DELETE request to "/api/studios/camp_mask_sys/v1/systems/{ms_sys_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/camp_mask_sys/v1/campaigns/{ms_camp_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Surviving Chronicle"
    And the JSON response field "system" is null
    When an API admin sends a GET request to "/api/studios/camp_mask_sys/v1/campaigns"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response path "0.id" has value "{ms_camp_id}"
    And the JSON response path "0.title" has value "Surviving Chronicle"
    And the JSON response path "0.system" has value "null"
    When an API admin sends a GET request to "/api/studios/camp_mask_sys/v1/campaigns?systemId={ms_sys_id}"
    Then the HTTP response status code is 200
    And the JSON response array has length 0

  Scenario: Successfully retrieve active campaign and return 404 for cross-tenant or soft-deleted access
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_ct_alpha",
        "name": "Campaign CT Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "camp_ct_beta",
        "name": "Campaign CT Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/camp_ct_alpha/v1/actors" with body:
      """
      {
        "preferredUsername": "camp_ct_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "camp_ct_actor_id"
    When an API admin sends a POST request to "/api/studios/camp_ct_alpha/v1/systems" with body:
      """
      {
        "generator": "{camp_ct_actor_id}",
        "name": "CT Ruleset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "camp_ct_sys_id"
    When an API admin sends a POST request to "/api/studios/camp_ct_alpha/v1/campaigns" with body:
      """
      {
        "generator": "{camp_ct_actor_id}",
        "title": "Alpha Secret Chronicle",
        "summary": "Hidden chronicle",
        "system": "{camp_ct_sys_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_camp_id"
    When an API admin sends a GET request to "/api/studios/camp_ct_alpha/v1/campaigns/{alpha_camp_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "id" with value "{alpha_camp_id}"
    And the JSON response contains field "title" with value "Alpha Secret Chronicle"
    And the JSON response contains field "summary" with value "Hidden chronicle"
    And the JSON response contains field "generator" with value "/api/studios/camp_ct_alpha/v1/actors/{camp_ct_actor_id}"
    And the JSON response contains field "system" with value "/api/studios/camp_ct_alpha/v1/systems/{camp_ct_sys_id}"
    And the JSON response does not contain field "tenantId"
    When an API admin sends a GET request to "/api/studios/camp_ct_beta/v1/campaigns/{alpha_camp_id}"
    Then the HTTP response status code is 404
    When an API admin sends a DELETE request to "/api/studios/camp_ct_alpha/v1/campaigns/{alpha_camp_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/camp_ct_alpha/v1/campaigns/{alpha_camp_id}"
    Then the HTTP response status code is 404

  Scenario: Successfully update campaign title, summary, and clear system adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_patch",
        "name": "Valiant Patch Campaign Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "patch_camp_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_camp_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_patch/v1/systems" with body:
      """
      {
        "generator": "{patch_camp_actor_id}",
        "name": "Ruleset One"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pc_sys_1_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_patch/v1/systems" with body:
      """
      {
        "generator": "{patch_camp_actor_id}",
        "name": "Ruleset Two"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "pc_sys_2_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_patch/v1/campaigns" with body:
      """
      {
        "generator": "{patch_camp_actor_id}",
        "title": "Season One",
        "summary": "Initial arc summary",
        "system": "{pc_sys_1_id}"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_camp_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_camp_patch/v1/campaigns/{patch_camp_id}?update_mask=title,system" with body:
      """
      {
        "title": "Season Two",
        "summary": "Unmasked summary should remain ignored",
        "system": "{pc_sys_2_id}"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Season Two"
    And the JSON response contains field "summary" with value "Initial arc summary"
    And the JSON response contains field "system" with value "/api/studios/valiant_camp_patch/v1/systems/{pc_sys_2_id}"
    When an API admin sends a PATCH request to "/api/studios/valiant_camp_patch/v1/campaigns/{patch_camp_id}?update_mask=system" with body:
      """
      {
        "system": null
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Season Two"
    And the JSON response field "system" is null

  Scenario: Patching campaign with non-System reference returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_bad_patch",
        "name": "Valiant Bad Patch Campaign Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_bad_patch/v1/actors" with body:
      """
      {
        "preferredUsername": "bad_pc_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_pc_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_bad_patch/v1/locations" with body:
      """
      {
        "generator": "{bad_pc_actor_id}",
        "name": "Not A System Location"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_pc_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_bad_patch/v1/campaigns" with body:
      """
      {
        "generator": "{bad_pc_actor_id}",
        "title": "Valid Chronicle"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_pc_camp_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_camp_bad_patch/v1/campaigns/{bad_pc_camp_id}?update_mask=system" with body:
      """
      {
        "system": "{bad_pc_loc_id}"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Successfully soft delete campaign and confirm database deleted_on
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_camp_soft_del",
        "name": "Valiant Soft Delete Campaign Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_camp_soft_del/v1/actors" with body:
      """
      {
        "preferredUsername": "soft_del_camp_actor"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_camp_actor_id"
    When an API admin sends a POST request to "/api/studios/valiant_camp_soft_del/v1/campaigns" with body:
      """
      {
        "generator": "{soft_del_camp_actor_id}",
        "title": "Campaign To Soft Delete"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_camp_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_camp_soft_del/v1/campaigns/{soft_del_camp_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_camp_soft_del/v1/campaigns/{soft_del_camp_id}"
    Then the HTTP response status code is 404
    And the entity for remembered campaign "soft_del_camp_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_camp_soft_del/v1/campaigns/{soft_del_camp_id}"
    Then the HTTP response status code is 404
