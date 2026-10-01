@AdminApi
Feature: User Space Studio Locations and Addresses REST API
  As a studio member or client
  I want to manage physical locations and subordinate addresses for a studio
  So that I can configure studio venues and contact locations within the tenant boundary under Row-Level Security

  Scenario: Successfully create location for studio and retrieve it
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_loc",
        "name": "Valiant Locations Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_loc/v1/locations" with body:
      """
      {
        "name": "Main Campsite",
        "summary": "Primary outdoor campgrounds"
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "name" with value "Main Campsite"
    And the JSON response contains field "summary" with value "Primary outdoor campgrounds"
    And the JSON response contains non-null field "createdOn"
    And the JSON response contains non-null field "updatedOn"
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_location_id"
    When an API admin sends a GET request to "/api/studios/valiant_loc/v1/locations/{created_location_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Main Campsite"
    And the JSON response contains field "summary" with value "Primary outdoor campgrounds"
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully update location name and summary adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_loc_patch",
        "name": "Valiant Patch Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_loc_patch/v1/locations" with body:
      """
      {
        "name": "Old Grounds",
        "summary": "Old summary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_location_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_loc_patch/v1/locations/{patch_location_id}?update_mask=name,summary" with body:
      """
      {
        "name": "Renovated Grounds",
        "summary": "Updated campsite facilities"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "name" with value "Renovated Grounds"
    And the JSON response contains field "summary" with value "Updated campsite facilities"

  Scenario: Successfully soft-delete location
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_loc_del",
        "name": "Valiant Delete Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_loc_del/v1/locations" with body:
      """
      {
        "name": "Grounds to Delete"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "delete_location_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_loc_del/v1/locations/{delete_location_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_loc_del/v1/locations/{delete_location_id}"
    Then the HTTP response status code is 404
    And the entity for remembered location "delete_location_id" has deleted_on populated in database

  Scenario: Successfully create address with GeoJSON point and retrieve it
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_addr",
        "name": "Valiant Address Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_addr/v1/locations" with body:
      """
      {
        "name": "Evergreen Lodge"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "addr_location_id"
    When an API admin sends a POST request to "/api/studios/valiant_addr/v1/locations/{addr_location_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "100 Pine Needle Way",
        "addressLine2": "Cabin 4",
        "locality": "North Bend",
        "administrativeArea": "WA",
        "postalCode": "98045",
        "countryCode": "US",
        "geom": {
          "type": "Point",
          "coordinates": [-121.785, 47.495]
        }
      }
      """
    Then the HTTP response status code is 201
    And the HTTP response content-type contains "application/json"
    And the JSON response contains field "addressType" with value "PHYSICAL"
    And the JSON response contains field "addressLine1" with value "100 Pine Needle Way"
    And the JSON response contains field "addressLine2" with value "Cabin 4"
    And the JSON response contains field "locality" with value "North Bend"
    And the JSON response contains field "administrativeArea" with value "WA"
    And the JSON response contains field "postalCode" with value "98045"
    And the JSON response contains field "countryCode" with value "US"
    And the JSON response path "geom.type" has value "Point"
    And the JSON response coordinate at index 0 is -121.785
    And the JSON response coordinate at index 1 is 47.495
    And the JSON response does not contain field "tenantId"
    And the response field "id" is remembered as "created_address_id"
    When an API admin sends a GET request to "/api/studios/valiant_addr/v1/locations/{addr_location_id}/addresses/{created_address_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "addressLine1" with value "100 Pine Needle Way"
    And the JSON response coordinate at index 0 is -121.785
    And the JSON response coordinate at index 1 is 47.495
    And the JSON response does not contain field "tenantId"

  Scenario: Successfully update address adhering to AIP-134
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_addr_patch",
        "name": "Valiant Address Patch Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_addr_patch/v1/locations" with body:
      """
      {
        "name": "Alpine Retreat"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_addr_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_addr_patch/v1/locations/{patch_addr_loc_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "100 Old Forest Rd",
        "locality": "North Bend",
        "administrativeArea": "WA",
        "postalCode": "98045",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "patch_address_id"
    When an API admin sends a PATCH request to "/api/studios/valiant_addr_patch/v1/locations/{patch_addr_loc_id}/addresses/{patch_address_id}?update_mask=addressLine1,locality" with body:
      """
      {
        "addressLine1": "200 New Forest Rd",
        "locality": "Snoqualmie"
      }
      """
    Then the HTTP response status code is 200
    And the JSON response contains field "addressLine1" with value "200 New Forest Rd"
    And the JSON response contains field "locality" with value "Snoqualmie"

  Scenario: Successfully delete address
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_addr_del",
        "name": "Valiant Address Delete Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_addr_del/v1/locations" with body:
      """
      {
        "name": "Cedar Hall"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "del_addr_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_addr_del/v1/locations/{del_addr_loc_id}/addresses" with body:
      """
      {
        "addressType": "MAILING",
        "addressLine1": "PO Box 500",
        "locality": "Seattle",
        "administrativeArea": "WA",
        "postalCode": "98101",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "delete_address_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_addr_del/v1/locations/{del_addr_loc_id}/addresses/{delete_address_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_addr_del/v1/locations/{del_addr_loc_id}/addresses/{delete_address_id}"
    Then the HTTP response status code is 404

  Scenario: Multiple addresses associated with a single location
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_multi_addr",
        "name": "Valiant Multi Address Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_multi_addr/v1/locations" with body:
      """
      {
        "name": "Headquarters Compound"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "multi_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_multi_addr/v1/locations/{multi_loc_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "500 Main St",
        "locality": "Tacoma",
        "administrativeArea": "WA",
        "postalCode": "98402",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "addr_physical_id"
    When an API admin sends a POST request to "/api/studios/valiant_multi_addr/v1/locations/{multi_loc_id}/addresses" with body:
      """
      {
        "addressType": "MAILING",
        "addressLine1": "PO Box 999",
        "locality": "Tacoma",
        "administrativeArea": "WA",
        "postalCode": "98401",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "addr_mailing_id"
    When an API admin sends a GET request to "/api/studios/valiant_multi_addr/v1/locations/{multi_loc_id}/addresses/{addr_physical_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "addressType" with value "PHYSICAL"
    And the JSON response contains field "addressLine1" with value "500 Main St"
    When an API admin sends a GET request to "/api/studios/valiant_multi_addr/v1/locations/{multi_loc_id}/addresses/{addr_mailing_id}"
    Then the HTTP response status code is 200
    And the JSON response contains field "addressType" with value "MAILING"
    And the JSON response contains field "addressLine1" with value "PO Box 999"

  Scenario: Cross-tenant location and address access returns 404
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "loc_studio_alpha",
        "name": "Location Studio Alpha"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "loc_studio_beta",
        "name": "Location Studio Beta"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/loc_studio_alpha/v1/locations" with body:
      """
      {
        "name": "Alpha Private Sanctuary"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_loc_id"
    When an API admin sends a POST request to "/api/studios/loc_studio_alpha/v1/locations/{alpha_loc_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "1 Alpha Secret Way",
        "locality": "Olympia",
        "administrativeArea": "WA",
        "postalCode": "98501",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "alpha_addr_id"
    When an API admin sends a GET request to "/api/studios/loc_studio_beta/v1/locations/{alpha_loc_id}"
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/loc_studio_beta/v1/locations/{alpha_loc_id}/addresses/{alpha_addr_id}"
    Then the HTTP response status code is 404

  Scenario: Soft-deleting location cascades access denial to child addresses
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_cascade_del",
        "name": "Valiant Cascade Delete Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_cascade_del/v1/locations" with body:
      """
      {
        "name": "Compound to Sunset"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "sunset_loc_id"
    When an API admin sends a POST request to "/api/studios/valiant_cascade_del/v1/locations/{sunset_loc_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "99 Sunset Boulevard",
        "locality": "Bremerton",
        "administrativeArea": "WA",
        "postalCode": "98310",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "sunset_addr_id"
    When an API admin sends a DELETE request to "/api/studios/valiant_cascade_del/v1/locations/{sunset_loc_id}"
    Then the HTTP response status code is 204
    When an API admin sends a GET request to "/api/studios/valiant_cascade_del/v1/locations/{sunset_loc_id}/addresses/{sunset_addr_id}"
    Then the HTTP response status code is 404

  Scenario: Validation error handling for locations and addresses
    Given the admin HTTP server is running with database migrations applied
    When an API admin sends a POST request to "/api/admin/v1/studios" with body:
      """
      {
        "alias": "valiant_loc_validation",
        "name": "Valiant Location Validation Studio"
      }
      """
    Then the HTTP response status code is 201
    When an API admin sends a POST request to "/api/studios/valiant_loc_validation/v1/locations" with body:
      """
      {
        "name": ""
      }
      """
    Then the HTTP response status code is 400
    When an API admin sends a POST request to "/api/studios/valiant_loc_validation/v1/locations" with body:
      """
      {
        "name": "Valid Location"
      }
      """
    Then the HTTP response status code is 201
    And the response field "id" is remembered as "valid_loc_for_val_id"
    When an API admin sends a POST request to "/api/studios/valiant_loc_validation/v1/locations/{valid_loc_for_val_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "",
        "locality": "Bellevue",
        "administrativeArea": "WA",
        "postalCode": "98004",
        "countryCode": "US"
      }
      """
    Then the HTTP response status code is 400
    When an API admin sends a POST request to "/api/studios/valiant_loc_validation/v1/locations/{valid_loc_for_val_id}/addresses" with body:
      """
      {
        "addressType": "PHYSICAL",
        "addressLine1": "100 Bellevue Way",
        "locality": "Bellevue",
        "administrativeArea": "WA",
        "postalCode": "98004",
        "countryCode": "USA"
      }
      """
    Then the HTTP response status code is 400
    When an API admin sends a GET request to "/api/studios/valiant_loc_validation/v1/locations/not-a-uuid"
    Then the HTTP response status code is 404
    When an API admin sends a GET request to "/api/studios/valiant_loc_validation/v1/locations/{valid_loc_for_val_id}/addresses/not-a-uuid"
    Then the HTTP response status code is 404
