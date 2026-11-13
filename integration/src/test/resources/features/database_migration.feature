Feature: Flyway Database Migration and Bootstrap Provisioning
  As a system administrator or deployment process
  I want to execute Flyway database migrations via the command-line or migration service
  So that the PostgreSQL database schemas, roles, enums, tables, and permissions are correctly provisioned

  Scenario: Bootstrap migration creates schemas and applies privileges
    Given a clean PostgreSQL database instance with pre-configured roles "njall", "njall_admin", "njall_users", and "njall_system"
    When the database migrator executes the bootstrap migration
    Then schemas "njall", "njall_admin", "njall_users", and "njall_system" exist and are owned by "njall"
    And role "njall_admin" has USAGE on "njall", "njall_admin", "njall_users" and SELECT on all tables in "njall"
    And role "njall_users" has USAGE on "njall", "njall_users" and SELECT on all tables in "njall"

  Scenario: Seed records are populated with configured placeholders
    Given migration configuration with server name "alpha-node", primary domain "larpconnect.test", and admin contact "ops@larpconnect.test"
    When the database migrator executes the bootstrap migration
    Then table "njall.servers" contains a server with name "alpha-node" and primary domain "larpconnect.test"
    And table "njall.server_contacts" contains an ADMIN contact with email "ops@larpconnect.test"

  Scenario: Server application executes migration and exits when migrate subcommand is provided
    Given the server application is started with argument "migrate"
    When the server execution completes
    Then database migrations are executed to completion
    And the application process terminates with exit status 0

  Scenario: Server application executes migration with custom database parameters
    Given the server application is started with custom database migration arguments
    When the server execution completes with custom arguments
    Then database migrations are executed to completion
    And the application process terminates with exit status 0

  Scenario: Bootstrap migration is idempotent
    Given the bootstrap database migration has already been executed
    When the database migrator executes the bootstrap migration again
    Then zero migrations are applied

  Scenario: Reactions schema and materialized view provisioning
    Given the bootstrap database migration has already been executed
    Then table "njall_users.reactions" exists and is owned by "njall"
    And table "njall_users.reactions" has row level security enabled
    And role "njall_users" has USAGE on schema "njall_users" and SELECT on table "reaction_counts"
    And materialized view "njall_users.reaction_counts" can be refreshed concurrently

  Scenario: Migration V13 renames entities owner to generator while retaining collections owner_id
    Given the bootstrap database migration has already been executed
    Then table "njall_users.entities" has non-null column "generator" and no column "owner"
    And table "njall_users.entities" has foreign key constraint "fk_entities_generator" and index "idx_entities_generator" and no constraint "fk_entities_owner" or index "idx_entities_owner"
    And table "njall_users.collections" retains column "owner_id", constraint "fk_collections_owner", constraint "unq_collections_owner_name", and index "idx_collections_owner"


