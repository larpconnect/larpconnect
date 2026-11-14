## Context

In **Njall**, tenanted resources in the **User plane** follow a Common Table Inheritance (CTI) hierarchy rooted at `njall_users.entities`, governed by the currently in-force architectural decision set:
- [0005: Sealed DAO and Hibernate Dual Session Architecture](../../../../adr/0005-sealed-dao-and-hibernate-dual-session-architecture.md)
- [0011: ArchUnit Injected Constructor Visibility and Package Dependency Rules](../../../../adr/0011-archunit-injected-constructor-visibility-and-package-dependency-rules.md)
- [0021: ArchUnit Record Factory Prohibition and Pure Data Carriers](../../../../adr/0021-archunit-record-factory-prohibition-and-pure-data-carriers.md)
- [0022: ArchUnit Immutable Records and ErrorProne Enforcement](../../../../adr/0022-archunit-immutable-records-and-errorprone-enforcement.md)
- [0023: Record Component Nonnull Invariant and Boundary Normalization](../../../../adr/0023-record-component-nonnull-invariant-and-boundary-normalization.md)
- [0025: User Space Studios API and Tenant Row-Level Security](../../../../adr/0025-user-space-studios-api-and-tenant-row-level-security.md)
- [0026: Caffeine Cached Studio Lookup Service](../../../../adr/0026-caffeine-cached-studio-lookup-service.md)
- [0027: Common Table Inheritance and Tenanted Links Architecture](../../../../adr/0027-common-table-inheritance-and-tenanted-links-architecture.md)
- [0030: DAO Naming Conventions and Entity Immutability Standards](../../../../adr/0030-dao-naming-conventions-and-entity-immutability-standards.md)
- [0032: Package Size Limits and Subpackage Decomposition](../../../../adr/0032-package-size-limits-and-subpackage-decomposition.md)
- [0033: Federated Hashtags and Case-Insensitive Functional Indexing](../../../../adr/0033-federated-hashtags-and-case-insensitive-functional-indexing.md)
- [0035: Tenanted Events and Location Associations Architecture](../../../../adr/0035-tenanted-events-and-location-associations-architecture.md)
- [0037: Tenanted Collections Architecture](../../../../adr/0037-tenanted-collections-architecture.md)
- [0038: Tenanted Actors and Federated Endpoints Architecture](../../../../adr/0038-tenanted-actors-and-federated-endpoints-architecture.md)
- [0040: Actor-Parented Collections Routes and Entity Generator Column](../../../../adr/0040-actor-parented-collections-and-entity-generator-column.md)

**Studios** already host general scheduled **Events** (`njall_users.events`), **Locations**, **Individual** records, **Collections**, **Hashtag** tags, **Link** entries, and **Actor** identities, but lack first-class domain models and endpoints for roleplaying rulesets (**System**), multi-session chronicles (**Campaigns**), and runnable game sessions (**Games**). Adding these three entity types introduces four concrete architectural constraints:
1. **2-Level CTI Parity (`1:1` Game-to-Event)**: Every **Games** record is a specialized **Events** record with identical scheduling and venue semantics (`title`, `summary`, `location_id`, `start_time`, `end_time`) plus a required **System** and an optional **Campaigns** association, while general **Events** gain an `event_type` classification (`EVENT_TYPE_UNSPECIFIED`, `GAME`, `WORKSHOP`, `CONFERENCE`, `MEETUP`).
2. **AIP-124 Many-to-One / Optional Association Pathing**: One **System** has many **Games** and **Campaigns**, and one **Campaigns** record has zero-or-many **Games** (while a game does not require a campaign, and `game.system` is independent of `campaign.system`), requiring flat peer collections with canonical AIP-122 resource URIs rather than hierarchical sub-paths that would duplicate or orphan game URLs.
3. **20-Type Package Cap vs Unnamed-Module `sealed` Interfaces**: `com.larpconnect.njall.data.domain` currently holds exactly 20 counted non-enum types (the maximum allowed by `ADR-0032`), and because `:data` compiles in an unnamed Java module (no `module-info.java`), `sealed interface DatabaseObject` cannot permit records across subpackages.
4. **Checkstyle 8-Parameter Limit & Upward Dependency Prohibition**: `StudioSubRoutes` currently injects 6 subroutes (adding 3 more directly would hit 9 parameters, violating `ADR-0030`), `GameDAO` create/patch operations span 9+ inputs, and `DefaultGameDAO` needs access to `EventEntity` without introducing an upward package dependency (`ADR-0011`).

