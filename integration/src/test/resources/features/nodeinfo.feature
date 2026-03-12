Feature: Nodeinfo

  Scenario: Well-known nodeinfo endpoint returns JRD
    Given the server is configured
    When I start the server
    Then I should receive a response from "http://localhost:8080/.well-known/nodeinfo" containing:
      """
      {
        "links": [
          {
            "rel": "http://nodeinfo.diaspora.software/ns/schema/2.2",
            "href": "http://localhost:8080/admin/nodeinfo"
          }
        ]
      }
      """

  Scenario: Admin nodeinfo endpoint returns NodeInfo 2.2 document
    Given the server is configured
    When I start the server
    Then I should receive a response from "http://localhost:8080/admin/nodeinfo" containing:
      """
      {
        "version": "2.2",
        "software": {
          "name": "larpconnect",
          "version": "0.0.1",
          "repository": "https://github.com/larpconnect/larpconnect",
          "homepage": "https://github.com/larpconnect/larpconnect"
        },
        "protocols": ["activitypub"],
        "openRegistrations": false
      }
      """
