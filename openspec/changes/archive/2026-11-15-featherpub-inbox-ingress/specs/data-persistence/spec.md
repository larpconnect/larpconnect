## ADDED Requirements

### Requirement: Case-Insensitive Preferred Username Actor Lookup
`AgentDAO` (`DefaultAgentDAO` in `com.larpconnect.njall.data.dao.studios.actors`) SHALL provide
`findByPreferredUsername(UUID tenantId, String preferredUsername)` and
`findByIdentifier(UUID tenantId, String identifier)` under `@NjallUsers` (`app.tenant_id`), trying
UUID primary-key lookup first before matching `lower(preferred_username)` across active
(`deleted_on IS NULL`) `'Actor'` rows.

#### Scenario: Resolve active actor by case-insensitive preferredUsername
- **GIVEN** an active **Studio** **Tenant** `tenantId` containing an active **Actor** with
  `preferred_username = "valkyrie_lead"` and a soft-deleted **Actor** with
  `preferred_username = "retired_lead"`
- **WHEN** `AgentDAO.findByPreferredUsername(tenantId, "VALKYRIE_LEAD")` and
  `AgentDAO.findByPreferredUsername(tenantId, "retired_lead")` execute
- **THEN** `"VALKYRIE_LEAD"` returns the active **Agent** record and `"retired_lead"` returns
  `Optional.empty()`

### Requirement: Tenanted Generator Hierarchy Inbox Routing Query
`InboxActivityDAO` (`DefaultInboxActivityDAO` in `com.larpconnect.njall.data.dao.studios.inbox`)
SHALL expose `resolveRoutableRecipients(UUID tenantId, UUID receivingActorId, Set<String>
candidateIdentifiers)` under `@NjallUsers` (`app.tenant_id`), resolving active **Actor** UUIDs and
case-insensitive usernames and walking upward along `njall_users.entities.generator` via a
cycle-safe `WITH RECURSIVE` query to return active targets whose **generator** lineage reaches
`receivingActorId`.

#### Scenario: Recursive generator ancestor query resolves transitive descendants and excludes siblings
- **GIVEN** an active **Studio** **Tenant** `tenantId` with self-generated **Actor** `rootA`, child
  `childA1` (`generator = rootA`), grandchild `grandchildA2` (`generator = childA1`), and unrelated
  self-generated **Actor** `rootB`
- **WHEN** `InboxActivityDAO.resolveRoutableRecipients` executes for `receivingActorId = rootA` with
  candidates referencing `rootA`, `grandchildA2` (by `preferredUsername`), and `rootB`
- **THEN** the returned set contains `rootA` and `grandchildA2` and excludes `rootB`

### Requirement: Tenanted Inbox Activity Deduplication DAO
`InboxActivityDAO` SHALL expose `filterUnseenTargets` (returning target **Actor** UUIDs without an
unexpired row `expires_on > :now` in `njall_users.inbox_activities`) and `recordAcceptedActivity`
(idempotently upserting `(tenant_id, target_actor_id, activity_id)` and pruning expired rows for
those targets) backed by `@Immutable` `InboxActivityRecord` in `com.larpconnect.njall.data.domain.inbox`.

#### Scenario: Deduplication DAO filters unexpired targets and re-accepts expired entries
- **GIVEN** an active **Studio** **Tenant** `tenantId` with **Actor** targets `actorA` (recorded for
  `activityId` with `expires_on` in the future) and `actorB` (recorded for `activityId` with
  `expires_on` in the past)
- **WHEN** `InboxActivityDAO.filterUnseenTargets(tenantId, activityId, Set.of(actorA, actorB), now)`
  executes
- **THEN** the returned unseen set contains only `actorB` and excludes `actorA`

## MODIFIED Requirements

### Requirement: Domain Subpackage Hierarchy and Immutable Domain Records
The system SHALL partition `@Immutable` domain records and enums across
`com.larpconnect.njall.data.domain.{common,admin,servers,studios,inbox}`, including `EventType`,
`Event`, **GameSystem**, `Campaign`, and `Game` in `studios`, `InboxActivityRecord` in `inbox`, and
`AdminUser`, `AdminRole`, and `StudioLookup` (`id()` returning `studioId()`) in `admin`.

#### Scenario: Domain records across subpackages implement DatabaseObject
- **GIVEN** `AdminUser`, `StudioLookup`, `Event`, `GameSystem`, `Campaign`, and `Game` record
  instances
- **WHEN** checked against `DatabaseObject`
- **THEN** each record is an instance of `DatabaseObject` and returns a non-null UUID from `id()`

#### Scenario: StudioLookup exposes tenantId and studioId
- **GIVEN** a `StudioLookup` record created from persistent data
- **WHEN** methods `tenantId()` and `studioId()` are invoked
- **THEN** both return valid non-null UUIDs, and `id()` equals `studioId()`
