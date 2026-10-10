@AdminApi
Feature: User Space Studio Events REST API
  As a studio member or client
  I want to manage events for a studio via user space endpoints
  So that I can coordinate gatherings within the tenant boundary under Row-Level Security

  Scenario: Successfully create event with all fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_all_fields",
        "name": "Valiant All Fields Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_all_fields/v1/locations" with body:
      """
      {
        "name": "Castle Grounds"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "castle_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_all_fields/v1/events" with body:
      """
      {
        "title": "Autumn Harvest Festival",
        "summary": "Annual festival and gathering",
        "locationId": "{castle_loc_id}",
        "startTime": "2026-10-25T18:00:00Z",
        "endTime": "2026-10-27T12:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "title" with value "Autumn Harvest Festival"
    And the JSON response contains field "summary" with value "Annual festival and gathering"
    And the JSON response contains field "locationId" with value "{castle_loc_id}"
    And the JSON response contains field "startTime" with value "2026-10-25T18:00:00Z"
    And the JSON response contains field "endTime" with value "2026-10-27T12:00:00Z"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_event_id"

  Scenario: Successfully create event with minimal fields
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_min",
        "name": "Valiant Minimal Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_min/v1/events" with body:
      """
      {
        "title": "Town Hall Gathering"
      }
      """
    Then the HTTP response status code is 201
    And the JSON response contains field "title" with value "Town Hall Gathering"
    And the JSON response field "summary" is null
    And the JSON response field "locationId" is null
    And the JSON response field "startTime" is null
    And the JSON response field "endTime" is null
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"

  Scenario: Creating event with blank title returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_blank",
        "name": "Valiant Blank Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_blank/v1/events" with body:
      """
      {
        "title": "   "
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating event with invalid time order returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_broken_time",
        "name": "Valiant Broken Time Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_broken_time/v1/events" with body:
      """
      {
        "title": "Broken Timeline Event",
        "startTime": "2026-10-27T12:00:00Z",
        "endTime": "2026-10-25T18:00:00Z"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating event with nonexistent location returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_nonexistent_loc",
        "name": "Valiant Nonexistent Loc Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_nonexistent_loc/v1/events" with body:
      """
      {
        "title": "Unanchored Event",
        "locationId": "01925b6a-93be-7000-8000-000000000999"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Creating event for nonexistent studio returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/studios/unknown/v1/events" with body:
      """
      {
        "title": "Ghost Event"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully list active events for studio
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_list_events",
        "name": "Valiant List Events Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_list_events/v1/events" with body:
      """
      {
        "title": "Event Alpha",
        "startTime": "2026-11-01T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_list_events/v1/events" with body:
      """
      {
        "title": "Event Beta",
        "startTime": "2026-11-05T10:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_list_events/v1/events"
    Then the HTTP response status code is 200
    And the HTTP response content-type contains "application/json"
    And the JSON response array has length 2
    And the JSON response array contains an item with "title" equal to "Event Alpha"
    And the JSON response array contains an item with "title" equal to "Event Beta"

  Scenario: Soft-deleted events are excluded from collection list
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_del_list",
        "name": "Valiant Delete List Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_del_list/v1/events" with body:
      """
      {
        "title": "Active Event"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_del_list/v1/events" with body:
      """
      {
        "title": "To Be Deleted Event"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "event_to_delete"
    When an API admin sends a DELETE request to "/api/studios/valiant_del_list/v1/events/{event_to_delete}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_del_list/v1/events"
    Then the HTTP response status code is 200
    And the JSON response array has length 1
    And the JSON response array contains an item with "title" equal to "Active Event"
    And the JSON response array does not contain an item with "title" equal to "To Be Deleted Event"

  Scenario: Empty collection returned when studio has no events
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_empty_events",
        "name": "Valiant Empty Events Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a GET request to "/api/studios/valiant_empty_events/v1/events"
    Then the HTTP response status code is 200
    And the JSON response array has length 0

  Scenario: Successfully retrieve active event
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_get_event",
        "name": "Valiant Get Event Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_get_event/v1/events" with body:
      """
      {
        "title": "Summer Gala",
        "summary": "Grand evening ballroom gathering"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "summer_gala_id"
    When an API admin sends a GET request to "/api/studios/valiant_get_event/v1/events/{summer_gala_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Summer Gala"
    And the JSON response contains field "summary" with value "Grand evening ballroom gathering"
    And the JSON response does not contain field "tenantId"

  Scenario: Retrieving soft-deleted event returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_get_del",
        "name": "Valiant Get Deleted Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_get_del/v1/events" with body:
      """
      {
        "title": "Ephemeral Event"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "ephemeral_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_get_del/v1/events/{ephemeral_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_get_del/v1/events/{ephemeral_id}"
    Then the HTTP response status code is 404

  Scenario: Cross-tenant event access returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_ct_alpha",
        "name": "Studio CT Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "studio_ct_beta",
        "name": "Studio CT Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/studio_ct_alpha/v1/events" with body:
      """
      {
        "title": "Alpha Secret Event"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_event_id"
    When an API admin sends a GET request to "/api/studios/studio_ct_beta/v1/events/{alpha_event_id}"
    Then the HTTP response status code is 404

  Scenario: Successfully update event title and time range adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_patch_event",
        "name": "Valiant Patch Event Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_patch_event/v1/events" with body:
      """
      {
        "title": "Original Event Title",
        "startTime": "2026-10-25T18:00:00Z",
        "endTime": "2026-10-27T12:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_event_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_patch_event/v1/events/{patch_event_id}?update_mask=title,start_time,end_time" with body:
      """
      {
        "title": "Autumn Harvest Festival - Rescheduled",
        "startTime": "2026-10-26T18:00:00Z",
        "endTime": "2026-10-28T12:00:00Z"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "title" with value "Autumn Harvest Festival - Rescheduled"
    And the JSON response contains field "startTime" with value "2026-10-26T18:00:00Z"
    And the JSON response contains field "endTime" with value "2026-10-28T12:00:00Z"

  Scenario: Patching with invalid time range returns 400
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_bad_patch",
        "name": "Valiant Bad Patch Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_bad_patch/v1/events" with body:
      """
      {
        "title": "Good Event",
        "startTime": "2026-10-25T18:00:00Z",
        "endTime": "2026-10-27T12:00:00Z"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "bad_patch_event_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_bad_patch/v1/events/{bad_patch_event_id}?update_mask=start_time,end_time" with body:
      """
      {
        "startTime": "2026-10-29T18:00:00Z",
        "endTime": "2026-10-28T12:00:00Z"
      }
      """
    Then the HTTP response status code is 400
    And the JSON response error has code 400 and non-empty message

  Scenario: Patching soft-deleted event returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_del_patch",
        "name": "Valiant Delete Patch Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_del_patch/v1/events" with body:
      """
      {
        "title": "Pre-deleted Event"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "del_patch_event_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_del_patch/v1/events/{del_patch_event_id}"
    Then the HTTP response status code is 204
    When an API admin sends a PATCH request to "/api/studios/valiant_del_patch/v1/events/{del_patch_event_id}" with body:
      """
      {
        "title": "Resurrection Attempt"
      }
      """
    Then the HTTP response status code is 404

  Scenario: Successfully soft delete event and confirm database deleted_on
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_soft_del",
        "name": "Valiant Soft Delete Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_soft_del/v1/events" with body:
      """
      {
        "title": "Event To Soft Delete"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "soft_del_event_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_soft_del/v1/events/{soft_del_event_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_soft_del/v1/events/{soft_del_event_id}"
    Then the HTTP response status code is 404
    And the entity for remembered event "soft_del_event_id" has deleted_on populated in database
    When an API admin sends a DELETE request to "/api/studios/valiant_soft_del/v1/events/{soft_del_event_id}"
    Then the HTTP response status code is 404
