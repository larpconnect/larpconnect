-- 01-default-roles-and-admin-users.sql
-- Idempotent seed data for administrative roles, admin users, assignments, and default studio roles.

SET search_path = njall_admin, njall_users, public;

-- Admin Roles
INSERT INTO njall_admin.admin_roles (id, role_name) VALUES
    ('018d0000-0000-7000-8000-000000000011', 'example_admin'),
    ('018d0000-0000-7000-8000-000000000012', 'fake_operator'),
    ('018d0000-0000-7000-8000-000000000013', 'example_auditor')
ON CONFLICT (role_name) DO NOTHING;

-- Admin Users
INSERT INTO njall_admin.admin_users (id, username, status) VALUES
    ('018d0000-0000-7000-8000-000000000021', 'admin_alice_example', 'ACTIVE'),
    ('018d0000-0000-7000-8000-000000000022', 'test_fake_admin', 'ACTIVE')
ON CONFLICT (username) DO NOTHING;

-- Admin Role Assignments
INSERT INTO njall_admin.admin_role_assignments (admin_user_id, role_id)
SELECT u.id, r.id
FROM njall_admin.admin_users u
CROSS JOIN njall_admin.admin_roles r
WHERE u.username = 'admin_alice_example' AND r.role_name = 'example_admin'
ON CONFLICT (admin_user_id, role_id) DO NOTHING;

INSERT INTO njall_admin.admin_role_assignments (admin_user_id, role_id)
SELECT u.id, r.id
FROM njall_admin.admin_users u
CROSS JOIN njall_admin.admin_roles r
WHERE u.username = 'test_fake_admin' AND r.role_name = 'fake_operator'
ON CONFLICT (admin_user_id, role_id) DO NOTHING;

-- Default Studio Roles
INSERT INTO njall_users.default_studio_roles (id, name) VALUES
    ('018d0000-0000-7000-8000-000000000031', 'Example Owner'),
    ('018d0000-0000-7000-8000-000000000032', 'Fake Storyteller'),
    ('018d0000-0000-7000-8000-000000000033', 'Example Player'),
    ('018d0000-0000-7000-8000-000000000034', 'Fake Crew')
ON CONFLICT (name) DO NOTHING;