## Goals / Non-Goals

**Goals:**
- Provision `njall_users.tevent_type`, `njall_users.events.event_type`, `njall_users.systems`, `njall_users.campaigns`, and `njall_users.games` in Flyway migration `V14__systems_campaigns_and_games.sql` with composite tenant foreign keys, functional indices, and PostgreSQL Row-Level Security (`app.tenant_id`).
- Expose flat AIP-124 peer collections `/api/studios/{studio-id}/v1/systems`, `/v1/campaigns`, and `/v1/games`, and update `/v1/events` so non-game **Events** (`entity_type = 'Event'`) and **Games** (`entity_type = 'Game'`) maintain strictly disjoint canonical URIs.
- Standardize AIP-122 canonical studio resource URI formatting and bare-UUID-or-URI input parsing across `generator`, `system`, and `campaign` references via a shared `StudioResourceUriSupport` utility.
- Decompose `com.larpconnect.njall.data.domain` into `common`, `admin`, `servers`, and `studios` subpackages (unsealing `DatabaseObject`), move `EventDAO`/`EventEntity` to `com.larpconnect.njall.data.dao.studios.events`, and aggregate studio HTTP routes via a `@StudioSubRoute` Guice multibinder so every package and constructor satisfies ArchUnit and Checkstyle gates.
- Hydrate active **System**, **Campaigns**, and **Games** display names in expanded **Collections** queries (`?form=expanded`).

**Non-Goals:**
- Enforcing database or application constraints requiring `game.system` to match `campaign.system` (they are intentionally independent).
- Changing studio-level **Actor** `generator` provenance rules established in `ADR-0040` (all three new endpoints remain unparented under `/api/studios/{studio-id}/v1/` and require `generator` in `POST` payloads).
- Implementing AMQP federation fan-out or **FeatherPub** inbox/outbox delivery for **System**, **Campaigns**, or **Games** activities in this change.

## Architectural Diagrams (Lightweight C4-Inspired)

### Container View (`:api` / `:data` Runtime & PostgreSQL 2-Level CTI Schema)

```text
+-----------------------------------------------------------------------------+
| [Client]                                                                    |
+--------------------------------------+--------------------------------------+
                                       | HTTPS JSON
                                       | /api/studios/{studio-id}/v1/
                                       |   {systems | campaigns | events | games}
                                       v
+-----------------------------------------------------------------------------+
| Container: Njall Server (:server + :api + :data, Java 25 / Pekko HTTP)      |
|                                                                             |
|  [User Plane Studio Routes & Pekko Typed Actors]                            |
|                        |                                                    |
|                        v                                                    |
|  [Data Plane Tenanted DAOs (@NjallUsers SessionFactory)]                    |
+--------------------------------------+--------------------------------------+
                                       | JDBC / Hibernate
                                       | set_config('app.tenant_id', ..., true)
                                       v
+-----------------------------------------------------------------------------+
| Container: PostgreSQL 18 (Schema: njall_users, RLS Enabled)                 |
|                                                                             |
|                     +----------------------------------+                    |
|                     | entities                         |                    |
|                     | PK (tenant_id, id)               |                    |
|                     | generator UUID NOT NULL (Actor)  |                    |
|                     | entity_type ('System'|'Campaign' |                    |
|                     |              |'Event'|'Game'...) |                    |
|                     +----+------------+-----------+----+                    |
|                          |            |           |                         |
|         +----------------+            |           +----------------+        |
|         v                             v                            v        |
|  +-----------------+        +-------------------+        +----------------+ |
|  | systems         |        | campaigns         |        | events         | |
|  | PK(tenant_id,id)|< - - - | PK (tenant_id,id) |        | PK(tenant_id,  | |
|  | UNIQUE(tenant,  | opt FK | system_id NULL    |        |    id)         | |
|  |   lower(name))  |        | title NOT NULL    |        | event_type     | |
|  +--------+--------+        +---------+---------+        | location_id    | |
|           ^                           ^                  +--------+-------+ |
|           | req FK                    | opt FK                    ^         |
|           | (system_id NOT NULL)      | (ON DELETE SET NULL)      | 1:1 CTI |
|           +--------------------+------+---------------------------+         |
|                                |                                            |
|                      +---------+---------+                                  |
|                      | games             |                                  |
|                      | PK (tenant_id,id) |                                  |
|                      | system_id         |                                  |
|                      | campaign_id NULL  |                                  |
|                      +-------------------+                                  |
+-----------------------------------------------------------------------------+
```

