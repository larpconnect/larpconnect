# Njall Business Glossary

| Term | Definition | Use When | Avoid |
| --- | --- | --- | --- |
| Njall | The internal name of the larpconnect application | Referring to the application in internal documents or source code | In external documents that are designed for the general public -- People who may be interfacing with but not installing the software.  In those cases, use "LarpConnect" |
| LarpConnect | The public name of the njall application | Referring to the application in external documents that are designed for the general public -- people who are not installing, modifying, or developing the software | In internal documents use "njall" |
| Server | The host application for the system. One server may have a large number of studios, users, etc as primary members (in the default tenant), as transient members (members on other servers who do not have a local account and that are not owned locally), or as hosted members (who are members of a tenant)  | Referring to the backend/API/DB layer. | When referring to tenants' services |
| Studio | A tenant running on the njall backend | | |
| Hosted | A tenant studio or a user who has a strong internal identity and cleanly portable separation | | |
| Transient | A studio or user who exists on a different server or with a different tenant | | |
| Primary | A studio or user who exists in the **default** tenant on a given server | | |
| FeatherPub | An ActivityPub inspired protocol that is being developed out of Njall | Talking about the protocol that njall uses to talk to other serviers or clients | Speaking generally about federated protocols that may or may not be compatible (e.g., ActivityPub) |
| Client | Any frontend application that interacts with the njall server via the public API | | |
| Admin User | A user who can administer the **server** | When discussing users with superuser privileges over the server architecture itself | When discussing roles that may be assigned in individual studios |
| Studio Admin | A user who can administer a **studio** | When discussing users with superuser privileges over a single studio | When discussing users with superuser privileges over the server architecture itself |
| Role | A collection of permissions or a title that may be given to a user | | |
| Alias | A term for any entity (studio, user, or object) that is plain text and easy to read and is not a synthetic ID | | |
| ID | The unique identifier for any entity (studio, user, or object).  It is a long string of random characters and is not meant to be human-readable | | |
| Entity | Any object or reference that may be federated to another server in some way | Any local objects | Objects that are server-specific and not meant to be federated |
| Link | An entity that references something external | | |
| Events | An entity representing a specific gathering of people. Usually in the real world. | | |
| Hashtag | A collective grouping of related entities given a specific searchable name. A kind of link | | |
| Reaction | A user's textless response to an entity (e.g., a like, dislike, or a heart emoji) | | |
| Relationship | A specified kind of connection between entities, e.g., a friendship or a romance. Represented as an entity. | | |
| Individual | A person who may not be a member or even have an account | | |
| User | An individual with either a local or remote account | | |
| Actor | An entity that can send and receive federated messages (e.g., users, studios, groups) | | |
| Collections | An entity that can hold references to an ordered set of other entities | | |
| Locations | A place in the physical world | | |
| System | A specific style of meeting or game, e.g. Vampire: The Masquerade | | |
| Campaigns | A series of events run under the same system (or lack of system) | | |
| Games | An event run under a campaign. | | When discussing events unconnected to campaigns. |
| Tenant | A studio when its data is kept separated in some manner from other studios | | | 
| allowlist | A list of approved or permitted items | Referring to a list of allowed origins, domains, or other identifiers. Use in favor of "whitelist" | |
| blocklist | A list of denied or disallowed items | Referring to a list of denied origins, domains, or other identifiers. Use in favor of "blacklist" | |