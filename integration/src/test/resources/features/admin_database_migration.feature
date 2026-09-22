Feature: Administrative Schema Migration and Isolation
  As a system administrator
  I want Flyway to provision the njall_admin schema
  So that administrative tables, enums, triggers, RLS, and permissions are correctly configured

  Scenario: Admin migration creates schema, tables, enums, and triggers
    Given a clean PostgreSQL database instance with pre-configured roles "njall", "njall_admin", "njall_users", and "njall_system"
    When the database migrator executes the bootstrap migration
    Then table "njall_admin.admin_roles" exists and is owned by "njall"
    And table "njall_admin.admin_users" exists and is owned by "njall"
    And table "njall_admin.admin_role_assignments" exists and is owned by "njall"
    And table "njall_admin.studios_lookup" exists and is owned by "njall"
    And role "njall_users" has USAGE on schema "njall_admin" and SELECT on table "studios_lookup"
    And role "njall_users" has no SELECT on table "njall_admin.admin_users"
    And table "njall_admin.studios_lookup" has row level security enabled