### Component View (`:api` Multibound Routes & `:data` Subpackage Topology)

```text
+-----------------------------------------------------------------------------+
| Component Boundary: com.larpconnect.njall.api.studios                       |
|                                                                             |
|  [StudiosRoute] ---> [StudioSubRoutes (@StudioSubRoute Set<RouteProvider>)] |
|                            |        |          |          |                 |
|                            v        v          v          v                 |
|                     [SystemsRoute] [Campaigns [Events   [GamesRoute]        |
|                            |        Route]     Route]         |             |
|                            |          |          |            | Pekko Ask   |
|                            v          v          v            v             |
|                     [GameSystem   [Campaign  [Event     [GameActor]         |
|                      Actor]        Actor]     Actor]          |             |
|                        \              \         /            /              |
|                         +-----> [StudioResourceUriSupport] <-+              |
+---------------------------------------+-------------------------------------+
                                        | Invokes Tenanted DAOs
                                        v
+-----------------------------------------------------------------------------+
| Component Boundary: com.larpconnect.njall.data                              |
|                                                                             |
|  dao.studios.systems    dao.studios.campaigns    dao.studios.events         |
|  [DefaultGameSystemDAO] [DefaultCampaignDAO]     [DefaultEventDAO]          |
|                                                  [EventEntity (public)]     |
|                                                           ^                 |
|  dao.studios.games                                        | sibling dep     |
|  [DefaultGameDAO] ---> persists EntityBaseEntity + EventEntity + GameEntity |
|                                                                             |
|  domain.common            domain.studios                                    |
|  [DatabaseObject] <------ [EventType, Event, GameSystem, Campaign, Game...] |
+-----------------------------------------------------------------------------+
```

- **Boundaries**: `:api` studio subpackages (`systems`, `campaigns`, `events`, `games`) handle HTTP routing, AIP-122 URI normalization, AIP-134 `update_mask` selection, and Pekko Typed actor command dispatch on the `@Blocking` dispatcher; `:data` subpackages manage Hibernate `@NjallUsers` transactions, `app.tenant_id` Row-Level Security context, and `ActorGeneratorValidator` checks.
- **Responsibilities**: `DefaultEventDAO` operates strictly on `entity_type = 'Event'` rows (rejecting `EventType.GAME`), while `DefaultGameDAO` atomically persists `EntityBaseEntity` (`entity_type = 'Game'`), `EventEntity` (`event_type = EventType.GAME`), and `GameEntity` (`system_id`, `campaign_id`).
- **Key Relationships**: Every cross-package dependency flows strictly downward or outward (`studios.games` -> `studios.events` -> `dao.common` -> `domain.studios` -> `domain.common`), satisfying `ArchitectureTest.notDependOnAncestorPackages` (`ADR-0011`) and the 20-type package cap (`ADR-0032`).
- **Assumptions**: Java classes representing **System** entities use the **GameSystem** prefix (`GameSystem`, `GameSystemDAO`, `GameSystemEntity`, `GameSystemActor`) per `glossary/technical.md` to avoid colliding with `java.lang.System`, while HTTP routes and JSON payloads use `systems` and `system`.
- **Open Questions**: None blocking implementation; in-force ADRs `0005`, `0032`, and `0035` are flagged below for formal amendment in the `adr` artifact step.

