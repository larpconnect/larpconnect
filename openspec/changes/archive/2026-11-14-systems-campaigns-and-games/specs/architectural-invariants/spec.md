## ADDED Requirements

### Requirement: Domain Subpackage Structure and Upward Dependency Invariant
The system SHALL organize domain types under `com.larpconnect.njall.data.domain` into `@NullMarked` sibling subpackages (`common`, `admin`, `servers`, `studios`) with zero classes in `com.larpconnect.njall.data.domain` and zero upward dependencies from subpackages.

#### Scenario: ArchUnit verifies zero upward dependencies and compliant type counts in data.domain subpackages
- **GIVEN** compiled non-test classes in `com.larpconnect.njall.data.domain..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes reside in `com.larpconnect.njall.data.domain`
- **AND** each subpackage (`common`, `admin`, `servers`, `studios`) contains at most 20 counted types and declares `@NullMarked` in `package-info.java`

## MODIFIED Requirements

### Requirement: Studios Subpackage Structure and Route Aggregation
The system SHALL organize studio **User plane** components under `com.larpconnect.njall.api.studios` into domain-focused subpackages: `common`, `links`, `locations`, `addresses`, `tags`, `events`, `individuals`, `collections`, `actors`, `systems`, `campaigns`, and `games`, with zero upward dependencies on `com.larpconnect.njall.api.studios`. Top-level studio routes (`LocationsRoute`, `LinksRoute`, `TagsRoute`, `EventsRoute`, `IndividualsRoute`, `AgentRoute`, `SystemsRoute`, `CampaignsRoute`, `GamesRoute`) SHALL bind into `@StudioSubRoute Set<RouteProvider>` injected by `StudioSubRoutes` and aggregated by `StudiosRoute`, while `StudiosModule` installs each direct subpackage Guice **Module** one layer down.

#### Scenario: ArchUnit verifies zero upward dependencies from studios subpackages
- **GIVEN** compiled non-test classes in `com.larpconnect.njall.api.studios.*`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes in subpackages depend on any class residing in ancestor package `com.larpconnect.njall.api.studios`

#### Scenario: StudiosModule installs direct subpackage modules
- **GIVEN** the `StudiosModule` in package `com.larpconnect.njall.api.studios`
- **WHEN** Guice injector initialization executes
- **THEN** `StudiosModule` installs `LinksModule`, `AddressesModule`, `LocationsModule`, `TagsModule`, `EventsModule`, `IndividualsModule`, `CollectionsModule`, `ActorsModule`, `SystemsModule`, `CampaignsModule`, and `GamesModule` one level down
- **AND** `StudioSubRoutes` resolves all 9 top-level studio `RouteProvider` bindings via `@StudioSubRoute Set<RouteProvider>`

### Requirement: DAO Subpackage Structure and Persistence Composition
The system SHALL organize data access components under `com.larpconnect.njall.data.dao` into domain-focused subpackages: `common`, `studios` (with child subpackages `individuals`, `collections`, `actors`, `events`, `systems`, `campaigns`, and `games`), `admin`, and `servers`, with zero upward dependencies on `com.larpconnect.njall.data.dao` or `com.larpconnect.njall.data.dao.studios`. Base entity and **DAO** types SHALL reside in `dao.common`, shared `EventEntity` SHALL reside in `studios.events`, `DaoModule` SHALL install `DaoCommonModule`, `StudiosDaoModule`, `AdminDaoModule`, and `ServersDaoModule`, and `StudiosDaoModule` SHALL install direct studio subpackage **DAO** modules one layer down.

#### Scenario: ArchUnit verifies zero upward dependencies from DAO subpackages
- **GIVEN** compiled non-test classes in `com.larpconnect.njall.data.dao..`
- **WHEN** the ArchUnit architecture test suite in `:integration` executes
- **THEN** zero classes in subpackages depend on any class residing in an ancestor package (`com.larpconnect.njall.data.dao` or `com.larpconnect.njall.data.dao.studios`)

#### Scenario: DaoModule installs direct subpackage DAO modules
- **GIVEN** `DaoModule` in `com.larpconnect.njall.data.dao` and `StudiosDaoModule` in `com.larpconnect.njall.data.dao.studios`
- **WHEN** Guice injector initialization executes
- **THEN** `DaoModule` installs `DaoCommonModule`, `StudiosDaoModule`, `AdminDaoModule`, and `ServersDaoModule` one level down
- **AND** `StudiosDaoModule` installs `IndividualsDaoModule`, `CollectionsDaoModule`, `AgentsDaoModule`, `EventsDaoModule`, `SystemsDaoModule`, `CampaignsDaoModule`, and `GamesDaoModule` one level down
