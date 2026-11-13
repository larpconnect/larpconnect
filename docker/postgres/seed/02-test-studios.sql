-- 02-test-studios.sql
-- Idempotent seed data for core test studios and studio lookup entries.

SET search_path = njall_users, njall_admin, public;

-- User-space tenanted studios (must be inserted before lookup due to foreign key)
INSERT INTO njall_users.studios (id, name) VALUES
    ('018d0000-0000-7000-8000-000000000001', 'Valkyrie Example Larp Studio'),
    ('018d0000-0000-7000-8000-000000000002', 'Ironwood Fake Larp Chronicles')
ON CONFLICT (id) DO NOTHING;

-- Admin studio lookup entries
INSERT INTO njall_admin.studios_lookup (tenant_id, studio_id, alias) VALUES
    ('018d0000-0000-7000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'valkyrie_example'),
    ('018d0000-0000-7000-8000-000000000002', 'b0000000-0000-4000-8000-000000000002', 'ironwood_fake')
ON CONFLICT (tenant_id) DO NOTHING;

-- Seed default self-owned studio Actors
INSERT INTO njall_users.entities (id, tenant_id, generator, entity_type, summary) VALUES
    ('018d0000-0000-7000-8000-000000000010', '018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000010', 'Actor', 'Valkyrie System Studio Actor'),
    ('018d0000-0000-7000-8000-000000000020', '018d0000-0000-7000-8000-000000000002', '018d0000-0000-7000-8000-000000000020', 'Actor', 'Ironwood System Studio Actor')
ON CONFLICT (id) DO NOTHING;

INSERT INTO njall_users.actors (tenant_id, id, preferred_username) VALUES
    ('018d0000-0000-7000-8000-000000000001', '018d0000-0000-7000-8000-000000000010', 'valkyrie_admin'),
    ('018d0000-0000-7000-8000-000000000002', '018d0000-0000-7000-8000-000000000020', 'ironwood_admin')
ON CONFLICT (id) DO NOTHING;