## Decisions

### 1. Two-Level Common Table Inheritance (`games` -> `events` -> `entities`) and `tevent_type`
- **Choice**: Create PostgreSQL enum `njall_users.tevent_type` (`'EVENT_TYPE_UNSPECIFIED'`, `'GAME'`, `'WORKSHOP'`, `'CONFERENCE'`, `'MEETUP'`), add `event_type njall_users.tevent_type NOT NULL DEFAULT 'EVENT_TYPE_UNSPECIFIED'` to `njall_users.events`, create 1st-level CTI tables `njall_users.systems` (with functional unique index `idx_systems_tenant_name ON (tenant_id, lower(name))`) and `njall_users.campaigns` (with nullable `system_id` `ON DELETE SET NULL`), and create 2nd-level CTI table `njall_users.games` whose `(tenant_id, id)` primary key references `njall_users.events(tenant_id, id) ON DELETE CASCADE` alongside required `system_id` and optional `campaign_id` (`ON DELETE SET NULL`).
- **Rationale**: A game has strict `1:1` parity with an event (`title`, `summary`, `location_id`, `start_time`, `end_time`, `chk_events_time_order`) while automatically inheriting CTI root capabilities (**Hashtag** tagging, reactions, **Collections** membership, and `CollectionItemHelper` event-title hydration via `njall_users.events`). Keeping `game.system_id` independent of `campaign.system_id` supports systemless anthology campaigns and cross-system guest sessions without schema rigidity. Case-insensitive uniqueness on `(tenant_id, lower(name))` prevents duplicate system catalogs within a studio (`409 Conflict`, matching `ADR-0033` hashtag indexing).
- **Alternatives Considered**:
  - Adding nullable `system_id` and `campaign_id` columns directly onto `njall_users.events` without `njall_users.games`. Rejected because non-game workshops/meetups would carry inapplicable columns and `system_id NOT NULL` could not be enforced at the table schema level without brittle conditional check constraints.
  - Making `njall_users.games` inherit directly from `njall_users.entities` and duplicating `location_id`, `title`, `start_time`, and `end_time`. Rejected because it duplicates temporal constraints, venue foreign keys, and event schedule queries.

### 2. Flat AIP-124 Peer Studio Collections, Disjoint `/v1/events` vs `/v1/games`, and AIP-122 Canonical Resource URIs
- **Choice**: Mount `/v1/systems`, `/v1/campaigns`, and `/v1/games` as flat peer collections under `/api/studios/{studio-id}/`. Scope `/v1/events` strictly to `entity_type = 'Event'` (rejecting `eventType = "GAME"` on `POST`/`PATCH` with `400 Bad Request` and returning `404 Not Found` on game UUIDs), and scope `/v1/games` strictly to `entity_type = 'Game'` (returning output-only `eventType = "GAME"`). Introduce `StudioResourceUriSupport` in `com.larpconnect.njall.api.studios.common` (with `GeneratorUriSupport` delegating to it) so `generator`, `system`, and `campaign` fields emit canonical studio URIs (`/api/studios/{studio-id}/v1/<collection>/{id}`) and accept either a bare UUID or same-studio canonical URI on input (including `?systemId` and `?campaignId` list filters). When an optional referenced `system` (on a campaign) or `campaign` (on a game) has been soft-deleted, read responses emit `null` rather than a dead URI.
- **Rationale**: Follows Google AIP-124 for many-to-one and optional associations so every **Games** and **Campaigns** resource has exactly one stable canonical **FeatherPub** URI even when `campaign` is omitted or reassigned via `PATCH`. Keeping `/v1/events` and `/v1/games` disjoint prevents dual-URI ambiguity where the same UUID resolves at two different endpoints with different schemas.
- **Alternatives Considered**:
  - Nesting games under `/campaigns/{campaign-id}/games` or `/systems/{system-id}/games`. Rejected because standalone one-shot games have no campaign, and moving a game between campaigns or systems would break its canonical URI.
  - Allowing `/v1/events` to return `Game` rows minus `system` and `campaign`. Rejected because mutating or reading a game through `/v1/events` would strip subtype invariants and create two competing resource URLs for one entity.

