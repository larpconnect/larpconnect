## ADDED Requirements

### Requirement: Base Database Object and DAO Abstractions
The system SHALL provide an unsealed `DatabaseObject` interface (`UUID id()`) in `com.larpconnect.njall.data.domain.common` and an unsealed `DAO<T extends DatabaseObject>` interface (`findById(UUID)`, `list()`) in `com.larpconnect.njall.data.dao.common` extended by all concrete **DAO** interfaces across `:data`.

#### Scenario: Query database object by primary key
- **GIVEN** a persisted entity with a known UUID identifier in the database
- **WHEN** a client invokes `findById(UUID id)` on a **DAO** supporting global lookup
- **THEN** the system returns an `Optional` containing the populated domain record

#### Scenario: Query all database objects
- **GIVEN** one or more persisted entities present in the database
- **WHEN** a client invokes `list()` on a **DAO** supporting global listing
- **THEN** the system returns an `ImmutableList` containing all corresponding domain records

### Requirement: Domain Subpackage Hierarchy and Immutable Domain Records
The system SHALL partition `@Immutable` domain records and enums across `com.larpconnect.njall.data.domain.{common,admin,servers,studios}`, including `EventType`, `Event`, **GameSystem**, `Campaign`, and `Game` in `studios` and `AdminUser`, `AdminRole`, and `StudioLookup` (`id()` returning `studioId()`) in `admin`.

#### Scenario: Domain records across subpackages implement DatabaseObject
- **GIVEN** `AdminUser`, `StudioLookup`, `Event`, `GameSystem`, `Campaign`, and `Game` record instances
- **WHEN** checked against `DatabaseObject`
- **THEN** each record is an instance of `DatabaseObject` and returns a non-null UUID from `id()`

#### Scenario: StudioLookup exposes tenantId and studioId
- **GIVEN** a `StudioLookup` record created from persistent data
- **WHEN** methods `tenantId()` and `studioId()` are invoked
- **THEN** both return valid non-null UUIDs, and `id()` equals `studioId()`

### Requirement: Tenanted System and Campaign Persistence
The system SHALL provide `@NjallUsers` tenanted `GameSystemDAO` (`studios.systems`, `entity_type = 'System'`, ordered by `lower(name) ASC, created_on DESC`) and `CampaignDAO` (`studios.campaigns`, `entity_type = 'Campaign'`, validating active `'System'` references, masking soft-deleted systems on read, and supporting `?systemId` filtering and `PatchCampaignParams` clearing), plus `System` and `Campaign` joins in `CollectionItemHelper.EXPANDED_ITEMS_QUERY`.

#### Scenario: Expanded collection query hydrates System, Campaign, and Game display names
- **GIVEN** an active collection containing an active **System** named "VtM", an active **Campaigns** record titled "Seattle By Night", and an active **Games** record titled "Court Session 1"
- **WHEN** `CollectionDAO.findExpandedById` executes
- **THEN** the returned `ExpandedEntity` list contains items with `(type="System", name="VtM")`, `(type="Campaign", name="Seattle By Night")`, and `(type="Game", name="Court Session 1")`

### Requirement: Tenanted Event Type Scoping and Two-Level CTI Game Persistence
`EventDAO` (`studios.events`, public `EventEntity`) SHALL map `EventType`, scope reads/mutations to `entity_type = 'Event'`, and reject `EventType.GAME`. `GameDAO` (`studios.games`, using `CreateGameParams`/`PatchGameParams`) SHALL atomically persist `EntityBaseEntity` (`'Game'`), `EventEntity` (`EventType.GAME`), and `GameEntity`, validating active `'System'` and optional `'Campaign'`/`'Location'` independently of `campaign.systemId()`.

#### Scenario: GameDAO persists 2nd-level CTI game invisible to EventDAO
- **GIVEN** an active **Studio** **Tenant** `tenantId`, an active **Actor** `actorId`, and an active **System** `systemId`
- **WHEN** `GameDAO.create` persists a new `Game` with `title = "Midnight Masquerade"`
- **THEN** `GameDAO.findById(tenantId, game.id())` returns the `Game` with `eventType() == EventType.GAME` and `systemId().equals(systemId)`
- **AND** `EventDAO.findById(tenantId, game.id())` returns `Optional.empty()` because `entity_type` is `'Game'` rather than `'Event'`

## REMOVED Requirements

### Requirement: Sealed Database Object and Base DAO Abstraction
**Reason**: Java sealed interfaces in an unnamed module require permitted subtypes in the same package, conflicting with the 20-type package cap (`ADR-0032`).

**Migration**: Replaced by `Base Database Object and DAO Abstractions`.

### Requirement: Admin Domain Records and Sealed DatabaseObject Hierarchy
**Reason**: `DatabaseObject` is unsealed and domain records are decomposed across `common`, `admin`, `servers`, and `studios` subpackages.

**Migration**: Replaced by `Domain Subpackage Hierarchy and Immutable Domain Records`.