### 3. Unsealing `DatabaseObject` and Decomposing `data.domain` and `data.dao.studios`
- **Choice**: Unseal `DatabaseObject` and relocate all types out of `com.larpconnect.njall.data.domain` into four sibling subpackages—`common` (`DatabaseObject`, `DeletionFilter`, `GeoJsonPoint`), `admin` (`AdminUser`, `AdminRole`, `AdminUserStatus`, `DefaultStudioRole`, `RoleType`, `StudioLookup`), `servers` (`Server`, `ServerContact`, `ContactType`), and `studios` (17 counted records plus enums `AddressType` and `EventType`)—leaving zero classes in the ancestor package. Relocate `EventDAO`, `DefaultEventDAO`, and `public class EventEntity` from `com.larpconnect.njall.data.dao.studios` into `com.larpconnect.njall.data.dao.studios.events` (`EventsDaoModule`), alongside new subpackages `systems` (`SystemsDaoModule`), `campaigns` (`CampaignsDaoModule`), and `games` (`GamesDaoModule`).
- **Rationale**: In Java's unnamed module, a `sealed` interface cannot permit implementations in other packages, and `com.larpconnect.njall.data.domain` is already at 20/20 non-enum types (`ADR-0032`). Mirroring the `DAO` unsealing from `ADR-0032` and emptying the ancestor `data.domain` package guarantees every subpackage stays well below 20 types (max 17 in `domain.studios`) with zero upward dependencies (`ADR-0011`). Moving `EventEntity` into `studios.events` as a `public` CTI parent entity (matching `EntityBaseEntity` in `dao.common`) lets `DefaultGameDAO` in `studios.games` persist `EventEntity` via a legal sibling dependency rather than an illegal upward dependency on `data.dao.studios`.
- **Alternatives Considered**:
  - Introducing `module-info.java` (JPMS) across `:common`, `:data`, `:api`, `:server`, and `:integration` just to keep `DatabaseObject` sealed across subpackages. Rejected due to split-package friction across third-party libraries and disproportionate build complexity after `DAO` was already unsealed in `ADR-0032`.
  - Leaving `DatabaseObject` in `com.larpconnect.njall.data.domain` while putting records in `data.domain.studios`. Rejected because `domain.studios` implementing `domain.DatabaseObject` would immediately fail `ArchitectureTest.notDependOnAncestorPackages`.

### 4. `@StudioSubRoute` Multibinder and Parameter Records for Checkstyle 8-Parameter Compliance
- **Choice**: Add `@BindingAnnotation @StudioSubRoute` in `com.larpconnect.njall.api.studios.common`. Have each top-level studio subroute module (`LocationsModule`, `LinksModule`, `TagsModule`, `EventsModule`, `IndividualsModule`, `ActorsModule`, `SystemsModule`, `CampaignsModule`, `GamesModule`) bind its `RouteProvider` into `Multibinder.newSetBinder(binder(), RouteProvider.class, StudioSubRoute.class)`, and refactor `StudioSubRoutes` to inject `@StudioSubRoute Set<RouteProvider>` (1 constructor parameter). Define `@Immutable` parameter records `CreateGameParams`, `PatchGameParams`, and `PatchCampaignParams` in `:data` for multi-field `DAO` create/patch methods and explicit nullable-field clearing under AIP-134 `update_mask`.
- **Rationale**: Checkstyle enforces a strict 8-parameter maximum on non-record constructors and methods (`ADR-0030`), whereas Java `record` classes are exempt. Multibinding `@StudioSubRoute` scales cleanly to any number of future studio resource packages without modifying `StudioSubRoutes` again and matches the root `RouteProvider` multibinder pattern in `:api`. Parameter records also express `clearSystem` / `clearCampaign` / `clearLocationId` / `clearSummary` / `clearStartTime` / `clearEndTime` flags cleanly without ambiguous nested `Optional` parameters.
- **Alternatives Considered**:
  - Grouping routes into intermediate wrapper classes (`StudioScheduleSubRoutes`, `StudioContentSubRoutes`). Rejected as artificial boilerplate compared to native Guice `Multibinder`.

## Risks / Trade-offs

- **[Risk]** Relocating `com.larpconnect.njall.data.domain.*` and `EventDAO` touches imports across `:data`, `:api`, `:server`, and `:integration` -> **Mitigation**: Execute the domain and `studios.events` package decomposition as a sequential prerequisite task slice (`[SEQ]`) verified with `./gradlew testClasses check` before parallel vertical slices begin.
- **[Risk]** Soft-deleting a **System** or **Campaigns** entity does not fire PostgreSQL `ON DELETE SET NULL` (because soft deletion sets `entities.deleted_on` rather than issuing SQL `DELETE`), which could otherwise leave active campaigns or games advertising URIs to soft-deleted entities -> **Mitigation**: On read/list in `DefaultCampaignDAO` and `DefaultGameDAO`, check whether an optional referenced `system_id` or `campaign_id` is active (`deleted_on IS NULL`), masking soft-deleted optional references as `Optional.empty()` (`null` in JSON); for required `games.system_id`, keep the historical UUID on existing games while rejecting new or patched associations to soft-deleted systems (`400 Bad Request`).
- **[Risk]** Concurrent `POST` or `PATCH` requests with case-variant system names in the same tenant hit `idx_systems_tenant_name` inside PostgreSQL -> **Mitigation**: Catch Hibernate `ConstraintViolationException` (in addition to transactional pre-checks) in `GameSystemActor` and map unique-index violations on `idx_systems_tenant_name` deterministically to `409 Conflict` (`GameSystemActorResponse.Conflict`).

## Migration Plan

1. **Database Schema (`V14`)**: Apply additive Flyway migration `V14__systems_campaigns_and_games.sql`. Existing rows in `njall_users.events` automatically receive `event_type = 'EVENT_TYPE_UNSPECIFIED'` with `entity_type = 'Event'`, requiring zero data backfill.
2. **Persistence & Domain Refactor (`:data`, `:server`, `:api` imports)**: Unseal and decompose `data.domain` into `common`, `admin`, `servers`, and `studios`; move `EventDAO` / `EventEntity` to `data.dao.studios.events`; add `systems`, `campaigns`, and `games` DAO subpackages and update `CollectionItemHelper.EXPANDED_ITEMS_QUERY`.
3. **HTTP Contract & Routes (`:api`)**: Introduce `@StudioSubRoute` and `StudioResourceUriSupport`, update `openapi.yaml` and `/v1/events`, and add `/v1/systems`, `/v1/campaigns`, and `/v1/games` Pekko actors and routes.
4. **Rollback**: Because `V14` only adds new tables and a defaulted `event_type` column on `njall_users.events`, rolling back application binaries prior to `V14` consumption remains safe on existing non-game event rows; full schema rollback in non-production environments drops `games`, `campaigns`, `systems`, `events.event_type`, and `tevent_type`.

## Open Questions

- None blocking design or task breakdown. In the `adr` step immediately following this design, a new durable ADR (`adr/0041-tenanted-systems-campaigns-and-games-architecture.md`) will record these decisions and formally amend in-force ADRs `0005` (unsealing `DatabaseObject`), `0032` (`data.domain` and `studios` subpackage decomposition + `@StudioSubRoute` multibinding), and `0035` (`events.event_type` classification and disjoint `/v1/events` vs `/v1/games` scoping).
